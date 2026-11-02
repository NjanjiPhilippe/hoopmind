package com.np3tech.hoopmind.auth.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

/**
 * Jeton de rafraîchissement. Seul son empreinte SHA-256 est stockée. Tous les jetons issus d'une même
 * connexion partagent une famille : la réutilisation d'un jeton déjà échangé révoque toute la famille.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String tokenHash;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** {@code null} pour les jetons révoqués avant l'ajout du motif : traités comme échangés. */
    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_reason", length = 30)
    private RevocationReason revokedReason;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    RefreshToken(Long userId, String tokenHash, String familyId, Instant expiresAt, String ipAddress, String userAgent) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = Instant.now();
    }

    boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    void revoke(Instant now, RevocationReason reason) {
        if (!revoked) {
            revoked = true;
            revokedAt = now;
            revokedReason = reason;
        }
    }

    /** Révoqué parce qu'il a été échangé : le présenter de nouveau est suspect. */
    boolean wasRotated() {
        return revoked && (revokedReason == null || revokedReason == RevocationReason.ROTATED);
    }

    /** Échangé il y a moins de {@code grace} : un autre onglet du même navigateur a pu le présenter en même temps. */
    boolean rotatedWithin(Instant now, Duration grace) {
        return wasRotated() && revokedAt != null && revokedAt.plus(grace).isAfter(now);
    }

    enum RevocationReason {
        /** Échangé contre un nouveau jeton. */
        ROTATED,
        /** Déconnexion. */
        LOGOUT,
        /** Mot de passe, rôles ou statut du compte changés. */
        ACCESS_CHANGED,
        /** Famille révoquée après la réutilisation d'un jeton échangé. */
        REUSE_DETECTED
    }
}
