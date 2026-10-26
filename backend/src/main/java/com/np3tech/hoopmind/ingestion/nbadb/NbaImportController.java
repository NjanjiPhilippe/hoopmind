package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.EoinMooreJobService.EoinMooreJobStatus;
import com.np3tech.hoopmind.ingestion.nbadb.EoinMooreJobService.EoinMooreStatus;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseJobService.JobStatus;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseJobService.NbaDatabaseStatus;
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
    private final EoinMooreJobService eoinMoore;

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

    @GetMapping("/eoin-moore/status")
    @Operation(operationId = "getEoinMooreStatus", summary = "Saisons récentes (fichiers d'Eoin Moore) : fichiers et dernier import",
            description = "Présence de Games.csv et TeamStatistics.csv dans le dossier du serveur, saison proposée et "
                    + "avancement du dernier import.")
    EoinMooreStatus getEoinMooreStatus() {
        return eoinMoore.status();
    }

    @PostMapping("/eoin-moore/jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(operationId = "startEoinMooreImport", summary = "Importer des saisons récentes depuis les fichiers d'Eoin Moore",
            description = "Lit Games.csv et TeamStatistics.csv (Kaggle eoinamoore/historical-nba-data-and-player-box-scores) "
                    + "et importe les matchs de saison régulière, de play-in et de playoffs des saisons fromYear à toYear. "
                    + "Suivre l'avancement avec GET /eoin-moore/status.")
    EoinMooreJobStatus startEoinMooreImport(
            @Parameter(description = "Année de début de la première saison (2023 pour 2023-24) ; par défaut celle qui "
                    + "suit la dernière saison en base")
            @RequestParam(required = false) Integer fromYear,
            @Parameter(description = "Année de début de la dernière saison ; par défaut fromYear")
            @RequestParam(required = false) Integer toYear) {
        return eoinMoore.start(fromYear, toYear);
    }
}
