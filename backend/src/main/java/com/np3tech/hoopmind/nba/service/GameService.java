package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.dto.request.GameCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.GameResultRequest;
import com.np3tech.hoopmind.nba.dto.request.GameUpdateRequest;
import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.GameResult;
import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.GameRepository;
import com.np3tech.hoopmind.nba.repository.GameResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GameService {

    /** Au-delà, la route publique renverrait des milliers de matchs d'un coup : utiliser la saison ou la pagination. */
    static final Duration MAX_DATE_RANGE = Duration.ofDays(31);

    private final GameRepository gameRepository;
    private final GameResultRepository gameResultRepository;
    private final TeamService teamService;
    private final SeasonService seasonService;
    private final Clock clock;

    public Game createGame(GameCreateRequest request) {
        if (request.getHomeTeamId().equals(request.getAwayTeamId())) {
            throw new AppException(
                    "Home team and away team cannot be the same",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_GAME_DATA"
            );
        }
        Team homeTeam = teamService.getTeamById(request.getHomeTeamId());
        Team awayTeam = teamService.getTeamById(request.getAwayTeamId());
        Season season = seasonService.getSeasonById(request.getSeasonId());

        Game game = Game.builder()
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .season(season)
                .gameType(request.getGameType())
                .scheduledDate(request.getScheduledDate())
                .venue(request.getVenue())
                .isNationalTv(request.getIsNationalTv())
                .broadcastNetwork(request.getBroadcastNetwork())
                .build();

        log.info("Creating game: {} vs {}", homeTeam.getName(), awayTeam.getName());
        return gameRepository.save(game);
    }

    public Game updateGame(Long id, GameUpdateRequest request) {
        log.info("Updating game with id: {}", id);

        Game existingGame = getGameById(id);

        if (request.getScheduledDate() != null) {
            existingGame.setScheduledDate(request.getScheduledDate());
        }
        if (request.getActualStartTime() != null) {
            existingGame.setActualStartTime(request.getActualStartTime());
        }
        if (request.getActualEndTime() != null) {
            existingGame.setActualEndTime(request.getActualEndTime());
        }
        if (request.getVenue() != null) {
            existingGame.setVenue(request.getVenue());
        }
        if (request.getAttendance() != null) {
            existingGame.setAttendance(request.getAttendance());
        }
        if (request.getIsNationalTv() != null) {
            existingGame.setIsNationalTv(request.getIsNationalTv());
        }
        if (request.getBroadcastNetwork() != null) {
            existingGame.setBroadcastNetwork(request.getBroadcastNetwork());
        }

        return gameRepository.save(existingGame);
    }

    @Transactional(readOnly = true)
    public Game getGameById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Game not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "GAME_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public Page<Game> getAllGames(Pageable pageable) {
        return gameRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Game> getGamesByStatus(GameStatus status, Pageable pageable) {
        return gameRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public List<Game> getGamesBySeason(Long seasonId) {
        Season season = seasonService.getSeasonById(seasonId);
        return gameRepository.findBySeason(season);
    }

    @Transactional(readOnly = true)
    public Page<Game> getGamesByTeam(Long teamId, Pageable pageable) {
        Team team = teamService.getTeamById(teamId);
        return gameRepository.findByTeam(team, pageable);
    }

    @Transactional(readOnly = true)
    public List<Game> getGamesByDateRange(LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start) || Duration.between(start, end).compareTo(MAX_DATE_RANGE) > 0) {
            throw new AppException(
                    "La période doit aller du plus ancien au plus récent et durer au plus " + MAX_DATE_RANGE.toDays() + " jours",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE"
            );
        }
        return gameRepository.findByScheduledDateBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<Game> getGamesBySeasonAndTeam(Long seasonId, Long teamId) {
        Season season = seasonService.getSeasonById(seasonId);
        Team team = teamService.getTeamById(teamId);
        return gameRepository.findBySeasonAndTeam(season, team);
    }

    public void updateGameStatus(Long id, GameStatus status) {
        log.info("Updating game status for id: {} to {}", id, status);

        Game game = getGameById(id);
        game.setStatus(status);

        if (status == GameStatus.IN_PROGRESS && game.getActualStartTime() == null) {
            game.setActualStartTime(LocalDateTime.now(clock));
        } else if (status == GameStatus.COMPLETED && game.getActualEndTime() == null) {
            game.setActualEndTime(LocalDateTime.now(clock));
        }

        gameRepository.save(game);
    }

    public void deleteGame(Long id) {
        log.info("Deleting game with id: {}", id);

        if (!gameRepository.existsById(id)) {
            throw new AppException(
                    "Game not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "GAME_NOT_FOUND"
            );
        }

        gameRepository.deleteById(id);
    }

    /**
     * Enregistre (ou corrige) le score final d'un match et le marque comme terminé. La forme des équipes, donc les
     * prédictions suivantes, en tient compte immédiatement : elle est recalculée à chaque prédiction.
     */
    public GameResult recordResult(Long id, GameResultRequest request) {
        if (request.getHomeTeamScore().equals(request.getAwayTeamScore())) {
            throw new AppException("Un match NBA ne se termine pas sur une égalité : vérifiez les scores.",
                    HttpStatus.BAD_REQUEST, "INVALID_GAME_RESULT");
        }
        Game game = getGameById(id);
        boolean homeWon = request.getHomeTeamScore() > request.getAwayTeamScore();
        int overtimePeriods = request.getOvertimePeriods() == null ? 0 : request.getOvertimePeriods();

        GameResult result = gameResultRepository.findByGame(game).orElseGet(() -> GameResult.builder().game(game).build());
        result.setHomeTeamScore(request.getHomeTeamScore());
        result.setAwayTeamScore(request.getAwayTeamScore());
        result.setScoreDifference(Math.abs(request.getHomeTeamScore() - request.getAwayTeamScore()));
        result.setWinner(homeWon ? game.getHomeTeam() : game.getAwayTeam());
        result.setLoser(homeWon ? game.getAwayTeam() : game.getHomeTeam());
        result.setOvertime(overtimePeriods > 0);
        result.setOvertimePeriods(overtimePeriods > 0 ? overtimePeriods : null);
        result.setRecordedAt(LocalDateTime.now(clock));

        game.setStatus(GameStatus.COMPLETED);
        gameRepository.save(game);
        log.info("Score saisi pour le match {} : {}-{}", id, request.getHomeTeamScore(), request.getAwayTeamScore());
        return gameResultRepository.save(result);
    }

    /** Retire un score saisi par erreur : le match redevient « à venir ». */
    public void deleteResult(Long id) {
        Game game = getGameById(id);
        GameResult result = gameResultRepository.findByGame(game).orElseThrow(() -> new AppException(
                "Ce match n'a pas de score enregistré.", HttpStatus.NOT_FOUND, "GAME_RESULT_NOT_FOUND"));
        gameResultRepository.delete(result);
        game.setStatus(GameStatus.SCHEDULED);
        gameRepository.save(game);
        log.info("Score retiré pour le match {}", id);
    }

    /** Résultats des matchs déjà joués, indexés par identifiant de match (en une requête). */
    @Transactional(readOnly = true)
    public Map<Long, GameResult> getResults(Collection<Game> games) {
        if (games.isEmpty()) {
            return Map.of();
        }
        return gameResultRepository.findByGameIdIn(games.stream().map(Game::getId).toList()).stream()
                .collect(Collectors.toMap(result -> result.getGame().getId(), Function.identity()));
    }
}
