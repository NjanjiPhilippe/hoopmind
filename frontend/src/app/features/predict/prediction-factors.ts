import { ExplainedPrediction } from '../../core/api/generated/models/explained-prediction';
import { PredictionFactor } from '../../core/api/generated/models/prediction-factor';

/** Un facteur du modèle prêt à afficher : libellé, valeur de chaque équipe, poids relatif et sens. */
export interface FactorRow {
  feature: string;
  label: string;
  home: string;
  away: string;
  /** Part du facteur le plus influent, de 0 à 1 (largeur de la barre). */
  weight: number;
  favorsHome: boolean;
  /**
   * Le facteur favorise l'équipe dont les valeurs sont moins bonnes : le modèle compense une variable par une autre
   * qui mesure presque la même chose (bilan et net rating). Affiché comme tel, jamais comme un avantage.
   */
  correction: boolean;
}

const LABELS: Record<string, string> = {
  win_pct_diff: 'Victoires cette saison',
  last10_win_pct_diff: '10 derniers matchs',
  net_rating_diff: 'Net rating (pour 100 possessions)',
  rest_days_diff: 'Jours de repos',
  is_playoff: 'Match de playoffs',
};

const percent = new Intl.NumberFormat('fr-FR', { style: 'percent', maximumFractionDigits: 0 });
const signed = new Intl.NumberFormat('fr-FR', { signDisplay: 'always', minimumFractionDigits: 1, maximumFractionDigits: 1 });

export function factorLabel(feature: string): string {
  return LABELS[feature] ?? feature;
}

export function factorValue(feature: string, value: number): string {
  switch (feature) {
    case 'win_pct_diff':
    case 'last10_win_pct_diff':
      // Espace insécable classique : l'espace fine de Intl varie selon les navigateurs.
      return percent.format(value).replace(/\s/g, '\u00a0');
    case 'net_rating_diff':
      return signed.format(value);
    case 'rest_days_diff': {
      const days = Math.round(value);
      return `${days} ${days > 1 ? 'jours' : 'jour'}`;
    }
    case 'is_playoff':
      return value > 0 ? 'oui' : 'non';
    default:
      return String(value);
  }
}

/**
 * Facteurs dans l'ordre du serveur (du plus au moins influent). Le drapeau playoffs n'est montré que pour un match
 * de playoffs : en saison régulière, il ne dit rien d'utile au lecteur.
 */
export function factorRows(factors: PredictionFactor[] | undefined, playoff: boolean): FactorRow[] {
  const shown = (factors ?? []).filter(factor => factor.feature !== 'is_playoff' || playoff);
  const strongest = Math.max(...shown.map(factor => Math.abs(factor.contribution ?? 0)), 0);
  return shown.map(factor => {
    const feature = factor.feature ?? '';
    const contribution = factor.contribution ?? 0;
    const difference = (factor.homeValue ?? 0) - (factor.awayValue ?? 0);
    return {
      feature,
      label: factorLabel(feature),
      home: factorValue(feature, factor.homeValue ?? 0),
      away: factorValue(feature, factor.awayValue ?? 0),
      weight: strongest === 0 ? 0 : Math.abs(contribution) / strongest,
      favorsHome: contribution >= 0,
      correction: feature !== 'is_playoff' && difference !== 0 && contribution !== 0
        && Math.sign(difference) !== Math.sign(contribution),
    };
  });
}

/** D'où vient le texte : un LLM nommé, ou la rédaction automatique (sans LLM configuré, ou en repli). */
export function explanationSource(result: ExplainedPrediction): string {
  const explanation = result.explanation;
  if (result.fallback) {
    return 'Le service d\'IA n\'a pas répondu : explication rédigée automatiquement à partir des chiffres du modèle.';
  }
  if (!explanation || explanation.provider === 'template') {
    return 'Explication rédigée automatiquement à partir des chiffres du modèle.';
  }
  return `Rédigé par ${explanation.model ?? 'un LLM'} à partir des seuls chiffres du modèle.`;
}
