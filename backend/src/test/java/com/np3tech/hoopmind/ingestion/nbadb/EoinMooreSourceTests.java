package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.GameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamGameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static com.np3tech.hoopmind.ingestion.nbadb.EoinMooreFixture.BOS;
import static com.np3tech.hoopmind.ingestion.nbadb.EoinMooreFixture.LAL;
import static com.np3tech.hoopmind.ingestion.nbadb.EoinMooreFixture.NYK;
import static com.np3tech.hoopmind.ingestion.nbadb.EoinMooreFixture.POR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EoinMooreSourceTests {

    private static final int[] REGULATION = {30, 25, 28, 27};

    @TempDir
    Path directory;

    @Test
    void keepsRegularSeasonPlayInAndPlayoffGamesOfTheRequestedSeasons() throws Exception {
        new EoinMooreFixture()
                .played("22200001", "2022-10-18", BOS, NYK, REGULATION, new int[]{20, 20, 20, 20})   // 2022-23 : hors fenêtre
                .played("12300001", "2023-10-05", BOS, NYK, REGULATION, new int[]{20, 20, 20, 20})   // présaison
                .played("22300001", "2023-10-24", BOS, NYK, new int[]{30, 25, 28, 27, 10, 8}, new int[]{28, 27, 30, 25, 10, 6})
                .played("62300001", "2023-12-09", LAL, POR, REGULATION, new int[]{20, 20, 20, 20})   // finale de la NBA Cup
                .played("52300101", "2024-04-16", LAL, POR, REGULATION, new int[]{20, 20, 20, 20})
                .played("42300101", "2024-04-20", BOS, NYK, REGULATION, new int[]{20, 20, 20, 20})
                .scheduled("22400001", "2024-10-22", NYK, BOS)
                .write(directory);

        EoinMooreSource source = EoinMooreSource.read(directory, 2023, 2024);

        assertThat(source.recentSeasons(5)).containsExactly("2023-24", "2024-25");
        List<GameRow> games = source.games(List.of("2023-24"));
        assertThat(games).extracting(GameRow::gameId).containsExactly("0022300001", "0052300101", "0042300101");
        GameRow overtime = games.getFirst();
        assertThat(overtime.gameDate()).isEqualTo(LocalDate.of(2023, 10, 24));
        assertThat(overtime.homeTeamId()).isEqualTo(BOS);
        assertThat(overtime.homePoints()).isEqualTo(128);
        assertThat(overtime.arena()).isEqualTo("TD Garden");
        assertThat(overtime.homePeriods()).containsExactly(30, 25, 28, 27, 10, 8);
        assertThat(overtime.overtimePeriods()).isEqualTo(2);
        assertThat(games.get(1).playIn()).isTrue();
        assertThat(games.get(1).regularSeason()).isFalse();

        GameRow scheduled = source.games(List.of("2024-25")).getFirst();
        assertThat(scheduled.played()).isFalse();
        assertThat(scheduled.homePeriods()).isEmpty();

        assertThat(source.teamGames(List.of("2023-24")))
                .filteredOn(line -> line.gameId().equals("0022300001") && line.teamId() == BOS)
                .singleElement()
                .satisfies(line -> {
                    assertThat(line.points()).isEqualTo(128);
                    assertThat(line.fga()).isEqualTo(88);
                    assertThat(line.reb()).isEqualTo(43);
                })
                .extracting(TeamGameRow::tov).isEqualTo(13);

        assertThat(source.teams()).extracting(TeamRow::abbreviation).containsExactlyInAnyOrder("BOS", "NYK", "LAL", "POR");
        assertThat(source.teams()).filteredOn(team -> team.teamId() == POR).singleElement()
                .satisfies(team -> {
                    assertThat(team.fullName()).isEqualTo("Portland Trail Blazers");
                    assertThat(team.city()).isEqualTo("Portland");
                    assertThat(team.conference()).isEqualTo("West");
                    assertThat(team.division()).isEqualTo("Northwest");
                });
        // Le play-in et les playoffs ne comptent pas dans les classements de saison régulière.
        assertThat(source.standings(List.of("2023-24"))).extracting(row -> row.teamId())
                .containsExactlyInAnyOrder(BOS, NYK);
    }

    @Test
    void normalisesGameIdsAndReadsTheSeasonFromThem() {
        assertThat(EoinMooreSource.gameId("22300061")).isEqualTo("0022300061");
        assertThat(EoinMooreSource.gameId("22300061.0")).isEqualTo("0022300061");
        assertThat(EoinMooreSource.gameId("0042300401")).isEqualTo("0042300401");
        assertThat(EoinMooreSource.gameId("abc")).isNull();
        assertThat(EoinMooreSource.seasonYear("0022300061", null)).isEqualTo(2023);
        assertThat(EoinMooreSource.seasonYear("0029900001", null)).isEqualTo(1999);
        // Playoffs 2019-20 joués en août 2020 : l'identifiant prime sur la date.
        assertThat(EoinMooreSource.seasonYear("0041900401", LocalDate.of(2020, 9, 30))).isEqualTo(2019);
    }

    @Test
    void explainsWhichFileIsMissing() {
        assertThatThrownBy(() -> EoinMooreSource.read(directory, 2023, 2023))
                .isInstanceOf(NbaImportException.class)
                .hasMessageContaining("TeamStatistics.csv introuvable");
    }
}
