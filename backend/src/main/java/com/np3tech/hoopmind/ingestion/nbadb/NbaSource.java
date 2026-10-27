package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.GameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.StandingRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamGameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamRow;

import java.util.List;

/**
 * Une source de matchs NBA, ramenée aux lignes de {@link NbaDatabaseRows} : la base de Wyatt Walsh
 * ({@link NbaDatabaseReader}) ou les saisons récentes des fichiers d'Eoin Moore ({@link EoinMooreSource}).
 * {@link NbaDatabaseImporter} les importe de la même façon.
 */
interface NbaSource extends AutoCloseable {

    /** Les {@code count} saisons les plus récentes qui ont des matchs de saison régulière, de la plus ancienne à la plus récente. */
    List<String> recentSeasons(int count);

    List<TeamRow> teams();

    List<GameRow> games(List<String> seasons);

    List<TeamGameRow> teamGames(List<String> seasons);

    List<StandingRow> standings(List<String> seasons);

    @Override
    void close();
}
