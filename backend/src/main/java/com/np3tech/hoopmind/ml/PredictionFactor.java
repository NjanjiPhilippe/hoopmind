package com.np3tech.hoopmind.ml;

/**
 * Une variable d'entrée du modèle pour un match donné, et ce qu'elle a pesé dans la prédiction.
 *
 * @param feature      nom de la variable (voir {@code GameOutcomeFeatures.NAMES})
 * @param homeValue    valeur côté équipe à domicile (pour {@code is_playoff} : 1 ou 0, identique des deux côtés)
 * @param awayValue    valeur côté équipe à l'extérieur
 * @param contribution part de la variable dans la marge de la régression logistique (log-cote) : positive, elle
 *                     pousse vers une victoire à domicile ; négative, vers une victoire à l'extérieur
 */
public record PredictionFactor(String feature, double homeValue, double awayValue, double contribution) {
}
