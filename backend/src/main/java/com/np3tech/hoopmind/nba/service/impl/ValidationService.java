package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.user.domain.User;

import com.np3tech.hoopmind.nba.entity.*;
import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import com.np3tech.hoopmind.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for complex business rule validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationService {

    /**
     * Validate if prediction can be made for a game
     */
    public void validatePredictionEligibility(Game game, User user) {
        // Check if game has already started
        if (game.getActualStartTime() != null && game.getActualStartTime().isBefore(LocalDateTime.now())) {
            throw new AppException(
                    "Cannot make prediction for a game that has already started",
                    HttpStatus.BAD_REQUEST,
                    "PREDICTION_TOO_LATE"
            );
        }

        // Check if game is finished
        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new AppException(
                    "Cannot make prediction for a finished game",
                    HttpStatus.BAD_REQUEST,
                    "GAME_ALREADY_FINISHED"
            );
        }

        // Check if prediction is made too close to game time (e.g., within 5 minutes)
        if (game.getScheduledDate().minusMinutes(5).isBefore(LocalDateTime.now())) {
            throw new AppException(
                    "Predictions must be made at least 5 minutes before game time",
                    HttpStatus.BAD_REQUEST,
                    "PREDICTION_DEADLINE_PASSED"
            );
        }

        log.debug("Prediction eligibility validated for user {} and game {}", user.getId(), game.getId());
    }

    /**
     * Validate if game result can be recorded
     */
    public void validateGameResultEligibility(Game game) {
        // Check if game is in progress or scheduled
        if (game.getStatus() != GameStatus.IN_PROGRESS && game.getStatus() != GameStatus.COMPLETED) {
            throw new AppException(
                    "Game must be in progress or finished to record results",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_GAME_STATUS"
            );
        }

        // Check if game has started
        if (game.getActualStartTime() == null) {
            throw new AppException(
                    "Cannot record results for a game that hasn't started",
                    HttpStatus.BAD_REQUEST,
                    "GAME_NOT_STARTED"
            );
        }

        log.debug("Game result eligibility validated for game {}", game.getId());
    }

    /**
     * Validate season dates
     */
    public void validateSeasonDates(Season season) {
        if (season.getStartDate().isAfter(season.getEndDate())) {
            throw new AppException(
                    "Season start date must be before end date",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SEASON_DATES"
            );
        }

        if (season.getPlayoffsStartDate() != null) {
            if (season.getPlayoffsStartDate().isBefore(season.getStartDate()) ||
                    season.getPlayoffsStartDate().isAfter(season.getEndDate())) {
                throw new AppException(
                        "Playoffs start date must be within season dates",
                        HttpStatus.BAD_REQUEST,
                        "INVALID_PLAYOFFS_DATE"
                );
            }
        }

        if (season.getFinalsStartDate() != null && season.getPlayoffsStartDate() != null) {
            if (season.getFinalsStartDate().isBefore(season.getPlayoffsStartDate())) {
                throw new AppException(
                        "Finals start date must be after playoffs start date",
                        HttpStatus.BAD_REQUEST,
                        "INVALID_FINALS_DATE"
                );
            }
        }

        log.debug("Season dates validated for season {}", season.getName());
    }

    /**
     * Validate game scheduling
     */
    public void validateGameScheduling(Game game) {
        // Check if teams are from the same season
        if (!game.getHomeTeam().equals(game.getAwayTeam())) {
            if (game.getHomeTeam().getId().equals(game.getAwayTeam().getId())) {
                throw new AppException(
                        "Home team and away team cannot be the same",
                        HttpStatus.BAD_REQUEST,
                        "SAME_TEAM_MATCHUP"
                );
            }
        }

        // Check if game is scheduled within season dates
        if (game.getSeason() != null) {
            if (game.getScheduledDate().toLocalDate().isBefore(game.getSeason().getStartDate()) ||
                    game.getScheduledDate().toLocalDate().isAfter(game.getSeason().getEndDate())) {
                throw new AppException(
                        "Game must be scheduled within season dates",
                        HttpStatus.BAD_REQUEST,
                        "GAME_OUTSIDE_SEASON"
                );
            }
        }

        log.debug("Game scheduling validated for game between {} and {}",
                game.getHomeTeam().getName(), game.getAwayTeam().getName());
    }

    /**
     * Validate player eligibility for stats recording
     */
    public void validatePlayerStatsEligibility(Player player, Game game) {
        // Check if player's team is playing in the game
        if (!player.getTeam().getId().equals(game.getHomeTeam().getId()) &&
                !player.getTeam().getId().equals(game.getAwayTeam().getId())) {
            throw new AppException(
                    "Player's team is not participating in this game",
                    HttpStatus.BAD_REQUEST,
                    "PLAYER_TEAM_MISMATCH"
            );
        }

        // Check if player is active
        if (player.getStatus().name().equals("INACTIVE")) {
            throw new AppException(
                    "Cannot record stats for inactive player",
                    HttpStatus.BAD_REQUEST,
                    "PLAYER_INACTIVE"
            );
        }

        log.debug("Player stats eligibility validated for player {} in game {}", player.getId(), game.getId());
    }

    /**
     * Validate score values
     */
    public void validateScores(Integer homeScore, Integer awayScore) {
        if (homeScore == null || awayScore == null) {
            throw new AppException(
                    "Both scores must be provided",
                    HttpStatus.BAD_REQUEST,
                    "MISSING_SCORES"
            );
        }

        if (homeScore < 0 || awayScore < 0) {
            throw new AppException(
                    "Scores cannot be negative",
                    HttpStatus.BAD_REQUEST,
                    "NEGATIVE_SCORE"
            );
        }

        if (homeScore == awayScore) {
            throw new AppException(
                    "Basketball games cannot end in a tie",
                    HttpStatus.BAD_REQUEST,
                    "TIE_GAME"
            );
        }

        // Realistic score validation (optional)
        if (homeScore > 200 || awayScore > 200) {
            log.warn("Unusually high score detected: Home {} - Away {}", homeScore, awayScore);
        }

        log.debug("Scores validated: Home {} - Away {}", homeScore, awayScore);
    }

    /**
     * Validate username format
     */
    public void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new AppException(
                    "Username cannot be empty",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_USERNAME"
            );
        }

        if (username.length() < 3 || username.length() > 20) {
            throw new AppException(
                    "Username must be between 3 and 20 characters",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_USERNAME_LENGTH"
            );
        }

        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            throw new AppException(
                    "Username can only contain letters, numbers, underscores, and hyphens",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_USERNAME_FORMAT"
            );
        }

        log.debug("Username validated: {}", username);
    }

    /**
     * Validate email format
     */
    public void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new AppException(
                    "Email cannot be empty",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_EMAIL"
            );
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!email.matches(emailRegex)) {
            throw new AppException(
                    "Invalid email format",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_EMAIL_FORMAT"
            );
        }

        log.debug("Email validated: {}", email);
    }

    /**
     * Validate password strength
     */
    public void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new AppException(
                    "Password cannot be empty",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PASSWORD"
            );
        }

        if (password.length() < 8) {
            throw new AppException(
                    "Password must be at least 8 characters long",
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_TOO_SHORT"
            );
        }

        // Check for at least one uppercase, one lowercase, one digit
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasUppercase || !hasLowercase || !hasDigit) {
            throw new AppException(
                    "Password must contain at least one uppercase letter, one lowercase letter, and one digit",
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_TOO_WEAK"
            );
        }

        log.debug("Password strength validated");
    }
}
