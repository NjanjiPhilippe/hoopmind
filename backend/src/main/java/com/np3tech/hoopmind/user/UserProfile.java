package com.np3tech.hoopmind.user;

import com.np3tech.hoopmind.user.domain.RoleName;
import io.swagger.v3.oas.annotations.media.Schema;
import com.np3tech.hoopmind.user.domain.User;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;

/**
 * Profil d'un utilisateur tel qu'il est exposé par l'API.
 */
public record UserProfile(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String username,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
        String firstName,
        String lastName,
        String bio,
        Long favoriteTeamId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Set<RoleName> roles,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        Instant lastLogin,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt
) {

    static UserProfile of(User user) {
        Set<RoleName> roles = new TreeSet<>();
        user.getRoles().forEach(role -> roles.add(role.getName()));
        return new UserProfile(user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getBio(), user.getFavoriteTeamId(), roles, user.isEnabled(),
                user.getLastLogin(), user.getCreatedAt());
    }
}
