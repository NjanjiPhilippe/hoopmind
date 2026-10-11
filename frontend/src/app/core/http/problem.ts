import { HttpErrorResponse } from '@angular/common/http';

/** Erreur renvoyée par l'API au format RFC 9457 (application/problem+json). */
export interface Problem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  /** Code applicatif stable (par exemple MODEL_NOT_TRAINED), à préférer au texte pour décider d'un affichage. */
  code?: string;
  /** Erreurs de validation par champ, quand l'API en renvoie. */
  errors?: Record<string, string>;
}

/** Extrait le problème d'une erreur HTTP, avec un message lisible par défaut. */
export function toProblem(error: unknown): Problem {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return { status: 0, detail: 'Le serveur est injoignable. Réessayez dans un instant.' };
    }
    const body = error.error;
    if (body && typeof body === 'object') {
      return { status: error.status, ...(body as Problem) };
    }
    return { status: error.status, detail: 'Une erreur est survenue.' };
  }
  return { detail: 'Une erreur est survenue.' };
}

/** Le type de problème sans son préfixe d'URL (par exemple « username-taken »). */
export function problemCode(problem: Problem): string | undefined {
  return problem.type?.split('/').pop();
}
