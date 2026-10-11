package com.np3tech.hoopmind.nba.importer.csv;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ReferenceCsvImportTests {

    private static final String AWARDS = """
            season,award,player,player_id,age,first,pts_won,pts_max,share,winner
            2025,nba mvp,Shai Gilgeous-Alexander,gilgesh01,26,71,913,1000,0.913,TRUE
            2025,nba mvp,Nikola Jokić,jokicni01,29,29,787,1000,0.787,FALSE
            1983,nba smoy,Bobby Jones,NA,31,NA,NA,NA,NA,TRUE
            """;

    private static final String TEAMS = """
            season,lg,team,abbreviation,playoffs,age,w,l,pw,pl,mov,sos,srs,o_rtg,d_rtg,n_rtg,pace,f_tr,x3p_ar,ts_percent,e_fg_percent,tov_percent,orb_percent,ft_fga,opp_e_fg_percent,opp_tov_percent,drb_percent,opp_ft_fga,arena,attend,attend_g
            2025,NBA,Oklahoma City Thunder,OKC,TRUE,25.6,68,14,67,15,12.9,-0.2,12.7,119.2,106.6,12.6,99.9,0.24,0.42,0.59,0.56,10.0,24.0,0.19,0.51,15.9,76.0,0.22,"Paycom Center",742034,18098
            2025,NBA,League Average,NA,FALSE,26.9,NA,NA,41,41,0,0,0,114.5,114.5,NA,98.7,0.25,0.42,0.58,0.54,12.4,25.4,0.19,0.54,12.4,74.6,0.19,NA,NA,NA
            """;

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void emptyTable() {
        jdbc.update("DELETE FROM reference_csv_rows");
    }

    @Test
    void recognisesFilesByTheirColumnsAndNeverDuplicatesRows() throws IOException {
        String admin = TestAccounts.admin(mvc).bearer();
        MockMultipartFile archive = new MockMultipartFile("files", "archive.zip", "application/zip",
                zip(Map.of("Player Award Shares.csv", AWARDS, "notes/readme.txt", "ignoré",
                        "nom-quelconque.csv", TEAMS)));
        MockMultipartFile unknown = new MockMultipartFile("files", "matchs.csv", "text/csv",
                "date,home,away\n2025-01-01,BOS,LAL\n".getBytes(StandardCharsets.UTF_8));

        assertThat(mvc.post().uri("/api/v1/admin/imports/csv").multipart().file(archive).file(unknown)
                .header(HttpHeaders.AUTHORIZATION, admin))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.length()").isEqualTo(3);
                    assertThat(json).extractingPath("$[?(@.file=='Player Award Shares.csv')].dataset").asArray()
                            .containsExactly("AWARD_SHARES");
                    assertThat(json).extractingPath("$[?(@.file=='Player Award Shares.csv')].inserted").asArray()
                            .containsExactly(3);
                    assertThat(json).extractingPath("$[?(@.file=='nom-quelconque.csv')].dataset").asArray()
                            .containsExactly("TEAM_SUMMARIES");
                    assertThat(json).extractingPath("$[?(@.file=='matchs.csv')].status").asArray()
                            .containsExactly("UNRECOGNIZED");
                });

        Map<String, Object> mvp = jdbc.queryForMap(
                "SELECT season, data->>'share' AS share, data->'winner' AS winner, data->'first' AS first "
                        + "FROM reference_csv_rows WHERE dataset = 'AWARD_SHARES' AND row_key = '2025|nba mvp|gilgesh01|Shai Gilgeous-Alexander'");
        assertThat(mvp.get("season")).isEqualTo(2025);
        assertThat(mvp.get("share")).isEqualTo("0.913");
        assertThat(mvp.get("winner").toString()).isEqualTo("true");
        assertThat(jdbc.queryForObject("SELECT data->'w' IS NULL OR data->>'w' IS NULL FROM reference_csv_rows "
                + "WHERE row_key = '2025|NBA|League Average'", Boolean.class)).isTrue();

        // Même fichier renvoyé, avec un vote corrigé : une ligne modifiée, les autres inchangées, aucun doublon.
        MockMultipartFile corrected = new MockMultipartFile("files", "awards.csv", "text/csv",
                AWARDS.replace("0.787", "0.788").getBytes(StandardCharsets.UTF_8));
        assertThat(mvc.post().uri("/api/v1/admin/imports/csv").multipart().file(corrected)
                .header(HttpHeaders.AUTHORIZATION, admin))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[0].inserted").isEqualTo(0);
                    assertThat(json).extractingPath("$[0].updated").isEqualTo(1);
                    assertThat(json).extractingPath("$[0].unchanged").isEqualTo(2);
                });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reference_csv_rows", Integer.class)).isEqualTo(5);

        assertThat(mvc.get().uri("/api/v1/admin/imports/csv/datasets").header(HttpHeaders.AUTHORIZATION, admin))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.length()").isEqualTo(ReferenceDataset.values().length);
                    assertThat(json).extractingPath("$[?(@.dataset=='AWARD_SHARES')].rows").asArray().containsExactly(3);
                    assertThat(json).extractingPath("$[?(@.dataset=='AWARD_SHARES')].firstSeason").asArray()
                            .containsExactly(1983);
                });
    }

    @Test
    void onlyAdministratorsCanImport() {
        MockMultipartFile file = new MockMultipartFile("files", "awards.csv", "text/csv", AWARDS.getBytes(StandardCharsets.UTF_8));
        assertThat(mvc.post().uri("/api/v1/admin/imports/csv").multipart().file(file)
                .header(HttpHeaders.AUTHORIZATION, TestAccounts.register(mvc).bearer()))
                .hasStatus(403);
    }

    private static byte[] zip(Map<String, String> files) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
