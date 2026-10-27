package com.np3tech.hoopmind.ml.scoring;

import com.np3tech.hoopmind.ml.MlProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Enregistre les cartes du dernier modèle entraîné et les garde en mémoire pour les prédictions :
 * {@code <model-dir>/game-outcome-scoring-card.json} pour le vainqueur, {@code <model-dir>/game-score-card.json}
 * pour le score.
 */
@Component
@EnableConfigurationProperties(MlProperties.class)
public class ScoringCardStore {

    private final CardFile<ScoringCard> outcome;
    private final CardFile<GameScoreCard> score;

    ScoringCardStore(MlProperties properties, JsonMapper json) {
        this.outcome = new CardFile<>(Path.of(properties.modelDir(), "game-outcome-scoring-card.json"),
                ScoringCard.class, json);
        this.score = new CardFile<>(Path.of(properties.modelDir(), "game-score-card.json"), GameScoreCard.class, json);
    }

    /** Écrit la carte du vainqueur (remplacement atomique du fichier) puis la rend active. */
    public void save(ScoringCard card) {
        outcome.save(card);
    }

    public void save(GameScoreCard card) {
        score.save(card);
    }

    /** La carte du vainqueur active, relue sur disque au premier appel après un redémarrage. */
    public Optional<ScoringCard> current() {
        return outcome.load().filter(c -> c.matches(GameOutcomeFeatures.NAMES));
    }

    /**
     * La carte du score, absente tant que le modèle n'a pas été réentraîné depuis son ajout : la prédiction du
     * vainqueur fonctionne sans elle.
     */
    public Optional<GameScoreCard> currentScore() {
        return score.load().filter(c -> c.matches(GameScoreFeatures.NAMES));
    }
}
