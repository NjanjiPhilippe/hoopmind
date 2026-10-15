# 0011 · Identité visuelle orange : logo, connexion en deux panneaux

## Problème

L'interface reprenait le bleu par défaut de Tailwind et un émoji 🏀 sur un carré bleu en guise de logo : rien ne la
distinguait d'un gabarit, et le bleu rappelait la NBA plus que HoopMind. La page de connexion, une carte sur un
dégradé bleu, ne disait rien du projet.

## Décision

- **Palette** : la gamme `primary` (`styles.css`) passe à un orange clair « ballon de basket ». Le 500 (`#f7873a`)
  sert au logo et aux icônes ; le 600 (`#ef6c1a`) aux boutons, en texte blanc gras (3:1, seuil WCAG des composants
  et des grands textes) ; le 700 (`#c2510e`) aux liens et au texte orange sur fond blanc (4,7:1, AA). Tout le code
  utilisait déjà `primary-*`. Les couleurs des franchises (`team-branding.ts`) et les badges d'état ne bougent pas.
- **Logo** (`app-logo`, `public/favicon.svg`, `favicon.ico`) : les seules coutures d'un ballon, en traits épais et
  irréguliers, couleur héritée, sans fond.
- **Noir plutôt que bleu** pour les grandes surfaces : bandeau de l'accueil, panneau gauche de la connexion, pied
  de page (`neutral-950`, sans la teinte bleutée de `gray`).
- **Connexion et inscription en deux panneaux** (`AuthLayoutComponent`) : à gauche, sur fond noir, le logo, une
  accroche et une photo de Kobe Bryant au dunk, à 70 % d'opacité ; à droite, la carte. Sous `lg`, il ne
  reste que la carte, sur fond noir.
- **Photo par lien** : l'image est chargée depuis Wikimedia Commons (`Special:FilePath`, qui suit les
  renommages), avec un crédit cliquable vers sa page. Rien n'est copié dans le dépôt. Si elle ne charge pas, le
  panneau reste noir.
- **Accueil** : le bandeau noir porte « The Last Shot » de Michael Jordan (finale 1998), chargée par lien de la
  même façon, à 70 % d'opacité, avec un crédit cliquable.
- **Retour à l'accueil** depuis la connexion et l'inscription : le logo en haut de la carte (et celui du panneau)
  mène à l'accueil.
- **Icônes au trait** (`app-icon`) à la place des émojis (accueil, tableau de bord admin, page Données) : même rendu
  sur tous les systèmes, couleur de la palette.
- **Accueil plus complet** : bandeau plus haut (78 % de l'écran) pour la photo, puis « Comment ça marche » en quatre
  étapes, les trois pages principales et une section « Pourquoi pas 100 % ? » sur les limites d'une prédiction.
- **Mot de passe** : l'œil (icône au trait) remplace le texte « Afficher » ; le bouton garde un libellé accessible.

## Alternatives écartées

- **Une photo copiée dans le dépôt** : le dépôt est public ; un lien vers Commons évite d'y redistribuer l'image
  et garde la source et la licence à un clic.
- **Une illustration dessinée** (première version de cette PR) : jugée moins parlante qu'une vraie photo.
- **Orange brûlé (`#c94a0c`) pour les boutons** (première version) : plus contrasté, mais jugé trop sombre ; l'orange
  clair garde le texte des liens lisible en passant au 700.

## Conséquences

- **+** Une identité reconnaissable, cohérente du favicon au pied de page.
- **+** Changer de teinte plus tard ne touche que `styles.css`.
- **−** L'accueil et la connexion dépendent de sites tiers pour leurs photos : hors ligne ou si le fichier disparaît, le panneau
  reste noir (la connexion fonctionne normalement).
- **−** Les photos montrent des personnes réelles et ne sont pas sous licence libre pour celle de Jordan : acceptable pour une démonstration non commerciale, à remplacer pour
  un usage commercial.
