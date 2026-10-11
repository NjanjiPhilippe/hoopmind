package com.np3tech.hoopmind.nba.importer;

import com.np3tech.hoopmind.nba.importer.NbaDatabaseImporter.ImportReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/imports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Import des données")
@SecurityRequirement(name = "bearerAuth")
class NbaImportController {

    private final NbaDatabaseImporter importer;

    @PostMapping("/nba-database")
    @Operation(operationId = "importNbaDatabase", summary = "Importer la base NBA (Wyatt Walsh)",
            description = "Lit le fichier nba.sqlite configuré (NBA_DATABASE_PATH) et importe équipes, saisons, matchs, "
                    + "résultats, statistiques par match et bilans de saison. Rejouable sans doublon.")
    ImportReport importNbaDatabase(
            @Parameter(description = "Nombre de saisons à importer, en partant de la plus récente (réglage par défaut sinon)")
            @RequestParam(required = false) Integer seasons) {
        return importer.importDatabase(seasons);
    }
}
