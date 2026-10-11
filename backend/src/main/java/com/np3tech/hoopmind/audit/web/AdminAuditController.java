package com.np3tech.hoopmind.audit.web;

import com.np3tech.hoopmind.audit.AuditAction;
import com.np3tech.hoopmind.audit.AuditOutcome;
import com.np3tech.hoopmind.audit.internal.AuditQueryService;
import com.np3tech.hoopmind.audit.internal.AuditQueryService.AuditSearch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/audit-events")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Journal d'audit", description = "Qui a fait quoi dans l'application")
@SecurityRequirement(name = "bearerAuth")
class AdminAuditController {

    private final AuditQueryService audit;

    @GetMapping
    @Operation(operationId = "listAuditEvents", summary = "Consulter le journal d'audit", description = "Du plus récent au plus ancien.")
    PagedModel<AuditEventView> list(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditOutcome outcome,
            @Parameter(description = "Utilisateur auteur ou cible de l'action") @RequestParam(required = false) Long userId,
            @Parameter(description = "Texte cherché dans le nom de l'auteur ou de la cible") @RequestParam(required = false) String user,
            @Parameter(description = "Borne incluse (ISO 8601)") @RequestParam(required = false) Instant from,
            @Parameter(description = "Borne exclue (ISO 8601)") @RequestParam(required = false) Instant to,
            @ParameterObject @PageableDefault(size = 50, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(audit.search(new AuditSearch(action, outcome, userId, user, from, to), pageable)
                .map(AuditEventView::of));
    }
}
