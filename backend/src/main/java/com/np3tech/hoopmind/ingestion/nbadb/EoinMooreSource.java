package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.NbaAlignment.Alignment;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.GameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.StandingRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamGameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Les fichiers {@code Games.csv} et {@code TeamStatistics.csv} du jeu Kaggle d'Eoin Moore
 * ({@code eoinamoore/historical-nba-data-and-player-box-scores}), ramenés aux lignes de {@link NbaDatabaseRows}.
 * Ils couvrent les saisons que la base de Wyatt Walsh n'a pas (après 2022-23), avec les identifiants stats.nba.com.
 * <p>
 * Seules les saisons demandées sont gardées en mémoire. Ce que ces fichiers ne donnent pas, ou autrement :
 * <ul>
 *   <li>les identifiants de match sont stockés en nombre (22300061) : ils sont remis sur dix caractères
 *       (« 0022300061 ») pour garder les préfixes 002 / 005 / 004 et retrouver les matchs déjà en base ;</li>
 *   <li>l'abréviation des équipes : elle vient de l'identifiant de la franchise ({@link NbaAlignment#abbreviation}) ;</li>
 *   <li>conférences, divisions et classements : alignement actuel et rangs recalculés ({@link NbaAlignment}) ;</li>
 *   <li>les points par prolongation au-delà de la deuxième : ils sont regroupés dans la troisième.</li>
 * </ul>
 */
@Slf4j
final class EoinMooreSource implements NbaSource {

    static final String GAMES_FILE = "Games.csv";
    static final String TEAM_STATISTICS_FILE = "TeamStatistics.csv";

    /** Minutes jouées par une équipe : 240 en temps réglementaire, 25 de plus par prolongation. */
    private static final int REGULATION_MINUTES = 240;
    private static final int OVERTIME_MINUTES = 25;

    private final Map<String, List<GameRow>> games = new LinkedHashMap<>();
    private final Map<String, List<TeamGameRow>> teamGames = new LinkedHashMap<>();
    private final Map<Long, TeamRow> teams = new LinkedHashMap<>();

    /**
     * @param directory dossier qui contient {@code Games.csv} et {@code TeamStatistics.csv}
     * @param fromYear  année de début de la première saison gardée (2023 pour 2023-24)
     * @param toYear    année de début de la dernière saison gardée
     */
    static EoinMooreSource read(Path directory, int fromYear, int toYear) {
        return new EoinMooreSource(directory.resolve(GAMES_FILE), directory.resolve(TEAM_STATISTICS_FILE), fromYear, toYear);
    }

    EoinMooreSource(Path gamesFile, Path teamStatisticsFile, int fromYear, int toYear) {
        Map<String, Map<Long, CSVRecord>> lines = new HashMap<>();
        each(teamStatisticsFile, line -> {
            String gameId = gameId(line.get("gameId"));
            Long teamId = id(line.get("teamId"));
            if (gameId != null && teamId != null && kept(gameId, null, fromYear, toYear)) {
                lines.computeIfAbsent(gameId, id -> new HashMap<>()).putIfAbsent(teamId, line);
            }
        });
        Map<String, GameRow> unique = new LinkedHashMap<>();
        Map<Long, LocalDate> latestTeamGame = new HashMap<>();
        each(gamesFile, line -> {
            String gameId = gameId(line.get("gameId"));
            Long home = id(line.get("hometeamId"));
            Long away = id(line.get("awayteamId"));
            if (gameId == null || home == null || away == null) {
                return;
            }
            LocalDate date = date(text(line, "gameDate") != null ? text(line, "gameDate") : text(line, "gameDateTimeEst"));
            if (!kept(gameId, date, fromYear, toYear)) {
                return;
            }
            Map<Long, CSVRecord> boxScore = lines.getOrDefault(gameId, Map.of());
            unique.putIfAbsent(gameId, new GameRow(gameId, date, NbaAlignment.season(seasonYear(gameId, date)), home, away,
                    text(line, "arenaName"), integer(line, "homeScore"), integer(line, "awayScore"),
                    periods(boxScore.get(home)), periods(boxScore.get(away))));
            addTeam(home, text(line, "hometeamCity"), text(line, "hometeamName"), date, latestTeamGame);
            addTeam(away, text(line, "awayteamCity"), text(line, "awayteamName"), date, latestTeamGame);
        });

        unique.values().stream()
                .sorted(Comparator.comparing(GameRow::gameDate).thenComparing(GameRow::gameId))
                .forEach(game -> {
                    games.computeIfAbsent(game.seasonYear(), s -> new ArrayList<>()).add(game);
                    lines.getOrDefault(game.gameId(), Map.of()).forEach((teamId, line) -> teamGames
                            .computeIfAbsent(game.seasonYear(), s -> new ArrayList<>()).add(teamGame(game.gameId(), teamId, line)));
                });
        log.info("Fichiers d'Eoin Moore, saisons {} à {} : {} matchs", NbaAlignment.season(fromYear),
                NbaAlignment.season(toYear), unique.size());
    }

    /** Saison régulière, play-in et playoffs des saisons demandées ; présaison, All-Star et NBA Cup sont ignorés. */
    private static boolean kept(String gameId, LocalDate date, int fromYear, int toYear) {
        if (!(gameId.startsWith("002") || gameId.startsWith("005") || gameId.startsWith("004"))) {
            return false;
        }
        int year = seasonYear(gameId, date);
        return year >= fromYear && year <= toYear;
    }

    /**
     * L'année de début de saison est codée dans l'identifiant : « 0022300061 » donne 2023. À défaut, elle vient de la
     * date (une saison commence à l'automne).
     */
    static int seasonYear(String gameId, LocalDate date) {
        String digits = gameId.substring(3, 5);
        if (Character.isDigit(digits.charAt(0)) && Character.isDigit(digits.charAt(1))) {
            int twoDigits = Integer.parseInt(digits);
            return twoDigits >= 46 ? 1900 + twoDigits : 2000 + twoDigits;
        }
        if (date == null) {
            return Integer.MIN_VALUE;
        }
        return date.getMonthValue() >= 8 ? date.getYear() : date.getYear() - 1;
    }

    /** Le dernier nom porté gagne : une franchise renommée garde son nom actuel. */
    private void addTeam(long teamId, String city, String nickname, LocalDate date, Map<Long, LocalDate> latest) {
        if (latest.containsKey(teamId) && latest.get(teamId).isAfter(date)) {
            return;
        }
        latest.put(teamId, date);
        Optional<String> abbreviation = NbaAlignment.abbreviation(teamId);
        Optional<Alignment> alignment = abbreviation.flatMap(NbaAlignment::of);
        String fullName = city == null ? nickname : nickname == null ? city : city + " " + nickname;
        teams.put(teamId, new TeamRow(teamId, abbreviation.orElse(null), fullName, city, null, null,
                alignment.map(Alignment::conference).orElse(null), alignment.map(Alignment::division).orElse(null)));
    }

    /** Quatre quart-temps puis les prolongations jouées ; vide si la feuille de match n'a pas de détail par période. */
    static List<Integer> periods(CSVRecord line) {
        if (line == null) {
            return List.of();
        }
        List<Integer> periods = new ArrayList<>();
        for (int quarter = 1; quarter <= 4; quarter++) {
            Integer points = integer(line, "q" + quarter + "Points");
            if (points == null) {
                return List.of();
            }
            periods.add(points);
        }
        int first = Optional.ofNullable(integer(line, "ot1Points")).orElse(0);
        int second = Optional.ofNullable(integer(line, "ot2Points")).orElse(0);
        int all = Optional.ofNullable(integer(line, "otAllPoints")).orElse(first + second);
        int overtimes = Math.max(overtimesFromMinutes(line), all > first + second ? 3 : second > 0 ? 2 : first > 0 ? 1 : 0);
        for (int overtime = 1; overtime <= overtimes; overtime++) {
            periods.add(switch (overtime) {
                case 1 -> first;
                case 2 -> second;
                case 3 -> all - first - second;
                default -> 0;
            });
        }
        return periods;
    }

    /** 265 minutes d'équipe : une prolongation. Une valeur hors de l'intervalle attendu est ignorée. */
    private static int overtimesFromMinutes(CSVRecord line) {
        Double minutes = number(line, "numMinutes");
        if (minutes == null || minutes < REGULATION_MINUTES) {
            return 0;
        }
        long overtimes = Math.round((minutes - REGULATION_MINUTES) / OVERTIME_MINUTES);
        return overtimes > 10 ? 0 : (int) overtimes;
    }

    private static TeamGameRow teamGame(String gameId, long teamId, CSVRecord line) {
        return new TeamGameRow(gameId, teamId, integer(line, "teamScore"),
                integer(line, "fieldGoalsMade"), integer(line, "fieldGoalsAttempted"),
                integer(line, "threePointersMade"), integer(line, "threePointersAttempted"),
                integer(line, "freeThrowsMade"), integer(line, "freeThrowsAttempted"),
                integer(line, "reboundsOffensive"), integer(line, "reboundsDefensive"), integer(line, "reboundsTotal"),
                integer(line, "assists"), integer(line, "steals"), integer(line, "blocks"),
                integer(line, "turnovers"), integer(line, "foulsPersonal"));
    }

    @Override
    public List<String> recentSeasons(int count) {
        List<String> withRegularSeason = games.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(GameRow::regularSeason))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        return withRegularSeason.subList(Math.max(0, withRegularSeason.size() - count), withRegularSeason.size());
    }

    @Override
    public List<TeamRow> teams() {
        return teams.values().stream().filter(team -> team.abbreviation() != null).toList();
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

    // ---------- Lecture du CSV ----------

    private static void each(Path file, Consumer<CSVRecord> action) {
        if (!Files.isRegularFile(file)) {
            throw new NbaImportException("Fichier " + file.getFileName() + " introuvable sur le serveur : déposez "
                    + GAMES_FILE + " et " + TEAM_STATISTICS_FILE + " dans le dossier EOINMOORE_DATA_DIR.");
        }
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true).setTrim(true).setAllowMissingColumnNames(true).get();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            skipByteOrderMark(reader);
            try (CSVParser parser = CSVParser.builder().setReader(reader).setFormat(format).get()) {
                parser.forEach(action);
            }
        } catch (IOException | UncheckedIOException e) {
            log.warn("Lecture de {} impossible", file.toAbsolutePath(), e);
            throw new NbaImportException("Fichier " + file.getFileName() + " illisible : CSV mal formé ou encodage "
                    + "autre que UTF-8.", e);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Colonnes inattendues dans {}", file.toAbsolutePath(), e);
            throw new NbaImportException("Fichier " + file.getFileName() + " : une colonne attendue manque. Prenez "
                    + "les fichiers du jeu Kaggle eoinamoore/historical-nba-data-and-player-box-scores.", e);
        }
    }

    private static void skipByteOrderMark(Reader reader) throws IOException {
        reader.mark(1);
        if (reader.read() != '﻿') {
            reader.reset();
        }
    }

    /** « 22300061 » ou « 0022300061 » donnent « 0022300061 ». */
    static String gameId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.strip();
        if (digits.endsWith(".0")) {
            digits = digits.substring(0, digits.length() - 2);
        }
        if (!digits.chars().allMatch(Character::isDigit) || digits.length() > 10) {
            return null;
        }
        return "0".repeat(10 - digits.length()) + digits;
    }

    private static Long id(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return (long) Double.parseDouble(value.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(CSVRecord line, String column) {
        if (!line.isMapped(column) || !line.isSet(column)) {
            return null;
        }
        String value = line.get(column);
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static Double number(CSVRecord line, String column) {
        String value = text(line, column);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer integer(CSVRecord line, String column) {
        Double value = number(line, column);
        return value == null ? null : (int) Math.round(value);
    }

    /** « 2024-04-14 », « 2024-04-14 13:00:00 » ou « 2024-04-14T13:00:00Z » donnent le 14 avril 2024. */
    static LocalDate date(String value) {
        if (value == null || value.length() < 10) {
            throw new NbaImportException("Date de match illisible dans " + GAMES_FILE + " : " + value);
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (DateTimeParseException e) {
            throw new NbaImportException("Date de match illisible dans " + GAMES_FILE + " : " + value, e);
        }
    }
}
