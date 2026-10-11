package com.np3tech.hoopmind.ingestion.csv;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Les fichiers de l'archive « NBA Stats (1947-present) » de Sumitro Datta (Kaggle, données Basketball-Reference).
 * <p>
 * Un fichier est reconnu par ses colonnes, pas par son nom : il appartient au jeu dont il contient toutes les colonnes
 * caractéristiques, le plus précis l'emportant (« End of Season Teams (Voting) » contient aussi les colonnes de
 * « End of Season Teams »). Chaque ligne est identifiée par sa clé naturelle, ce qui rend l'import rejouable.
 */
public enum ReferenceDataset {

    PLAYER_TOTALS("Joueurs : totaux par saison", Set.of("player_id", "team", "fg", "pts"), PlayerKey.COLUMNS),
    PLAYER_PER_GAME("Joueurs : moyennes par match", Set.of("player_id", "team", "pts_per_game"), PlayerKey.COLUMNS),
    PLAYER_PER_36("Joueurs : pour 36 minutes", Set.of("player_id", "team", "pts_per_36_min"), PlayerKey.COLUMNS),
    PLAYER_PER_100("Joueurs : pour 100 possessions", Set.of("player_id", "team", "pts_per_100_poss"), PlayerKey.COLUMNS),
    PLAYER_ADVANCED("Joueurs : statistiques avancées", Set.of("player_id", "team", "per", "ws", "bpm", "vorp"),
            PlayerKey.COLUMNS),
    PLAYER_SHOOTING("Joueurs : tirs", Set.of("player_id", "team", "avg_dist_fga"), PlayerKey.COLUMNS),
    PLAYER_PLAY_BY_PLAY("Joueurs : play-by-play", Set.of("player_id", "team", "on_court_plus_minus_per_100_poss"),
            PlayerKey.COLUMNS),
    PLAYER_SEASON_INFO("Joueurs : infos de saison", Set.of("player_id", "team", "experience"), PlayerKey.COLUMNS),
    PLAYER_CAREER_INFO("Joueurs : carrière", Set.of("player_id", "debut", "hof"), List.of("player_id")),
    AWARD_SHARES("Trophées individuels : votes", Set.of("award", "player_id", "share", "winner"),
            List.of("season", "award", "player_id", "player")),
    END_OF_SEASON_TEAMS("Équipes de fin de saison (All-NBA…)", Set.of("type", "number_tm", "player_id"),
            List.of("season", "lg", "type", "player_id")),
    END_OF_SEASON_TEAMS_VOTING("Équipes de fin de saison : votes", Set.of("type", "number_tm", "player_id", "share", "x1st_tm"),
            List.of("season", "lg", "type", "player_id")),
    ALL_STAR_SELECTIONS("Sélections au All-Star Game", Set.of("player_id", "replaced"), List.of("season", "lg", "player_id")),
    DRAFT_PICKS("Draft", Set.of("overall_pick", "round", "player_id"), List.of("season", "lg", "overall_pick", "player_id")),
    TEAM_ABBREVIATIONS("Équipes : abréviations", Set.of("abbreviation", "playoffs"), TeamKey.COLUMNS),
    TEAM_SUMMARIES("Équipes : bilans et ratings", Set.of("team", "abbreviation", "w", "l", "srs"), TeamKey.COLUMNS),
    TEAM_TOTALS("Équipes : totaux", Set.of("team", "abbreviation", "fg", "pts"), TeamKey.COLUMNS),
    TEAM_PER_GAME("Équipes : moyennes par match", Set.of("team", "abbreviation", "pts_per_game"), TeamKey.COLUMNS),
    TEAM_PER_100("Équipes : pour 100 possessions", Set.of("team", "abbreviation", "pts_per_100_poss"), TeamKey.COLUMNS),
    OPPONENT_TOTALS("Adversaires : totaux", Set.of("team", "abbreviation", "opp_pts"), TeamKey.COLUMNS),
    OPPONENT_PER_GAME("Adversaires : moyennes par match", Set.of("team", "abbreviation", "opp_pts_per_game"),
            TeamKey.COLUMNS),
    OPPONENT_PER_100("Adversaires : pour 100 possessions", Set.of("team", "abbreviation", "opp_pts_per_100_poss"),
            TeamKey.COLUMNS);

    private final String label;
    private final Set<String> signature;
    private final List<String> keyColumns;

    ReferenceDataset(String label, Set<String> signature, List<String> keyColumns) {
        this.label = label;
        this.signature = signature;
        this.keyColumns = keyColumns;
    }

    public String label() {
        return label;
    }

    public List<String> keyColumns() {
        return keyColumns;
    }

    /** Le jeu auquel appartient un fichier d'après ses colonnes, s'il n'y a pas d'ambiguïté. */
    public static Optional<ReferenceDataset> detect(Collection<String> header) {
        Set<String> columns = Set.copyOf(header);
        List<ReferenceDataset> matches = java.util.Arrays.stream(values())
                .filter(d -> columns.containsAll(d.signature) && columns.containsAll(d.keyColumns))
                .sorted(Comparator.comparingInt((ReferenceDataset d) -> d.signature.size()).reversed())
                .toList();
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        if (matches.size() > 1 && matches.get(0).signature.size() == matches.get(1).signature.size()) {
            return Optional.empty();
        }
        return Optional.of(matches.getFirst());
    }

    private static final class PlayerKey {
        static final List<String> COLUMNS = List.of("season", "lg", "player_id", "team");
    }

    private static final class TeamKey {
        static final List<String> COLUMNS = List.of("season", "lg", "team");
    }
}
