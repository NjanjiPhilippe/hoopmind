# Décisions du frontend (ADR)

Même format que les [ADR du backend](../../../backend/docs/adr/README.md) : le problème constaté, ce qui a été
choisi, les alternatives écartées, les avantages et les inconvénients assumés. Les numéros suivent l'ordre du
chantier frontend qui a suivi la revue des PR #1 à #3.

| N° | Décision | Statut |
|----|----------|--------|
| [0001](0001-entrainement-suivi-en-job.md) | La page Données suit l'entraînement comme un job ; l'endpoint synchrone disparaît | Acceptée |
| [0002](0002-decompte-apres-429.md) | Après trop de tentatives de connexion, un décompte bloque le bouton | Acceptée |
| [0003](0003-lectures-nba-du-backend.md) | Les lectures NBA du backend chargent leurs relations et renvoient les scores | Acceptée |
| [0004](0004-quatre-pages-nba.md) | Les pages NBA héritées sont remplacées par quatre pages branchées sur l'API | Acceptée |
| [0005](0005-tableau-de-bord-admin.md) | Le tableau de bord admin n'affiche que des chiffres réels | Acceptée |
| [0006](0006-pages-403-404-et-menu-mobile.md) | Pages 403 et 404, menu mobile accessible | Acceptée |
| [0007](0007-ressources-sans-exception.md) | Les gabarits lisent les ressources avec `currentValue()` | Acceptée |
| [0008](0008-filtres-d-audit-dans-l-url.md) | Les filtres du journal d'audit vivent dans l'URL | Acceptée |
| [0009](0009-zoneless.md) | L'application tourne sans zone.js | Acceptée |
| [0010](0010-calendrier-logos-couleurs.md) | Calendrier par équipe, logos et couleurs des franchises | Acceptée |
| [0011](0011-identite-orange.md) | Identité visuelle orange : logo, connexion en deux panneaux | Acceptée |
