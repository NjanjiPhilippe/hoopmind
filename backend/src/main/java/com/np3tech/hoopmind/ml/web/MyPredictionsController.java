package com.np3tech.hoopmind.ml.web;

import com.np3tech.hoopmind.ml.history.PredictionHistory;
import com.np3tech.hoopmind.ml.history.PredictionHistoryEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** L'historique des prédictions de l'utilisateur connecté : il ne voit que les siennes. */
@RestController
@RequestMapping("/api/v1/predictions/mine")
@RequiredArgsConstructor
@Tag(name = "Mes prédictions")
@SecurityRequirement(name = "bearerAuth")
class MyPredictionsController {

    private final PredictionHistory history;

    @GetMapping
    @Operation(operationId = "listMyPredictions", summary = "Mes prédictions, les plus récentes d'abord",
            description = "Chaque match prédit depuis la page « Prédire », avec le score réel et le verdict une fois le match joué.")
    PagedModel<PredictionHistoryEntry> listMyPredictions(@AuthenticationPrincipal Jwt jwt,
            @ParameterObject @PageableDefault(size = 20, sort = "predictedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(history.mine(Long.parseLong(jwt.getSubject()), pageable));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deleteMyPrediction", summary = "Retirer une de mes prédictions",
            description = "404 si elle n'existe pas ou appartient à un autre utilisateur.")
    void deleteMyPrediction(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        history.delete(Long.parseLong(jwt.getSubject()), id);
    }
}
