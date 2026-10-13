package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseJobService.JobStatus;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseJobService.NbaDatabaseStatus;
import com.np3tech.hoopmind.ingestion.nbadb.StatsNbaJobService.StatsNbaJobStatus;
import com.np3tech.hoopmind.ingestion.nbadb.StatsNbaJobService.StatsNbaStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/imports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Import des données")
@SecurityRequirement(name = "bearerAuth")
/**
 * L'import ne se lance qu'en arrière-plan, via {@link NbaDatabaseJobService} : il prend le verrou des données,
 * ne bloque pas un fil HTTP pendant plusieurs minutes et son avancement est visible sur la page Données.
 */
class NbaImportController {

    private final NbaDatabaseJobService jobs;
    private final StatsNbaJobService statsNba;

    @GetMapping("/nba-database/status")
    @Operation(operationId = "getNbaDatabaseStatus", summary = "État de la base NBA (Wyatt Walsh)",
            description = "Fichier présent sur le serveur, clé Kaggle configurée ou non, et avancement du dernier import.")
    NbaDatabaseStatus getNbaDatabaseStatus() {
        return jobs.status();
    }

    @PostMapping("/nba-database/jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(operationId = "startNbaDatabaseImport", summary = "Lancer l'import de la base NBA en arrière-plan",
            description = "Télécharge d'abord la base depuis Kaggle si download=true (clé KAGGLE_USERNAME et KAGGLE_KEY "
                    + "requise), puis l'importe. Suivre l'avancement avec GET /nba-database/status.")
    JobStatus startNbaDatabaseImport(
            @Parameter(description = "Nombre de saisons à importer, en partant de la plus récente (réglage par défaut sinon)")
            @RequestParam(required = false) Integer seasons,
            @Parameter(description = "Télécharger la base depuis Kaggle avant l'import")
            @RequestParam(defaultValue = "false") boolean download) {
        return jobs.start(seasons, download);
    }

    @GetMapping("/stats-nba/status")
    @Operation(operationId = "getStatsNbaStatus", summary = "Complément depuis stats.nba.com : saisons visées et dernier import",
            description = "Saisons qu'un import lirait (de la dernière saison en base à la saison en cours) et avancement "
                    + "du dernier import.")
    StatsNbaStatus getStatsNbaStatus() {
        return statsNba.status();
    }

    @PostMapping("/stats-nba/jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(operationId = "startStatsNbaImport", summary = "Compléter les matchs récents depuis stats.nba.com",
            description = "Lit sur stats.nba.com (public, sans clé) les matchs de saison régulière et de playoffs, de la "
                    + "saison fromYear jusqu'à la saison en cours, puis les importe. Suivre l'avancement avec "
                    + "GET /stats-nba/status.")
    StatsNbaJobStatus startStatsNbaImport(
            @Parameter(description = "Année de début de la première saison (2023 pour 2023-24) ; par défaut la dernière "
                    + "saison en base")
            @RequestParam(required = false) Integer fromYear) {
        return statsNba.start(fromYear);
    }
}
