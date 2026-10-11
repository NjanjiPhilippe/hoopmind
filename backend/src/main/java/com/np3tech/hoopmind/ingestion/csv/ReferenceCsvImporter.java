package com.np3tech.hoopmind.ingestion.csv;

import com.np3tech.hoopmind.ingestion.nbadb.NbaImportException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Importe les CSV de l'archive de Sumitro Datta déposés depuis l'interface : un zip, des CSV, ou les deux.
 * <p>
 * Chaque fichier est reconnu par ses colonnes ({@link ReferenceDataset#detect}) et importé dans sa propre transaction :
 * un fichier illisible n'empêche pas les autres. Chaque ligne est retrouvée par sa clé naturelle et mise à jour
 * seulement si elle a changé, sans doublon. Les valeurs « NA » deviennent nulles, les nombres restent des nombres.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceCsvImporter {

    /** Garde-fous contre une archive anormale (l'archive complète fait environ 32 Mo et 22 fichiers). */
    static final long MAX_UNCOMPRESSED_BYTES = 512L * 1024 * 1024;
    static final int MAX_FILES = 100;
    private static final int BATCH_SIZE = 1000;

    private static final String UPSERT = """
            INSERT INTO reference_csv_rows (dataset, row_key, season, data, imported_at)
            VALUES (?, ?, ?, ?::jsonb, ?)
            ON CONFLICT (dataset, row_key) DO UPDATE
               SET season = EXCLUDED.season, data = EXCLUDED.data, imported_at = EXCLUDED.imported_at
             WHERE reference_csv_rows.data IS DISTINCT FROM EXCLUDED.data
            """;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final JsonMapper jsonMapper;

    public enum FileStatus { IMPORTED, UNRECOGNIZED, FAILED }

    /**
     * Compte rendu d'un fichier : {@code inserted} lignes nouvelles, {@code updated} modifiées, {@code unchanged}
     * déjà identiques, {@code skipped} sans clé exploitable.
     */
    public record FileReport(String file, FileStatus status, String dataset, String label, int rows, int inserted,
                             int updated, int unchanged, int skipped, String message) {
    }

    /** Un fichier reçu (ou extrait d'un zip). */
    public record Upload(String name, InputStream content) {
    }

    /** Ce que contient la base pour un jeu de l'archive. */
    public record DatasetSummary(String dataset, String label, long rows, Integer firstSeason, Integer lastSeason,
                                 LocalDateTime importedAt) {
    }

    /** Importe chaque fichier ; un zip est parcouru et chacun de ses CSV traité comme un fichier à part. */
    public List<FileReport> importFiles(List<Upload> uploads) {
        List<FileReport> reports = new ArrayList<>();
        long[] budget = {MAX_UNCOMPRESSED_BYTES};
        for (Upload upload : uploads) {
            if (lowerCase(upload.name()).endsWith(".zip")) {
                importZip(upload, reports, budget);
            } else {
                add(reports, importCsv(upload.name(), limited(upload.content(), budget)));
            }
        }
        return reports;
    }

    /** Le contenu actuel de la base, jeu par jeu (tous les jeux connus, même vides). */
    public List<DatasetSummary> summary() {
        Map<String, DatasetSummary> found = new LinkedHashMap<>();
        jdbc.query("""
                SELECT dataset, count(*) AS n, min(season) AS first, max(season) AS last, max(imported_at) AS at
                  FROM reference_csv_rows GROUP BY dataset
                """, rs -> {
            String name = rs.getString("dataset");
            found.put(name, new DatasetSummary(name, null, rs.getLong("n"), (Integer) rs.getObject("first"),
                    (Integer) rs.getObject("last"), rs.getObject("at", LocalDateTime.class)));
        });
        return Arrays.stream(ReferenceDataset.values()).map(d -> {
            DatasetSummary s = found.get(d.name());
            return s == null ? new DatasetSummary(d.name(), d.label(), 0, null, null, null)
                    : new DatasetSummary(d.name(), d.label(), s.rows(), s.firstSeason(), s.lastSeason(), s.importedAt());
        }).toList();
    }

    private void importZip(Upload upload, List<FileReport> reports, long[] budget) {
        try (ZipInputStream zip = new ZipInputStream(upload.content())) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                String name = Path.of(entry.getName()).getFileName().toString();
                if (entry.isDirectory() || entry.getName().startsWith("__MACOSX") || name.startsWith(".")
                        || !lowerCase(name).endsWith(".csv")) {
                    continue;
                }
                add(reports, importCsv(name, limited(nonClosing(zip), budget)));
            }
        } catch (IOException | UncheckedIOException e) {
            log.warn("Archive zip {} illisible", upload.name(), e);
            reports.add(failed(upload.name(), tooLarge(e) ? UploadTooLargeException.MESSAGE : "Archive zip illisible."));
        }
    }

    private void add(List<FileReport> reports, FileReport report) {
        if (reports.size() >= MAX_FILES) {
            throw new NbaImportException("Trop de fichiers dans un même envoi (" + MAX_FILES + " au plus).");
        }
        reports.add(report);
    }

    private FileReport importCsv(String name, InputStream content) {
        Reader reader = new InputStreamReader(content, StandardCharsets.UTF_8);
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true).setTrim(true).get();
        try (CSVParser parser = CSVParser.builder().setReader(reader).setFormat(format).get()) {
            List<String> header = parser.getHeaderNames().stream()
                    .map(h -> h.replace("﻿", "").trim().toLowerCase(Locale.ROOT))
                    .toList();
            Optional<ReferenceDataset> detected = ReferenceDataset.detect(header);
            if (detected.isEmpty()) {
                return new FileReport(name, FileStatus.UNRECOGNIZED, null, null, 0, 0, 0, 0, 0,
                        "Colonnes non reconnues : ce fichier ne fait pas partie de l'archive NBA Stats.");
            }
            ReferenceDataset dataset = detected.get();
            FileReport report = transaction.execute(status -> write(name, dataset, header, parser));
            log.info("CSV {} importé comme {} : {}", name, dataset, report);
            return report;
        } catch (IOException | UncheckedIOException e) {
            log.warn("CSV {} illisible", name, e);
            return failed(name, tooLarge(e) ? UploadTooLargeException.MESSAGE
                    : "Fichier illisible : CSV mal formé ou encodage autre que UTF-8.");
        } catch (RuntimeException e) {
            log.warn("Import du CSV {} impossible", name, e);
            return failed(name, e instanceof NbaImportException ? e.getMessage()
                    : "Import impossible : erreur inattendue. Le détail est dans les journaux du serveur.");
        }
    }

    private FileReport write(String name, ReferenceDataset dataset, List<String> header, CSVParser parser) {
        long before = countRows(dataset);
        LocalDateTime now = LocalDateTime.now();
        int rows = 0;
        int skipped = 0;
        int affected = 0;
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        for (CSVRecord record : parser) {
            rows++;
            Map<String, String> raw = new LinkedHashMap<>();
            Map<String, Object> values = new LinkedHashMap<>();
            for (int i = 0; i < header.size() && i < record.size(); i++) {
                raw.put(header.get(i), record.get(i));
                values.put(header.get(i), value(record.get(i)));
            }
            String key = key(dataset, raw);
            if (key == null) {
                skipped++;
                continue;
            }
            batch.add(new Object[]{dataset.name(), key, season(values.get("season")),
                    jsonMapper.writeValueAsString(values), now});
            if (batch.size() == BATCH_SIZE) {
                affected += flush(batch);
            }
        }
        affected += flush(batch);
        int inserted = (int) (countRows(dataset) - before);
        int written = rows - skipped;
        return new FileReport(name, FileStatus.IMPORTED, dataset.name(), dataset.label(), rows, inserted,
                affected - inserted, written - affected, skipped,
                skipped > 0 ? skipped + " ligne(s) sans clé ignorée(s)." : null);
    }

    private int flush(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return 0;
        }
        int affected = Arrays.stream(jdbc.batchUpdate(UPSERT, batch)).map(n -> Math.max(n, 0)).sum();
        batch.clear();
        return affected;
    }

    private long countRows(ReferenceDataset dataset) {
        Long n = jdbc.queryForObject("SELECT count(*) FROM reference_csv_rows WHERE dataset = ?", Long.class, dataset.name());
        return n == null ? 0 : n;
    }

    /** Clé naturelle, à partir des valeurs brutes ; {@code null} si une colonne de clé est vide. */
    static String key(ReferenceDataset dataset, Map<String, String> raw) {
        List<String> parts = new ArrayList<>();
        for (String column : dataset.keyColumns()) {
            String value = raw.get(column);
            if (value == null || value.isEmpty()) {
                return null;
            }
            parts.add(value);
        }
        String key = String.join("|", parts);
        return key.length() > 255 ? null : key;
    }

    /** « NA » et cases vides deviennent nulles ; nombres et booléens sont typés, le reste reste du texte. */
    static Object value(String raw) {
        if (raw == null || raw.isEmpty() || raw.equals("NA")) {
            return null;
        }
        if (raw.equals("TRUE") || raw.equals("FALSE")) {
            return Boolean.valueOf(raw);
        }
        if (raw.matches("-?(\\d+\\.?\\d*|\\.\\d+)")) {
            BigDecimal number = new BigDecimal(raw);
            return number.scale() <= 0 && number.abs().compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) < 0
                    ? (Object) number.longValueExact() : number;
        }
        return raw;
    }

    private static Integer season(Object value) {
        return value instanceof Long l && l > 1800 && l < 3000 ? l.intValue() : null;
    }

    private static boolean tooLarge(Exception e) {
        return e instanceof UploadTooLargeException || e.getCause() instanceof UploadTooLargeException;
    }

    /** Budget d'octets dépassé : le seul détail d'entrée/sortie utile à l'administrateur. */
    private static final class UploadTooLargeException extends IOException {

        static final String MESSAGE = "Envoi trop volumineux une fois décompressé (512 Mo au plus).";

        UploadTooLargeException() {
            super(MESSAGE);
        }
    }

    private static FileReport failed(String name, String message) {
        return new FileReport(name, FileStatus.FAILED, null, null, 0, 0, 0, 0, 0, message);
    }

    private static String lowerCase(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    /** Lit un flux sans dépasser le budget d'octets restant pour tout l'envoi. */
    private static InputStream limited(InputStream in, long[] budget) {
        return new FilterInputStream(in) {
            @Override
            public int read() throws IOException {
                int b = super.read();
                if (b >= 0) consume(1);
                return b;
            }

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                int n = super.read(buffer, offset, length);
                if (n > 0) consume(n);
                return n;
            }

            private void consume(long n) throws IOException {
                budget[0] -= n;
                if (budget[0] < 0) {
                    throw new UploadTooLargeException();
                }
            }
        };
    }

    /** Le parseur ferme son flux à la fin d'un fichier : il ne doit pas fermer le zip qui contient les suivants. */
    private static InputStream nonClosing(InputStream in) {
        return new FilterInputStream(in) {
            @Override
            public void close() {
            }
        };
    }
}
