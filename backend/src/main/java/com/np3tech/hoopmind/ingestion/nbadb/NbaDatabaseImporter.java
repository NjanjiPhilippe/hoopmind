package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.Conference;
import com.np3tech.hoopmind.nba.entity.enummeration.Division;
import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.GameType;
import com.np3tech.hoopmind.nba.entity.enummeration.SeasonStatus;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.GameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.StandingRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamGameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamRow;
import com.np3tech.hoopmind.ingestion.nbadb.TeamSeasonAggregator.TeamSeason;
import com.np3tech.hoopmind.nba.repository.SeasonRepository;
import com.np3tech.hoopmind.nba.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Importe la base NBA de Wyatt Walsh (Kaggle {@code wyattowalsh/basketball}, licence CC BY-SA 4.0) dans le schéma HoopMind :
 * équipes, saisons, matchs, résultats, statistiques d'équipe par match, puis bilans de saison calculés.
 * <p>
 * L'import est rejouable : chaque ligne est retrouvée par son identifiant stats.nba.com et mise à jour plutôt que dupliquée.
 * Seules les {@code seasons} saisons les plus récentes sont importées.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NbaDatabaseImporter {

    static final int MAX_SEASONS = 80;

    /** Lignes envoyées par lot : assez pour amortir les allers-retours, assez peu pour borner la mémoire. */
    static final int BATCH_SIZE = 1_000;

    private final NbaDatabaseImportProperties properties;
    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    /** Ce que l'import a écrit, pour l'administrateur. */
    public record ImportReport(List<String> seasons, int teams, int games, int playedGames, int teamGameStats,
                               int teamSeasons, int skippedGames, long durationMs) {
    }

    /**
     * @param seasons nombre de saisons à importer ; {@code null} reprend le réglage {@code hoopmind.import.nba-database.seasons}
     */
    @Transactional
    public ImportReport importDatabase(Integer seasons) {
        int seasonCount = seasons != null ? seasons : properties.seasons();
        if (seasonCount < 1 || seasonCount > MAX_SEASONS) {
            throw new NbaImportException("Le nombre de saisons doit être compris entre 1 et " + MAX_SEASONS + ".");
        }
        Path file = Path.of(properties.path());
        log.info("Import de la base NBA {} ({} saisons)", file.toAbsolutePath(), seasonCount);
        try (NbaDatabaseReader reader = NbaDatabaseReader.open(file)) {
            return importSource(reader, seasonCount);
        }
    }

    /**
     * Importe les {@code seasonCount} saisons les plus récentes d'une source : la base de Walsh, ou les saisons
     * lues sur stats.nba.com (déjà téléchargées : la transaction ne couvre que l'écriture).
     */
    @Transactional
    ImportReport importSource(NbaSource source, int seasonCount) {
        long start = System.nanoTime();
        List<String> seasonYears = source.recentSeasons(seasonCount);
        if (seasonYears.isEmpty()) {
            throw new NbaImportException("La source ne contient aucun match de saison régulière.");
        }
        Map<Long, Long> teamIds = importTeams(source.teams());

        List<GameRow> allGames = source.games(seasonYears);
        List<GameRow> games = allGames.stream()
                .filter(g -> teamIds.containsKey(g.homeTeamId()) && teamIds.containsKey(g.awayTeamId()))
                .toList();
        int skipped = allGames.size() - games.size();
        if (skipped > 0) {
            log.warn("{} matchs ignorés : au moins une des deux équipes est inconnue", skipped);
        }

        Map<String, Long> seasonIds = importSeasons(seasonYears, games);
        // Une saison sans match de saison régulière n'est pas créée : ni elle ni ses matchs ne sont importés.
        List<String> importedSeasons = seasonYears.stream().filter(seasonIds::containsKey).toList();
        List<GameRow> seasonGames = games.stream().filter(g -> seasonIds.containsKey(g.seasonYear())).toList();
        skipped += games.size() - seasonGames.size();
        games = seasonGames;
        Map<String, Long> gameIds = importGames(games, teamIds, seasonIds);
        Map<String, Long> resultIds = importResults(games, gameIds, teamIds);

        Map<String, Map<Long, TeamGameRow>> boxScore = source.teamGames(seasonYears).stream()
                .filter(row -> gameIds.containsKey(row.gameId()) && teamIds.containsKey(row.teamId()))
                .collect(Collectors.groupingBy(TeamGameRow::gameId, Collectors.toMap(TeamGameRow::teamId, row -> row)));
        int teamGameStats = importTeamGameStats(boxScore, games, gameIds, resultIds, teamIds);
        int teamSeasons = importTeamSeasons(games, boxScore, source.standings(seasonYears), teamIds, seasonIds);

        ImportReport report = new ImportReport(importedSeasons, teamIds.size(), games.size(),
                (int) games.stream().filter(GameRow::played).count(), teamGameStats, teamSeasons, skipped,
                Duration.ofNanos(System.nanoTime() - start).toMillis());
        log.info("Import terminé : {}", report);
        return report;
    }

    // ---------- Équipes ----------

    private Map<Long, Long> importTeams(List<TeamRow> rows) {
        Map<Long, Long> ids = new HashMap<>();
        for (TeamRow row : rows) {
            Optional<Team> existing = teamRepository.findByExternalId(row.teamId())
                    .or(() -> teamRepository.findByAbbreviation(row.abbreviation()));
            Optional<Conference> conference = conference(row.conference());
            Optional<Division> division = division(row.division());
            Team team;
            if (existing.isPresent()) {
                team = existing.get();
            } else if (conference.isPresent() && division.isPresent()) {
                team = Team.builder()
                        .abbreviation(row.abbreviation())
                        .city(Objects.requireNonNullElse(row.city(), ""))
                        .name(nickname(row))
                        .build();
            } else {
                log.warn("Équipe {} ignorée : conférence ou division inconnue", row.abbreviation());
                continue;
            }
            team.setExternalId(row.teamId());
            conference.ifPresent(team::setConference);
            division.ifPresent(team::setDivision);
            if (row.arena() != null) team.setArena(row.arena());
            if (row.yearFounded() != null) team.setFoundedYear(row.yearFounded());
            ids.put(row.teamId(), teamRepository.saveAndFlush(team).getId());
        }
        return ids;
    }

    /** « Los Angeles Lakers » et « Los Angeles » donnent « Lakers ». */
    private static String nickname(TeamRow row) {
        String fullName = Objects.requireNonNullElse(row.fullName(), row.abbreviation());
        if (row.city() != null && fullName.startsWith(row.city() + " ")) {
            return fullName.substring(row.city().length() + 1);
        }
        return fullName.substring(fullName.lastIndexOf(' ') + 1);
    }

    static Optional<Conference> conference(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        return switch (Character.toUpperCase(value.strip().charAt(0))) {
            case 'E' -> Optional.of(Conference.EASTERN);
            case 'W' -> Optional.of(Conference.WESTERN);
            default -> Optional.empty();
        };
    }

    static Optional<Division> division(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(Division.valueOf(value.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // ---------- Saisons ----------

    private Map<String, Long> importSeasons(List<String> seasonYears, List<GameRow> games) {
        Map<String, List<GameRow>> bySeason = games.stream().collect(Collectors.groupingBy(GameRow::seasonYear));
        Map<String, Long> ids = new HashMap<>();
        for (String seasonYear : seasonYears) {
            List<GameRow> seasonGames = bySeason.getOrDefault(seasonYear, List.of());
            List<GameRow> regular = seasonGames.stream().filter(GameRow::regularSeason).toList();
            if (regular.isEmpty()) {
                continue;
            }
            int year = Integer.parseInt(seasonYear.substring(0, 4));
            Season season = seasonRepository.findByYear(year).orElseGet(Season::new);
            season.setYear(year);
            season.setName(year + "-" + (year + 1));
            season.setStartDate(minDate(regular));
            season.setEndDate(maxDate(regular));
            season.setPlayoffsStartDate(seasonGames.stream().filter(GameRow::playoffs).map(GameRow::gameDate).min(Comparator.naturalOrder()).orElse(null));
            season.setFinalsStartDate(seasonGames.stream().filter(GameRow::finals).map(GameRow::gameDate).min(Comparator.naturalOrder()).orElse(null));
            season.setStatus(seasonStatus(seasonGames));
            ids.put(seasonYear, seasonRepository.saveAndFlush(season).getId());
        }
        return ids;
    }

    private static SeasonStatus seasonStatus(List<GameRow> games) {
        if (games.stream().noneMatch(GameRow::played)) return SeasonStatus.UPCOMING;
        if (games.stream().allMatch(GameRow::played) && games.stream().anyMatch(GameRow::finals)) return SeasonStatus.COMPLETED;
        return SeasonStatus.IN_PROGRESS;
    }

    private static LocalDate minDate(List<GameRow> games) {
        return games.stream().map(GameRow::gameDate).min(Comparator.naturalOrder()).orElseThrow();
    }

    private static LocalDate maxDate(List<GameRow> games) {
        return games.stream().map(GameRow::gameDate).max(Comparator.naturalOrder()).orElseThrow();
    }

    // ---------- Matchs et résultats ----------

    private Map<String, Long> importGames(List<GameRow> games, Map<Long, Long> teamIds, Map<String, Long> seasonIds) {
        List<Object[]> rows = games.stream()
                .filter(g -> seasonIds.containsKey(g.seasonYear()))
                .map(g -> new Object[]{
                        g.gameId(), teamIds.get(g.homeTeamId()), teamIds.get(g.awayTeamId()), seasonIds.get(g.seasonYear()),
                        gameType(g).name(), g.gameDate().atStartOfDay(), g.arena(),
                        (g.played() ? GameStatus.COMPLETED : GameStatus.SCHEDULED).name()})
                .toList();
        batch("""
                INSERT INTO games (external_id, home_team_id, away_team_id, season_id, game_type, scheduled_date, venue, status,
                                   created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                ON CONFLICT (external_id) DO UPDATE SET
                    home_team_id = EXCLUDED.home_team_id, away_team_id = EXCLUDED.away_team_id, season_id = EXCLUDED.season_id,
                    game_type = EXCLUDED.game_type, scheduled_date = EXCLUDED.scheduled_date,
                    venue = COALESCE(EXCLUDED.venue, games.venue), status = EXCLUDED.status, updated_at = now()""", rows);

        Map<String, Long> ids = new HashMap<>();
        jdbc.query("SELECT id, external_id FROM games WHERE external_id IS NOT NULL",
                rs -> {
                    ids.put(rs.getString(2), rs.getLong(1));
                });
        return ids;
    }

    /**
     * Envoie les lignes par lots de {@link #BATCH_SIZE}. Avec {@code reWriteBatchedInserts} (voir application.yml),
     * le pilote PostgreSQL transforme chaque lot en un seul {@code INSERT … VALUES (…), (…)}.
     */
    private void batch(String sql, List<Object[]> rows) {
        for (int from = 0; from < rows.size(); from += BATCH_SIZE) {
            jdbc.batchUpdate(sql, rows.subList(from, Math.min(from + BATCH_SIZE, rows.size())));
        }
    }

    private static GameType gameType(GameRow game) {
        if (game.finals()) return GameType.FINALS;
        if (game.playoffs()) return GameType.PLAYOFFS;
        return game.playIn() ? GameType.PLAY_IN : GameType.REGULAR_SEASON;
    }

    private Map<String, Long> importResults(List<GameRow> games, Map<String, Long> gameIds, Map<Long, Long> teamIds) {
        List<Object[]> rows = new ArrayList<>();
        for (GameRow g : games) {
            Long gameId = gameIds.get(g.gameId());
            if (gameId == null || !g.played() || g.homePoints().equals(g.awayPoints())) {
                continue;
            }
            boolean homeWon = g.homePoints() > g.awayPoints();
            Long home = teamIds.get(g.homeTeamId());
            Long away = teamIds.get(g.awayTeamId());
            int overtime = g.overtimePeriods();
            rows.add(new Object[]{gameId, g.homePoints(), g.awayPoints(), Math.abs(g.homePoints() - g.awayPoints()),
                    homeWon ? home : away, homeWon ? away : home,
                    json(g.homePeriods()), json(g.awayPeriods()), overtime > 0, overtime > 0 ? overtime : null});
        }
        batch("""
                INSERT INTO game_results (game_id, home_team_score, away_team_score, score_difference, winner_id, loser_id,
                                          home_team_quarter_scores_json, away_team_quarter_scores_json, overtime,
                                          overtime_periods, recorded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (game_id) DO UPDATE SET
                    home_team_score = EXCLUDED.home_team_score, away_team_score = EXCLUDED.away_team_score,
                    score_difference = EXCLUDED.score_difference, winner_id = EXCLUDED.winner_id, loser_id = EXCLUDED.loser_id,
                    home_team_quarter_scores_json = EXCLUDED.home_team_quarter_scores_json,
                    away_team_quarter_scores_json = EXCLUDED.away_team_quarter_scores_json,
                    overtime = EXCLUDED.overtime, overtime_periods = EXCLUDED.overtime_periods""", rows);

        Map<String, Long> ids = new HashMap<>();
        jdbc.query("SELECT r.id, g.external_id FROM game_results r JOIN games g ON g.id = r.game_id WHERE g.external_id IS NOT NULL",
                rs -> {
                    ids.put(rs.getString(2), rs.getLong(1));
                });
        return ids;
    }

    private static String json(List<Integer> periods) {
        return periods.isEmpty() ? null : periods.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }

    private int importTeamGameStats(Map<String, Map<Long, TeamGameRow>> boxScore, List<GameRow> games,
                                    Map<String, Long> gameIds, Map<String, Long> resultIds, Map<Long, Long> teamIds) {
        Map<String, GameRow> playedGames = games.stream().filter(GameRow::played)
                .collect(Collectors.toMap(GameRow::gameId, g -> g));
        List<Object[]> rows = new ArrayList<>();
        boxScore.forEach((externalGameId, lines) -> {
            GameRow game = playedGames.get(externalGameId);
            if (game == null) {
                return;
            }
            lines.values().forEach(line -> {
                Integer points = line.points() != null ? line.points()
                        : line.teamId() == game.homeTeamId() ? game.homePoints() : game.awayPoints();
                rows.add(new Object[]{teamIds.get(line.teamId()), gameIds.get(externalGameId), resultIds.get(externalGameId),
                        points, line.fgm(), line.fga(), percentage(line.fgm(), line.fga()),
                        line.fg3m(), line.fg3a(), percentage(line.fg3m(), line.fg3a()),
                        line.ftm(), line.fta(), percentage(line.ftm(), line.fta()),
                        line.oreb(), line.dreb(), line.reb(), line.ast(), line.stl(), line.blk(), line.tov(), line.pf()});
            });
        });
        batch("""
                INSERT INTO team_game_stats (team_id, game_id, game_result_id, points,
                    field_goals_made, field_goals_attempted, field_goal_percentage,
                    three_points_made, three_points_attempted, three_point_percentage,
                    free_throws_made, free_throws_attempted, free_throw_percentage,
                    offensive_rebounds, defensive_rebounds, total_rebounds, assists, steals, blocks, turnovers, personal_fouls)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (team_id, game_id) DO UPDATE SET
                    game_result_id = EXCLUDED.game_result_id, points = EXCLUDED.points,
                    field_goals_made = EXCLUDED.field_goals_made, field_goals_attempted = EXCLUDED.field_goals_attempted,
                    field_goal_percentage = EXCLUDED.field_goal_percentage,
                    three_points_made = EXCLUDED.three_points_made, three_points_attempted = EXCLUDED.three_points_attempted,
                    three_point_percentage = EXCLUDED.three_point_percentage,
                    free_throws_made = EXCLUDED.free_throws_made, free_throws_attempted = EXCLUDED.free_throws_attempted,
                    free_throw_percentage = EXCLUDED.free_throw_percentage,
                    offensive_rebounds = EXCLUDED.offensive_rebounds, defensive_rebounds = EXCLUDED.defensive_rebounds,
                    total_rebounds = EXCLUDED.total_rebounds, assists = EXCLUDED.assists, steals = EXCLUDED.steals,
                    blocks = EXCLUDED.blocks, turnovers = EXCLUDED.turnovers, personal_fouls = EXCLUDED.personal_fouls""", rows);
        return rows.size();
    }

    private static BigDecimal percentage(Integer made, Integer attempted) {
        if (made == null || attempted == null || attempted == 0) return null;
        return BigDecimal.valueOf((double) made / attempted).setScale(3, RoundingMode.HALF_UP);
    }

    // ---------- Bilans de saison ----------

    private int importTeamSeasons(List<GameRow> games, Map<String, Map<Long, TeamGameRow>> boxScore,
                                  List<StandingRow> standings, Map<Long, Long> teamIds, Map<String, Long> seasonIds) {
        Map<String, StandingRow> ranks = standings.stream()
                .collect(Collectors.toMap(s -> s.seasonYear() + "/" + s.teamId(), s -> s, (a, b) -> a));
        Map<String, List<GameRow>> bySeason = games.stream().collect(Collectors.groupingBy(GameRow::seasonYear));
        List<Object[]> rows = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);
        bySeason.forEach((seasonYear, seasonGames) -> {
            Long seasonId = seasonIds.get(seasonYear);
            if (seasonId == null) return;
            for (TeamSeason t : TeamSeasonAggregator.aggregate(seasonGames, boxScore)) {
                StandingRow rank = ranks.get(seasonYear + "/" + t.teamId());
                rows.add(new Object[]{teamIds.get(t.teamId()), seasonId, t.wins(), t.losses(), t.winPercentage(),
                        rank == null ? null : rank.conferenceRank(), rank == null ? null : rank.divisionRank(),
                        t.homeWins(), t.homeLosses(), t.awayWins(), t.awayLosses(),
                        t.pointsPerGame(), t.pointsAllowedPerGame(), t.pointsDifferential(),
                        t.fieldGoalPercentage(), t.threePointPercentage(), t.freeThrowPercentage(),
                        t.reboundsPerGame(), t.assistsPerGame(), t.stealsPerGame(), t.blocksPerGame(), t.turnoversPerGame(),
                        t.offensiveRating(), t.defensiveRating(), t.netRating(), t.pace(),
                        t.effectiveFieldGoalPercentage(), t.trueShootingPercentage(),
                        t.currentStreak(), t.longestWinStreak(), t.longestLoseStreak(), t.last10GamesJson(), now});
            }
        });
        batch("""
                INSERT INTO team_stats (team_id, season_id, wins, losses, win_percentage, conference_rank, division_rank,
                    home_wins, home_losses, away_wins, away_losses, points_per_game, points_allowed_per_game, points_differential,
                    field_goal_percentage, three_point_percentage, free_throw_percentage, rebounds_per_game, assists_per_game,
                    steals_per_game, blocks_per_game, turnovers_per_game, offensive_rating, defensive_rating, net_rating, pace,
                    effective_field_goal_percentage, true_shooting_percentage, current_streak, longest_win_streak,
                    longest_lose_streak, last_10_games_json, last_updated)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (team_id, season_id) DO UPDATE SET
                    wins = EXCLUDED.wins, losses = EXCLUDED.losses, win_percentage = EXCLUDED.win_percentage,
                    conference_rank = EXCLUDED.conference_rank, division_rank = EXCLUDED.division_rank,
                    home_wins = EXCLUDED.home_wins, home_losses = EXCLUDED.home_losses,
                    away_wins = EXCLUDED.away_wins, away_losses = EXCLUDED.away_losses,
                    points_per_game = EXCLUDED.points_per_game, points_allowed_per_game = EXCLUDED.points_allowed_per_game,
                    points_differential = EXCLUDED.points_differential, field_goal_percentage = EXCLUDED.field_goal_percentage,
                    three_point_percentage = EXCLUDED.three_point_percentage, free_throw_percentage = EXCLUDED.free_throw_percentage,
                    rebounds_per_game = EXCLUDED.rebounds_per_game, assists_per_game = EXCLUDED.assists_per_game,
                    steals_per_game = EXCLUDED.steals_per_game, blocks_per_game = EXCLUDED.blocks_per_game,
                    turnovers_per_game = EXCLUDED.turnovers_per_game, offensive_rating = EXCLUDED.offensive_rating,
                    defensive_rating = EXCLUDED.defensive_rating, net_rating = EXCLUDED.net_rating, pace = EXCLUDED.pace,
                    effective_field_goal_percentage = EXCLUDED.effective_field_goal_percentage,
                    true_shooting_percentage = EXCLUDED.true_shooting_percentage, current_streak = EXCLUDED.current_streak,
                    longest_win_streak = EXCLUDED.longest_win_streak, longest_lose_streak = EXCLUDED.longest_lose_streak,
                    last_10_games_json = EXCLUDED.last_10_games_json, last_updated = EXCLUDED.last_updated""", rows);
        return rows.size();
    }
}
