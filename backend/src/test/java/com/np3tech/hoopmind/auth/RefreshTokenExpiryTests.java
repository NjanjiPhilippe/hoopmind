package com.np3tech.hoopmind.auth;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import com.np3tech.hoopmind.TestAccounts.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/** Un jeton de rafraîchissement expiré mais pas encore purgé (purge la nuit) n'ouvre plus de session. */
@IntegrationTest
class RefreshTokenExpiryTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void anExpiredRefreshTokenIsRefusedBeforeThePurge() {
        Session session = TestAccounts.register(mvc);
        // Un jour plutôt qu'une minute : now() suit le fuseau de la session SQL, Hibernate écrit l'heure en UTC.
        int expired = jdbc.update("UPDATE refresh_tokens SET expiry_date = now() - INTERVAL '1 day' WHERE token = ?",
                sha256(session.refreshCookie().getValue()));
        assertThat(expired).isEqualTo(1);

        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(session.refreshCookie())).hasStatus(401);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
