package com.np3tech.hoopmind.ml.scoring;

import com.np3tech.hoopmind.ml.MlProperties;
import com.np3tech.hoopmind.shared.exception.AppException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Enregistre la carte de score du dernier modèle entraîné ({@code <model-dir>/game-outcome-scoring-card.json})
 * et la garde en mémoire pour les prédictions.
 */
@Component
@EnableConfigurationProperties(MlProperties.class)
public class ScoringCardStore {

    private final Path file;
    private final JsonMapper json;
    private final AtomicReference<ScoringCard> current = new AtomicReference<>();

    ScoringCardStore(MlProperties properties, JsonMapper json) {
        this.file = Path.of(properties.modelDir(), "game-outcome-scoring-card.json");
        this.json = json;
    }

    /** Écrit la carte (remplacement atomique du fichier) puis la rend active. */
    public void save(ScoringCard card) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "scoring-card-", ".json");
            Files.writeString(temporary, json.writeValueAsString(card));
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new AppException("Impossible d'enregistrer le modèle entraîné.", e, HttpStatus.INTERNAL_SERVER_ERROR,
                    "MODEL_SAVE_FAILED");
        }
        current.set(card);
    }

    /** La carte active, relue sur disque au premier appel après un redémarrage. */
    public Optional<ScoringCard> current() {
        ScoringCard card = current.get();
        if (card == null && Files.isRegularFile(file)) {
            try {
                card = json.readValue(Files.readString(file), ScoringCard.class);
            } catch (IOException e) {
                throw new AppException("Le modèle enregistré est illisible : réentraînez-le.", e,
                        HttpStatus.INTERNAL_SERVER_ERROR, "MODEL_UNREADABLE");
            }
            current.compareAndSet(null, card);
        }
        return Optional.ofNullable(card).filter(c -> c.matches(GameOutcomeFeatures.NAMES));
    }
}
