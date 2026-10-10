package com.np3tech.hoopmind.user;

/**
 * Champs modifiables par l'utilisateur lui-même. Un champ {@code null} n'est pas modifié.
 */
public record ProfileUpdate(String firstName, String lastName, String bio, Long favoriteTeamId) {
}
