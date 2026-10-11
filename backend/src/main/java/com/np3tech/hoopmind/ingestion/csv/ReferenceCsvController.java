package com.np3tech.hoopmind.ingestion.csv;

import com.np3tech.hoopmind.ingestion.nbadb.NbaImportException;
import com.np3tech.hoopmind.ingestion.csv.ReferenceCsvImporter.DatasetSummary;
import com.np3tech.hoopmind.ingestion.csv.ReferenceCsvImporter.FileReport;
import com.np3tech.hoopmind.ingestion.csv.ReferenceCsvImporter.Upload;
import com.np3tech.hoopmind.shared.lock.DataOperationLock;
import com.np3tech.hoopmind.shared.lock.DataOperationLock.Permit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/imports/csv")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Import des données")
@SecurityRequirement(name = "bearerAuth")
class ReferenceCsvController {

    private final ReferenceCsvImporter importer;
    private final DataOperationLock lock;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "importReferenceCsv", summary = "Importer des CSV de l'archive NBA Stats (Sumitro Datta)",
            description = "Accepte le zip de l'archive et/ou des fichiers CSV. Chaque fichier est reconnu par ses colonnes, "
                    + "quel que soit son nom, puis importé sans doublon. Renvoie un compte rendu par fichier.")
    List<FileReport> importReferenceCsv(@RequestPart("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new NbaImportException("Aucun fichier reçu.");
        }
        List<InputStream> opened = new ArrayList<>();
        try (Permit ignored = lock.acquire(DataOperationLock.Operation.CSV_IMPORT)) {
            List<Upload> uploads = new ArrayList<>();
            for (MultipartFile file : files) {
                InputStream in = file.getInputStream();
                opened.add(in);
                uploads.add(new Upload(file.getOriginalFilename(), in));
            }
            return importer.importFiles(uploads);
        } catch (IOException e) {
            throw new NbaImportException("Lecture des fichiers reçus impossible : " + e.getMessage(), e);
        } finally {
            for (InputStream in : opened) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // rien à faire
                }
            }
        }
    }

    @GetMapping("/datasets")
    @Operation(operationId = "listReferenceDatasets", summary = "Contenu importé de l'archive NBA Stats",
            description = "Pour chaque fichier connu de l'archive : nombre de lignes, saisons couvertes, date du dernier import.")
    List<DatasetSummary> listReferenceDatasets() {
        return importer.summary();
    }
}
