import { AuthResponse } from '../../api/generated/models/auth-response';

/** Réponse d'authentification factice pour les tests. */
export function authResponse(roles: AuthResponse['user']['roles'] = ['ROLE_USER'], token = 'access-token'): AuthResponse {
  return {
    accessToken: token,
    tokenType: 'Bearer',
    expiresIn: 900,
    user: { id: 1, username: 'saiko', email: 'saiko@example.com', roles, enabled: true, createdAt: '2026-10-10T00:00:00Z' },
  };
}
