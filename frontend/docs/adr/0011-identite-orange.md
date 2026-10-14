# 0011 · Identité visuelle orange : logo, connexion en deux panneaux

## Problème

L'interface reprenait le bleu par défaut de Tailwind et un émoji 🏀 sur un carré bleu en guise de logo : rien ne la
distinguait d'un gabarit, et le bleu rappelait la NBA plus que HoopMind. La page de connexion, une carte sur un
dégradé bleu, ne disait rien du projet.

## Décision

- **Palette** : la gamme `primary` (`styles.css`) passe à un orange « ballon de basket ». Le 500 (`#ef6a1f`) sert au
  logo et aux accents ; le 600 (`#c94a0c`) aux boutons et aux liens, car le blanc y garde un contraste de 4,7:1
  (WCAG AA). Tout le code utilisait déjà `primary-*`, donc seules les valeurs changent. Les couleurs des franchises
  (`team-branding.ts`) et les badges d'état (vert, ambre, rouge, info) ne bougent pas.
- **Logo** (`app-logo`, `public/favicon.svg`, `favicon.ico`) : les seules coutures d'un ballon, en traits épais et
  irréguliers, couleur héritée, sans fond.
- **Noir plutôt que bleu** pour les grandes surfaces : bandeau de l'accueil, panneau gauche de la connexion, pied
  de page (`neutral-950`, sans la teinte bleutée de `gray`).
- **Connexion et inscription en deux panneaux** (`AuthLayoutComponent`) : à gauche, sur fond noir, le logo, une
  accroche et une photo de Kobe Bryant au dunk, très transparente (35 %) ; à droite, la carte. Sous `lg`, il ne
  reste que la carte, sur fond noir.
- **Photo par lien** : l'image est chargée depuis Wikimedia Commons (`Special:FilePath`, qui suit les
  renommages), avec un crédit cliquable vers sa page. Rien n'est copié dans le dépôt. Si elle ne charge pas, le
  panneau reste noir.

## Alternatives écartées

- **Une photo copiée dans le dépôt** : le dépôt est public ; un lien vers Commons évite d'y redistribuer l'image
  et garde la source et la licence à un clic.
- **Une illustration dessinée** (première version de cette PR) : jugée moins parlante qu'une vraie photo.
- **Orange vif (`#ef6a1f`) pour les boutons** : texte blanc à 3,1:1, illisible pour une partie des utilisateurs.

## Conséquences

- **+** Une identité reconnaissable, cohérente du favicon au pied de page.
- **+** Changer de teinte plus tard ne touche que `styles.css`.
- **−** La page de connexion dépend de Wikimedia pour sa photo : hors ligne ou si le fichier disparaît, le panneau
  reste noir (la connexion fonctionne normalement).
- **−** La photo montre une personne réelle : acceptable pour une démonstration non commerciale, à remplacer pour
  un usage commercial.
