import { HttpErrorResponse } from '@angular/common/http';

/**
 * Délai d'attente imposé par un 429, en secondes : champ {@code retryAfterSeconds} du problème renvoyé par l'API,
 * sinon l'en-tête {@code Retry-After}. {@code null} si l'erreur n'est pas un 429 ou ne donne pas de délai.
 */
export function retryAfterSeconds(error: unknown): number | null {
  if (!(error instanceof HttpErrorResponse) || error.status !== 429) {
    return null;
  }
  const fromBody = (error.error as { retryAfterSeconds?: unknown } | null)?.retryAfterSeconds;
  const seconds = typeof fromBody === 'number' ? fromBody : Number.parseInt(error.headers?.get('Retry-After') ?? '', 10);
  return Number.isFinite(seconds) && seconds > 0 ? Math.ceil(seconds) : null;
}
