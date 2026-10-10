package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.GameOdds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameOddsRepository extends JpaRepository<GameOdds, Long> {

    List<GameOdds> findByGame(Game game);

    List<GameOdds> findByBookmaker(String bookmaker);

    Optional<GameOdds> findByGameAndBookmaker(Game game, String bookmaker);

    @Query("SELECT go FROM GameOdds go WHERE go.game = :game ORDER BY go.createdAt DESC")
    List<GameOdds> findLatestOddsByGame(@Param("game") Game game);

    List<GameOdds> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    void deleteByGame(Game game);
}