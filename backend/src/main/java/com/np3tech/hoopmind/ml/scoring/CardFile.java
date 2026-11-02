package com.np3tech.hoopmind.ml.scoring;

import com.np3tech.hoopmind.shared.exception.AppException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Le fichier JSON d'une carte de modèle entraîné, remplacé atomiquement à chaque entraînement et gardé en mémoire.
 * Partagé par les cartes du vainqueur et du score.
 */
final class CardFile<T> {

    private final Path file;
    private final Class<T> type;
    private final JsonMapper json;
    private final AtomicReference<T> current = new AtomicReference<>();

    CardFile(Path file, Class<T> type, JsonMapper json) {
        this.file = file;
        this.type = type;
        this.json = json;
    }

    /** Écrit la carte (remplacement atomique du fichier) puis la rend active. */
    void save(T card) {
        Path temporary = null;
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "scoring-card-", ".json");
            Files.writeString(temporary, json.writeValueAsString(card));
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            deleteQuietly(temporary);
            throw new AppException("Impossible d'enregistrer le modèle entraîné.", e, HttpStatus.INTERNAL_SERVER_ERROR,
                    "MODEL_SAVE_FAILED");
        }
        current.set(card);
    }

    private static void deleteQuietly(Path temporary) {
        if (temporary == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
            // L'erreur d'origine est celle qui compte ; un fichier temporaire resté là ne gêne pas la lecture.
        }
    }

    /** La carte active, relue sur disque au premier appel après un redémarrage. */
    Optional<T> load() {
        T card = current.get();
        if (card == null && Files.isRegularFile(file)) {
            try {
                card = json.readValue(Files.readString(file), type);
            } catch (IOException e) {
                throw new AppException("Le modèle enregistré est illisible : réentraînez-le.", e,
                        HttpStatus.INTERNAL_SERVER_ERROR, "MODEL_UNREADABLE");
            }
            current.compareAndSet(null, card);
        }
        return Optional.ofNullable(card);
    }
}
