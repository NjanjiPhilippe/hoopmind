package com.np3tech.hoopmind.ingestion.nbadb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Base64;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Télécharge la base NBA depuis l'API Kaggle et la place à l'endroit lu par l'import.
 * <p>
 * Kaggle renvoie les gros fichiers compressés en zip, après une redirection vers son stockage : la clé n'est envoyée
 * qu'à Kaggle, jamais au stockage. Le fichier n'est remplacé qu'une fois le téléchargement complet.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class KaggleDownloader {

    private static final int MAX_REDIRECTS = 5;

    private final KaggleProperties properties;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /**
     * @param target   où écrire le fichier (le dossier est créé au besoin)
     * @param progress reçoit les octets déjà téléchargés et la taille totale (-1 si inconnue)
     */
    void download(Path target, BiConsumer<Long, Long> progress) {
        if (!properties.configured()) {
            throw new NbaImportException("Aucune clé Kaggle configurée sur le serveur (KAGGLE_USERNAME et KAGGLE_KEY).");
        }
        URI uri = URI.create(properties.apiUrl() + "/datasets/download/" + properties.dataset() + "/" + properties.file());
        log.info("Téléchargement de {} depuis Kaggle", properties.file());
        Path temporary = null;
        try {
            Path directory = target.toAbsolutePath().getParent();
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, "kaggle-", ".download");
            HttpResponse<InputStream> response = send(uri);
            long total = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(temporary)) {
                byte[] buffer = new byte[1 << 16];
                long done = 0;
                for (int read; (read = in.read(buffer)) >= 0; ) {
                    out.write(buffer, 0, read);
                    done += read;
                    progress.accept(done, total);
                }
            }
            if (isZip(temporary)) {
                Path extracted = Files.createTempFile(directory, "kaggle-", ".sqlite");
                unzip(temporary, extracted);
                Files.delete(temporary);
                temporary = extracted;
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            temporary = null;
            log.info("Base NBA téléchargée : {} ({} octets)", target.toAbsolutePath(), Files.size(target));
        } catch (IOException e) {
            log.warn("Téléchargement Kaggle impossible", e);
            throw new NbaImportException("Téléchargement Kaggle impossible (réseau ou espace disque). "
                    + "Le détail est dans les journaux du serveur.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NbaImportException("Téléchargement Kaggle interrompu.", e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // fichier temporaire laissé sur place, sans conséquence
                }
            }
        }
    }

    private HttpResponse<InputStream> send(URI uri) throws IOException, InterruptedException {
        String credentials = Base64.getEncoder().encodeToString(
                (properties.username() + ":" + properties.key()).getBytes(StandardCharsets.UTF_8));
        URI current = uri;
        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            HttpRequest.Builder request = HttpRequest.newBuilder(current).GET();
            if (current.getHost().equals(uri.getHost())) {
                request.header("Authorization", "Basic " + credentials);
            }
            HttpResponse<InputStream> response = http.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                response.body().close();
                String location = response.headers().firstValue("Location")
                        .orElseThrow(() -> new IOException("redirection sans adresse"));
                current = current.resolve(location);
                continue;
            }
            if (status == 401 || status == 403) {
                response.body().close();
                throw new NbaImportException("Kaggle refuse la clé configurée (HTTP " + status
                        + "). Vérifiez KAGGLE_USERNAME et KAGGLE_KEY, et acceptez les conditions du jeu de données.");
            }
            if (status != 200) {
                response.body().close();
                throw new IOException("réponse HTTP " + status);
            }
            return response;
        }
        throw new IOException("trop de redirections");
    }

    private static boolean isZip(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] magic = in.readNBytes(4);
            return magic.length == 4 && magic[0] == 'P' && magic[1] == 'K' && magic[2] == 3 && magic[3] == 4;
        }
    }

    private void unzip(Path archive, Path target) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                if (!entry.isDirectory() && Path.of(entry.getName()).getFileName().toString().equals(properties.file())) {
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            }
        }
        throw new IOException("l'archive Kaggle ne contient pas " + properties.file());
    }
}
