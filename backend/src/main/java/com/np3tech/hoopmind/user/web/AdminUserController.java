package com.np3tech.hoopmind.user.web;

import com.np3tech.hoopmind.user.UserAccountService;
import com.np3tech.hoopmind.user.UserProfile;
import com.np3tech.hoopmind.user.web.UserDtos.UpdateRolesRequest;
import com.np3tech.hoopmind.user.web.UserDtos.UpdateStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration des utilisateurs")
@SecurityRequirement(name = "bearerAuth")
class AdminUserController {

    private final UserAccountService accounts;

    @GetMapping
    @Operation(operationId = "listUsers", summary = "Lister et rechercher les utilisateurs")
    PagedModel<UserProfile> list(@RequestParam(required = false) String search,
                                 @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(accounts.search(search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getUser", summary = "Lire un utilisateur")
    UserProfile get(@PathVariable Long id) {
        return accounts.getProfile(id);
    }

    @PutMapping("/{id}/status")
    @Operation(operationId = "setUserStatus", summary = "Activer ou désactiver un compte")
    UserProfile setStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return accounts.setEnabled(id, request.enabled());
    }

    @PutMapping("/{id}/roles")
    @Operation(operationId = "setUserRoles", summary = "Remplacer les rôles d'un utilisateur", description = "ROLE_USER est toujours conservé.")
    UserProfile setRoles(@PathVariable Long id, @Valid @RequestBody UpdateRolesRequest request) {
        return accounts.setRoles(id, request.roles());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deleteUser", summary = "Supprimer un utilisateur")
    void delete(@PathVariable Long id) {
        accounts.delete(id);
    }
}
