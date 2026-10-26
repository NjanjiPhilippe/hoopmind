package com.np3tech.hoopmind.ml.history;

import com.np3tech.hoopmind.ml.GameOutcomePrediction;
import com.np3tech.hoopmind.nba.GameHistory;
import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Les prédictions de chaque utilisateur, et seulement les siennes : toutes les lectures et suppressions sont filtrées
 * par son identifiant, jamais par un paramètre de la requête.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PredictionHistory {

    private final SavedPredictionRepository repository;
    private final GameHistory games;
    private final Clock clock;

    /** Garde la prédiction ; le même match redemandé remplace la ligne existante. */
    public void record(long userId, PredictedMatch match, GameOutcomePrediction prediction) {
        SavedPrediction saved = repository.findByUserIdAndSeasonIdAndHomeTeamIdAndAwayTeamIdAndGameDateAndPlayoff(
                        userId, match.seasonId(), match.homeTeamId(), match.awayTeamId(), match.date(), match.playoff())
                .orElseGet(() -> new SavedPrediction(userId, match));
        saved.update(prediction.homeWinProbability(),
                prediction.score() == null ? null : prediction.score().homePoints(),
                prediction.score() == null ? null : prediction.score().awayPoints(),
                prediction.model(), clock.instant());
        repository.save(saved);
    }

    @Transactional(readOnly = true)
    public Page<PredictionHistoryEntry> mine(long userId, Pageable pageable) {
        Page<SavedPrediction> page = repository.findByUserId(userId, pageable);
        List<SavedPrediction> rows = page.getContent();
        Map<Long, String> names = games.teamNames(rows.stream()
                .flatMap(row -> Stream.of(row.getHomeTeamId(), row.getAwayTeamId())).collect(Collectors.toSet()));
        Map<MatchKey, GameRecord> played = playedGames(rows);
        return page.map(row -> {
            GameRecord game = played.get(MatchKey.of(row));
            Boolean correct = game == null ? null : (row.getHomeWinProbability() >= 0.5) == (game.homePoints() > game.awayPoints());
            return new PredictionHistoryEntry(row.getId(), row.getSeasonId(), row.getHomeTeamId(),
                    names.getOrDefault(row.getHomeTeamId(), "Équipe " + row.getHomeTeamId()), row.getAwayTeamId(),
                    names.getOrDefault(row.getAwayTeamId(), "Équipe " + row.getAwayTeamId()), row.getGameDate(),
                    row.isPlayoff(), row.getHomeWinProbability(), row.getPredictedHomePoints(),
                    row.getPredictedAwayPoints(), row.getModel(), row.getPredictedAt(),
                    game == null ? null : game.homePoints(), game == null ? null : game.awayPoints(), correct);
        });
    }

    /** 404 aussi pour la prédiction d'un autre utilisateur : on ne révèle pas qu'elle existe. */
    public void delete(long userId, long id) {
        repository.delete(repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> AppException.notFound("PREDICTION_NOT_FOUND", "Prédiction introuvable")));
    }

    /** Les matchs déjà joués qui correspondent aux prédictions de la page (mêmes équipes, même jour). */
    private Map<MatchKey, GameRecord> playedGames(List<SavedPrediction> rows) {
        Map<MatchKey, GameRecord> played = new HashMap<>();
        rows.stream().collect(Collectors.groupingBy(SavedPrediction::getSeasonId)).forEach((seasonId, seasonRows) ->
                games.seasonYear(seasonId).ifPresent(year -> {
                    Set<MatchKey> wanted = seasonRows.stream().map(MatchKey::of).collect(Collectors.toSet());
                    Set<Long> teams = seasonRows.stream().map(SavedPrediction::getHomeTeamId).collect(Collectors.toSet());
                    games.teamGames(year, teams).stream()
                            .filter(GameRecord::played)
                            .filter(game -> wanted.contains(MatchKey.of(game)))
                            .forEach(game -> played.put(MatchKey.of(game), game));
                }));
        return played;
    }

    private record MatchKey(long homeTeamId, long awayTeamId, LocalDate date) {
        static MatchKey of(SavedPrediction row) {
            return new MatchKey(row.getHomeTeamId(), row.getAwayTeamId(), row.getGameDate());
        }

        static MatchKey of(GameRecord game) {
            return new MatchKey(game.homeTeamId(), game.awayTeamId(), game.date());
        }
    }
}
