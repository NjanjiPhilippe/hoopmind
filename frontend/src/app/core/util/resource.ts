import { Resource } from '@angular/core';

/**
 * Valeur d'une ressource, ou {@code undefined} si elle n'en a pas. Depuis Angular 20, {@code value()} lève une
 * exception quand la ressource est en erreur (requête refusée, annulée par une navigation, serveur injoignable) :
 * lue dans un computed ou un gabarit, elle casserait tout l'affichage au lieu de montrer l'état d'erreur.
 */
export function currentValue<T>(resource: Resource<T>): T | undefined {
  return resource.hasValue() ? resource.value() : undefined;
}
