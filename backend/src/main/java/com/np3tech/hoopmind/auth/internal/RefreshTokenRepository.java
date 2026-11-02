package com.np3tech.hoopmind.auth.internal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshToken t set t.revoked = true, t.revokedAt = :now, t.revokedReason = :reason
            where t.familyId = :familyId and t.revoked = false""")
    int revokeFamily(@Param("familyId") String familyId, @Param("now") Instant now,
                     @Param("reason") RefreshToken.RevocationReason reason);

    @Modifying
    @Query("""
            update RefreshToken t set t.revoked = true, t.revokedAt = :now, t.revokedReason = :reason
            where t.userId = :userId and t.revoked = false""")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now,
                         @Param("reason") RefreshToken.RevocationReason reason);

    @Modifying
    @Query("delete from RefreshToken t where t.userId = :userId")
    int deleteAllForUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
