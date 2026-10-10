package com.np3tech.hoopmind.user.web;

import com.np3tech.hoopmind.user.ProfileUpdate;
import com.np3tech.hoopmind.user.UserAccountService;
import com.np3tech.hoopmind.user.UserProfile;
import com.np3tech.hoopmind.user.web.UserDtos.ChangePasswordRequest;
import com.np3tech.hoopmind.user.web.UserDtos.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "Mon compte", description = "Profil de l'utilisateur connecté")
@SecurityRequirement(name = "bearerAuth")
class MeController {

    private final UserAccountService accounts;

    @GetMapping
    @Operation(operationId = "getMyProfile", summary = "Lire mon profil")
    UserProfile me(@AuthenticationPrincipal Jwt jwt) {
        return accounts.getProfile(userId(jwt));
    }

    @PatchMapping
    @Operation(operationId = "updateMyProfile", summary = "Modifier mon profil")
    UserProfile update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return accounts.updateProfile(userId(jwt), new ProfileUpdate(request.firstName(), request.lastName(),
                request.bio(), request.favoriteTeamId()));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "changeMyPassword", summary = "Changer mon mot de passe", description = "Déconnecte toutes les autres sessions.")
    void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        accounts.changePassword(userId(jwt), request.currentPassword(), request.newPassword());
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
