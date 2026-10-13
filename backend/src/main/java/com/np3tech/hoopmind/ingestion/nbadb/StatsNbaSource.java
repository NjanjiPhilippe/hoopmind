package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.NbaAlignment.Alignment;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.GameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.StandingRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamGameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamRow;
import com.np3tech.hoopmind.ingestion.nbadb.StatsNbaClient.GameLogLine;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Les saisons lues sur stats.nba.com, ramenées aux lignes de {@link NbaDatabaseRows}. Le journal donne une ligne
 * par équipe et par match : les deux lignes d'un même match deviennent un {@link GameRow} (l'équipe à domicile est
 * celle dont l'affiche porte « vs. », l'autre porte « @ ») et deux {@link TeamGameRow}.
 * <p>
 * Ce que le journal ne donne pas :
 * <ul>
 *   <li>les points par période : ces matchs n'ont pas de détail par quart-temps ni de prolongations comptées ;</li>
 *   <li>conférences, divisions et classements : alignement actuel et rangs recalculés ({@link NbaAlignment}) ;</li>
 *   <li>la salle et l'année de création des équipes : celles déjà en base gardent les leurs.</li>
 * </ul>
 */
@Slf4j
final class StatsNbaSource implements NbaSource {

    private static final DateTimeFormatter US_DATE = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().appendPattern("MMM d, yyyy").toFormatter(Locale.ENGLISH);

    /** Surnoms en deux mots : la ville est le reste du nom complet. */
    private static final Set<String> TWO_WORD_NICKNAMES = Set.of("Trail Blazers");

    private final Map<String, List<GameRow>> games = new LinkedHashMap<>();
    private final Map<String, List<TeamGameRow>> teamGames = new LinkedHashMap<>();
    private final Map<Long, TeamRow> teams = new LinkedHashMap<>();

    /**
     * @param lines par saison (« 2024-25 »), les lignes du journal de la saison régulière puis des playoffs
     */
    StatsNbaSource(Map<String, List<GameLogLine>> lines) {
        lines.forEach(this::addSeason);
    }

    private void addSeason(String season, List<GameLogLine> lines) {
        Map<String, List<GameLogLine>> byGame = new LinkedHashMap<>();
        for (GameLogLine line : lines) {
            String gameId = text(line, "GAME_ID");
            if (gameId != null) {
                byGame.computeIfAbsent(gameId, id -> new ArrayList<>()).add(line);
            }
        }
        List<GameRow> seasonGames = new ArrayList<>();
        List<TeamGameRow> seasonLines = new ArrayList<>();
        int incomplete = 0;
        for (Map.Entry<String, List<GameLogLine>> entry : byGame.entrySet()) {
            String gameId = entry.getKey();
            Optional<GameLogLine> home = entry.getValue().stream().filter(StatsNbaSource::atHome).findFirst();
            Optional<GameLogLine> away = entry.getValue().stream().filter(l -> !atHome(l)).findFirst();
            if (home.isEmpty() || away.isEmpty() || !(gameId.startsWith("002") || gameId.startsWith("004"))) {
                incomplete++;
                continue;
            }
            addTeam(home.get());
            addTeam(away.get());
            seasonGames.add(new GameRow(gameId, date(text(home.get(), "GAME_DATE")), season,
                    id(home.get()), id(away.get()), null, integer(home.get(), "PTS"), integer(away.get(), "PTS"),
                    List.of(), List.of()));
            seasonLines.add(teamGame(gameId, home.get()));
            seasonLines.add(teamGame(gameId, away.get()));
        }
        if (incomplete > 0) {
            log.warn("stats.nba.com, saison {} : {} matchs ignorés (une seule équipe dans le journal, ou ni saison "
                    + "régulière ni playoffs)", season, incomplete);
        }
        seasonGames.sort(Comparator.comparing(GameRow::gameDate).thenComparing(GameRow::gameId));
        games.put(season, seasonGames);
        teamGames.put(season, seasonLines);
    }

    /** « BOS vs. NYK » : Boston reçoit ; « NYK @ BOS » : New York se déplace. */
    private static boolean atHome(GameLogLine line) {
        String matchup = text(line, "MATCHUP");
        return matchup != null && matchup.contains(" vs. ");
    }

    private void addTeam(GameLogLine line) {
        teams.computeIfAbsent(id(line), teamId -> {
            String abbreviation = text(line, "TEAM_ABBREVIATION");
            String fullName = text(line, "TEAM_NAME");
            Optional<Alignment> alignment = NbaAlignment.of(abbreviation);
            return new TeamRow(teamId, abbreviation, fullName, city(fullName), null, null,
                    alignment.map(Alignment::conference).orElse(null), alignment.map(Alignment::division).orElse(null));
        });
    }

    /** « Portland Trail Blazers » donne « Portland », « Boston Celtics » donne « Boston ». */
    static String city(String fullName) {
        if (fullName == null || !fullName.contains(" ")) {
            return null;
        }
        for (String nickname : TWO_WORD_NICKNAMES) {
            if (fullName.endsWith(" " + nickname)) {
                return fullName.substring(0, fullName.length() - nickname.length() - 1);
            }
        }
        return fullName.substring(0, fullName.lastIndexOf(' '));
    }

    private static TeamGameRow teamGame(String gameId, GameLogLine line) {
        return new TeamGameRow(gameId, id(line), integer(line, "PTS"),
                integer(line, "FGM"), integer(line, "FGA"), integer(line, "FG3M"), integer(line, "FG3A"),
                integer(line, "FTM"), integer(line, "FTA"), integer(line, "OREB"), integer(line, "DREB"),
                integer(line, "REB"), integer(line, "AST"), integer(line, "STL"), integer(line, "BLK"),
                integer(line, "TOV"), integer(line, "PF"));
    }

    @Override
    public List<String> recentSeasons(int count) {
        List<String> withRegularSeason = games.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(g -> !g.playoffs()))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        return withRegularSeason.subList(Math.max(0, withRegularSeason.size() - count), withRegularSeason.size());
    }

    @Override
    public List<TeamRow> teams() {
        return List.copyOf(teams.values());
    }

    @Override
    public List<GameRow> games(List<String> seasons) {
        return seasons.stream().flatMap(s -> games.getOrDefault(s, List.of()).stream()).toList();
    }

    @Override
    public List<TeamGameRow> teamGames(List<String> seasons) {
        return seasons.stream().flatMap(s -> teamGames.getOrDefault(s, List.of()).stream()).toList();
    }

    @Override
    public List<StandingRow> standings(List<String> seasons) {
        return NbaAlignment.standings(games(seasons), teams());
    }

    @Override
    public void close() {
        // tout est déjà en mémoire
    }

    private static long id(GameLogLine line) {
        return switch (line.get("TEAM_ID")) {
            case Number number -> number.longValue();
            case String text when !text.isBlank() -> Long.parseLong(text.strip());
            case null, default ->
                    throw new NbaImportException("Réponse inattendue de stats.nba.com : une ligne n'a pas d'équipe.");
        };
    }

    private static String text(GameLogLine line, String column) {
        Object value = line.get(column);
        return value == null ? null : value.toString().strip();
    }

    private static Integer integer(GameLogLine line, String column) {
        return switch (line.get(column)) {
            case null -> null;
            case Number number -> (int) Math.round(number.doubleValue());
            case String text when !text.isBlank() -> (int) Math.round(Double.parseDouble(text.strip()));
            default -> null;
        };
    }

    /** « 2024-10-22 » (avec ou sans heure), ou « OCT 22, 2024 » selon les versions de l'API. */
    static LocalDate date(String value) {
        if (value == null || value.isBlank()) {
            throw new NbaImportException("Réponse inattendue de stats.nba.com : un match n'a pas de date.");
        }
        try {
            if (Character.isDigit(value.charAt(0))) {
                return LocalDate.parse(value.substring(0, 10));
            }
            return LocalDate.parse(value, US_DATE);
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            throw new NbaImportException("Réponse inattendue de stats.nba.com : date de match illisible.", e);
        }
    }
}
