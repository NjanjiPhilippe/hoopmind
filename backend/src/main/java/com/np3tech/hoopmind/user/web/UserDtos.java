package com.np3tech.hoopmind.user.web;

import com.np3tech.hoopmind.user.domain.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Corps de requête de l'API utilisateurs.
 */
final class UserDtos {

    private UserDtos() {
    }

    record UpdateProfileRequest(
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Size(max = 1000) String bio,
            Long favoriteTeamId) {
    }

    record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword) {
    }

    record UpdateStatusRequest(@NotNull Boolean enabled) {
    }

    record UpdateRolesRequest(@NotNull Set<RoleName> roles) {
    }
}
