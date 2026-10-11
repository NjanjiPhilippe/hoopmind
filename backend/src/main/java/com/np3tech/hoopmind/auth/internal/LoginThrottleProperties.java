package com.np3tech.hoopmind.auth.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Limitation des tentatives de connexion ({@code hoopmind.security.login-throttle.*}).
 *
 * @param maxFailuresPerLogin   échecs tolérés pour un même identifiant depuis une même adresse
 * @param maxFailuresPerAddress échecs tolérés depuis une même adresse, tous identifiants confondus
 *                              (essais d'un mot de passe sur beaucoup de comptes)
 * @param initialBlock          premier blocage une fois le seuil atteint, doublé à chaque nouvel échec
 * @param maxBlock              blocage maximal
 * @param resetAfter            sans nouvel échec pendant cette durée, le compteur repart de zéro
 */
@ConfigurationProperties("hoopmind.security.login-throttle")
record LoginThrottleProperties(
        @DefaultValue("5") int maxFailuresPerLogin,
        @DefaultValue("50") int maxFailuresPerAddress,
        @DefaultValue("30s") Duration initialBlock,
        @DefaultValue("15m") Duration maxBlock,
        @DefaultValue("15m") Duration resetAfter
) {
}
