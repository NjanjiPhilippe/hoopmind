package com.np3tech.hoopmind.auth.web;

import com.np3tech.hoopmind.auth.config.JwtProperties;
import com.np3tech.hoopmind.auth.internal.AuthService;
import com.np3tech.hoopmind.auth.internal.AuthService.ClientInfo;
import com.np3tech.hoopmind.auth.internal.AuthService.Session;
import com.np3tech.hoopmind.user.Registration;
import com.np3tech.hoopmind.user.UserAccountService;
import com.np3tech.hoopmind.user.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Le jeton d'accès est renvoyé dans le corps et gardé en mémoire par le client. Le jeton de rafraîchissement
 * voyage uniquement dans un cookie HttpOnly, inaccessible au JavaScript.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification")
class AuthController {

    private final AuthService authService;
    private final UserAccountService accounts;
    private final JwtProperties properties;

    @PostMapping("/register")
    @Operation(operationId = "register", summary = "Créer un compte et ouvrir une session")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        Session session = authService.register(new Registration(request.username(), request.email(),
                request.password(), request.firstName(), request.lastName()), client(http));
        return respond(HttpStatus.CREATED, session);
    }

    @PostMapping("/login")
    @Operation(operationId = "login", summary = "Se connecter avec son nom d'utilisateur ou son e-mail")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return respond(HttpStatus.OK, authService.login(request.login(), request.password(), client(http)));
    }

    @PostMapping("/refresh")
    @Operation(operationId = "refreshSession", summary = "Renouveler le jeton d'accès", description = "Lit le cookie de rafraîchissement et le remplace.")
    ResponseEntity<AuthResponse> refresh(@CookieValue(name = "${hoopmind.security.jwt.refresh-cookie.name:hoopmind_refresh}",
            required = false) String refreshToken, HttpServletRequest http) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.SET_COOKIE, clearCookie()).build();
        }
        return respond(HttpStatus.OK, authService.refresh(refreshToken, client(http)));
    }

    @PostMapping("/logout")
    @Operation(operationId = "logout", summary = "Se déconnecter", description = "Révoque la session courante et efface le cookie.")
    ResponseEntity<Void> logout(@CookieValue(name = "${hoopmind.security.jwt.refresh-cookie.name:hoopmind_refresh}",
            required = false) String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie()).build();
    }

    private ResponseEntity<AuthResponse> respond(HttpStatus status, Session session) {
        UserProfile user = accounts.getProfile(session.userId());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(),
                        Duration.ofSeconds(session.refreshTokenExpiresIn())))
                .body(new AuthResponse(session.accessToken(), "Bearer", session.accessTokenExpiresIn(), user));
    }

    private String refreshCookie(String value, Duration maxAge) {
        JwtProperties.RefreshCookie cookie = properties.refreshCookie();
        return ResponseCookie.from(cookie.name(), value)
                .httpOnly(true)
                .secure(cookie.secure())
                .sameSite(cookie.sameSite())
                .path(cookie.path())
                .maxAge(maxAge)
                .build()
                .toString();
    }

    private String clearCookie() {
        return refreshCookie("", Duration.ZERO);
    }

    private static ClientInfo client(HttpServletRequest http) {
        return new ClientInfo(http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT));
    }

    record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                    message = "lettres, chiffres, point, tiret et tiret bas uniquement") String username,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName) {
    }

    record LoginRequest(@NotBlank String login, @NotBlank String password) {
    }

    @Schema(requiredProperties = {"accessToken", "tokenType", "expiresIn", "user"})
    record AuthResponse(String accessToken, String tokenType, long expiresIn, UserProfile user) {
    }
}
