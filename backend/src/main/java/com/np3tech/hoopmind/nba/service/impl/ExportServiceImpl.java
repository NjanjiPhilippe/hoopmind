package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.entity.*;
import com.np3tech.hoopmind.nba.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * Service for exporting data in various formats (CSV, JSON, etc.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {

    /**
     * Export team stats to CSV
     */
    public byte[] exportTeamStatsToCSV(List<TeamStats> stats) {
        log.info("Exporting team stats to CSV");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(baos)) {

            // CSV Header
            writer.println("Team,Season,Wins,Losses,Win %,PPG,OPP PPG,Diff,Home Record,Away Record");

            // Data rows
            for (TeamStats stat : stats) {
                writer.printf("%s,%s,%d,%d,%.3f,%.2f,%.2f,%.2f,%d-%d,%d-%d%n",
                        stat.getTeam().getName(),
                        stat.getSeason().getName(),
                        stat.getWins(),
                        stat.getLosses(),
                        stat.getWinPercentage(),
                        stat.getPointsPerGame(),
                        stat.getPointsAllowedPerGame(),
                        stat.getPointsDifferential(),
                        stat.getHomeWins(), stat.getHomeLosses(),
                        stat.getAwayWins(), stat.getAwayLosses()
                );
            }

            writer.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error exporting team stats to CSV", e);
            throw new RuntimeException("Failed to export team stats", e);
        }
    }

    /**
     * Export player stats to CSV
     */
    public byte[] exportPlayerStatsToCSV(List<PlayerStats> stats) {
        log.info("Exporting player stats to CSV");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(baos)) {

            // CSV Header
            writer.println("Player,Team,Season,GP,GS,PPG,RPG,APG,FG%,3P%,FT%");

            // Data rows
            for (PlayerStats stat : stats) {
                writer.printf("%s %s,%s,%s,%d,%d,%.2f,%.2f,%.2f,%.3f,%.3f,%.3f%n",
                        stat.getPlayer().getFirstName(),
                        stat.getPlayer().getLastName(),
                        stat.getPlayer().getTeam().getName(),
                        stat.getSeason().getName(),
                        stat.getGamesPlayed(),
                        stat.getGamesStarted(),
                        stat.getPointsPerGame(),
                        stat.getTotalReboundsPerGame(),
                        stat.getAssistsPerGame(),
                        stat.getFieldGoalPercentage(),
                        stat.getThreePointPercentage(),
                        stat.getFreeThrowPercentage()
                );
            }

            writer.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error exporting player stats to CSV", e);
            throw new RuntimeException("Failed to export player stats", e);
        }
    }

    /**
     * Export game schedule to CSV
     */
    public byte[] exportGameScheduleToCSV(List<Game> games) {
        log.info("Exporting game schedule to CSV");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(baos)) {

            // CSV Header
            writer.println("Date,Time,Home Team,Away Team,Venue,Status,Broadcast");

            // Data rows
            for (Game game : games) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                        game.getScheduledDate().toLocalDate(),
                        game.getScheduledDate().toLocalTime(),
                        game.getHomeTeam().getName(),
                        game.getAwayTeam().getName(),
                        game.getVenue() != null ? game.getVenue() : "",
                        game.getStatus(),
                        game.getBroadcastNetwork() != null ? game.getBroadcastNetwork() : ""
                );
            }

            writer.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error exporting game schedule to CSV", e);
            throw new RuntimeException("Failed to export game schedule", e);
        }
    }
}
