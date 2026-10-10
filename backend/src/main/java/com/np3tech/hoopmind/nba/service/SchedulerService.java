package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import com.np3tech.hoopmind.nba.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for scheduled tasks and automated processes
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SchedulerService {

    private final GameRepository gameRepository;
    private final DataSyncService dataSyncService;
    private final CacheService cacheService;
    private final NotificationService notificationService;

    /**
     * Check for games that should be marked as in progress
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void checkGameStatuses() {
        log.debug("Checking game statuses...");

        LocalDateTime now = LocalDateTime.now();

        // Find games scheduled to start but not yet marked as in progress
        List<Game> gamesToStart = gameRepository.findByStatusAndScheduledDateBefore(
                GameStatus.SCHEDULED,
                now.plusMinutes(10) // Start checking 10 minutes before scheduled time
        );

        for (Game game : gamesToStart) {
            if (game.getScheduledDate().isBefore(now) && game.getActualStartTime() == null) {
                log.info("Marking game {} as in progress", game.getId());
                game.setStatus(GameStatus.IN_PROGRESS);
                game.setActualStartTime(now);
                gameRepository.save(game);
            }
        }
    }

    /**
     * Update daily statistics
     * Runs daily at 4 AM
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void updateDailyStatistics() {
        log.info("Updating daily statistics...");

        try {
            // This would trigger calculation of yesterday's statistics
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

            // Find all games completed yesterday and update stats
            List<Game> completedGames = gameRepository.findByScheduledDateBetween(
                    yesterday.withHour(0).withMinute(0),
                    yesterday.withHour(23).withMinute(59)
            );

            for (Game game : completedGames) {
                if (game.getStatus() == GameStatus.COMPLETED) {
                    dataSyncService.updateGameStatistics(game.getId());
                    dataSyncService.evaluateGamePredictions(game.getId());
                }
            }

            log.info("Daily statistics updated successfully for {} games", completedGames.size());
        } catch (Exception e) {
            log.error("Error updating daily statistics", e);
        }
    }

    /**
     * Clear application caches
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void clearCaches() {
        log.info("Clearing application caches...");

        try {
            cacheService.clearAllCaches();
            log.info("Application caches cleared successfully");
        } catch (Exception e) {
            log.error("Error clearing caches", e);
        }
    }

    /**
     * Send game reminders to users
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void sendGameReminders() {
        log.debug("Checking for upcoming games to send reminders...");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime twoHoursFromNow = now.plusHours(2);

            // Find games starting in the next 2 hours
            List<Game> upcomingGames = gameRepository.findByScheduledDateBetween(
                    now,
                    twoHoursFromNow
            );

            for (Game game : upcomingGames) {
                if (game.getStatus() == GameStatus.SCHEDULED) {
                    // Send notifications to users who follow these teams
                    // This would require a user subscription/follow system
                    log.debug("Game reminder would be sent for game {}", game.getId());
                }
            }
        } catch (Exception e) {
            log.error("Error sending game reminders", e);
        }
    }

    /**
     * Generate weekly statistics report
     * Runs every Monday at 8 AM
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public void generateWeeklyReport() {
        log.info("Generating weekly statistics report...");

        try {
            // Generate reports for the previous week
            LocalDateTime weekStart = LocalDateTime.now().minusWeeks(1).withHour(0).withMinute(0);
            LocalDateTime weekEnd = LocalDateTime.now().withHour(0).withMinute(0);

            List<Game> weekGames = gameRepository.findByScheduledDateBetween(weekStart, weekEnd);

            log.info("Weekly report generated: {} games played last week", weekGames.size());

            // Here you could send email reports, generate PDFs, etc.
        } catch (Exception e) {
            log.error("Error generating weekly report", e);
        }
    }

    /**
     * Health check - verify system status
     * Runs every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void healthCheck() {
        log.debug("Performing system health check...");

        try {
            // Check database connectivity
            long gameCount = gameRepository.count();

            // Check for any stuck games (in progress for too long)
            LocalDateTime fourHoursAgo = LocalDateTime.now().minusHours(4);
            List<Game> stuckGames = gameRepository.findByStatus(GameStatus.IN_PROGRESS)
                    .stream()
                    .filter(g -> g.getActualStartTime() != null &&
                            g.getActualStartTime().isBefore(fourHoursAgo))
                    .toList();

            if (!stuckGames.isEmpty()) {
                log.warn("Found {} games stuck in progress state", stuckGames.size());
            }

            log.debug("Health check completed - {} total games in database", gameCount);
        } catch (Exception e) {
            log.error("Health check failed", e);
        }
    }
}