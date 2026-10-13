package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.GameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.StandingRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamGameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamRow;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lecture du fichier {@code nba.sqlite} de Wyatt Walsh, en lecture seule. Seules les tables utiles aux prédictions sont lues,
 * et seulement pour les saisons demandées.
 * <p>
 * Le fichier publié sur Kaggle existe sous deux formats, reconnus à l'ouverture :
 * <ul>
 *   <li>le schéma en étoile de nbadb ({@code dim_game}, {@code fact_game_result}…), lu par {@link NbadbReader} ;</li>
 *   <li>l'ancien format à plat ({@code game}, {@code team}, {@code line_score}…), encore livré dans l'archive Kaggle
 *       alors que sa description annonce le nouveau, lu par {@link LegacyKaggleReader}.</li>
 * </ul>
 * Les deux produisent les mêmes lignes ({@link NbaDatabaseRows}) : l'import ne dépend pas du format.
 */
@Slf4j
abstract sealed class NbaDatabaseReader implements AutoCloseable permits NbadbReader, LegacyKaggleReader {

    private final Connection connection;

    NbaDatabaseReader(Connection connection) {
        this.connection = connection;
    }

    static NbaDatabaseReader open(Path file) {
        if (!Files.isRegularFile(file)) {
            log.warn("Base NBA introuvable : {}", file.toAbsolutePath());
            throw new NbaImportException("Base NBA introuvable sur le serveur. Téléchargez-la depuis la page Données, "
                    + "avec scripts/download-nba-database.sh, ou renseignez NBA_DATABASE_PATH.");
        }
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:file:" + file.toAbsolutePath() + "?mode=ro");
        } catch (SQLException e) {
            log.warn("Impossible d'ouvrir la base NBA {}", file.toAbsolutePath(), e);
            throw new NbaImportException("Impossible d'ouvrir la base NBA : le fichier n'est pas une base SQLite lisible.", e);
        }
        Set<String> tables = tables(connection);
        if (tables.contains("dim_game")) {
            return new NbadbReader(connection);
        }
        if (tables.contains("game") && tables.contains("team")) {
            log.info("Base NBA {} au format ancien (tables game, team, line_score)", file.toAbsolutePath());
            return new LegacyKaggleReader(connection);
        }
        close(connection);
        throw new NbaImportException("Format de base NBA inconnu : ni la table dim_game (nbadb) ni la table game "
                + "(ancien format Kaggle) n'existent dans ce fichier.");
    }

    /** Les {@code count} saisons les plus récentes qui ont des matchs de saison régulière, de la plus ancienne à la plus récente. */
    abstract List<String> recentSeasons(int count);

    abstract List<TeamRow> teams();

    abstract List<GameRow> games(List<String> seasons);

    abstract List<TeamGameRow> teamGames(List<String> seasons);

    abstract List<StandingRow> standings(List<String> seasons);

    @Override
    public void close() {
        close(connection);
    }

    private static void close(Connection connection) {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // lecture seule : rien à perdre
        }
    }

    private static Set<String> tables(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type IN ('table', 'view')");
             ResultSet rs = statement.executeQuery()) {
            Set<String> names = new HashSet<>();
            while (rs.next()) {
                names.add(rs.getString(1));
            }
            return names;
        } catch (SQLException e) {
            close(connection);
            log.warn("Impossible de lister les tables de la base NBA", e);
            throw new NbaImportException("Impossible d'ouvrir la base NBA : le fichier n'est pas une base SQLite lisible.", e);
        }
    }

    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            List<T> rows = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
            }
            return rows;
        } catch (SQLException e) {
            log.warn("Lecture de la base NBA impossible", e);
            throw new NbaImportException("Lecture de la base NBA impossible : une table ou une colonne attendue manque "
                    + "(le détail est dans le journal du serveur).", e);
        }
    }

    static String placeholders(List<?> values) {
        return values.stream().map(v -> "?").collect(Collectors.joining(", "));
    }

    /** Les nombres peuvent arriver en entier ou en réel (l'ancien format stocke les points en {@code REAL}). */
    static Integer integer(ResultSet rs, int column) throws SQLException {
        Object value = rs.getObject(column);
        return switch (value) {
            case null -> null;
            case Number number -> number.intValue();
            case String text when !text.isBlank() -> (int) Double.parseDouble(text.strip());
            default -> null;
        };
    }

    /** Quatre quart-temps puis les prolongations jouées (une prolongation à 0 point reste une prolongation jouée). */
    static List<Integer> periods(ResultSet rs, int firstColumn, int overtimeColumns) throws SQLException {
        List<Integer> periods = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Integer points = integer(rs, firstColumn + i);
            if (points == null) {
                return List.of();
            }
            periods.add(points);
        }
        for (int i = 4; i < 4 + overtimeColumns; i++) {
            Integer points = integer(rs, firstColumn + i);
            if (points != null) {
                periods.add(points);
            }
        }
        return periods;
    }

    /** Les dates arrivent en texte ISO, avec ou sans heure. */
    static LocalDate date(String value) {
        if (value == null || value.length() < 10) {
            throw new NbaImportException("Date de match illisible : " + value);
        }
        return LocalDate.parse(value.substring(0, 10));
    }

    static <T> List<T> distinctBy(List<T> rows, Function<T, String> key) {
        Map<String, T> unique = new LinkedHashMap<>();
        rows.forEach(row -> unique.putIfAbsent(key.apply(row), row));
        return new ArrayList<>(unique.values());
    }
}
