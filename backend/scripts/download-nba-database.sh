#!/usr/bin/env sh
# Télécharge la base NBA de Wyatt Walsh (Kaggle wyattowalsh/basketball, licence CC BY-SA 4.0) dans backend/data/.
# Les données ne sont jamais versionnées : chacun les télécharge avec son propre compte Kaggle.
#
# Prérequis : un jeton d'API Kaggle (https://www.kaggle.com/settings > API > Create New Token),
# placé dans ~/.kaggle/kaggle.json ou exporté dans KAGGLE_USERNAME et KAGGLE_KEY.
#
# Puis importer en administrateur : POST /api/v1/admin/imports/nba-database?seasons=10
set -eu

DATA_DIR="$(cd "$(dirname "$0")/.." && pwd)/data"
DATASET="wyattowalsh/basketball"
FILE="nba.sqlite"

mkdir -p "$DATA_DIR"

if command -v kaggle >/dev/null 2>&1; then
  kaggle datasets download "$DATASET" --file "$FILE" --path "$DATA_DIR"
  # Les fichiers volumineux arrivent compressés
  if [ -f "$DATA_DIR/$FILE.zip" ]; then
    unzip -o "$DATA_DIR/$FILE.zip" -d "$DATA_DIR" && rm "$DATA_DIR/$FILE.zip"
  fi
elif [ -n "${KAGGLE_USERNAME:-}" ] && [ -n "${KAGGLE_KEY:-}" ]; then
  curl -fL -u "$KAGGLE_USERNAME:$KAGGLE_KEY" \
    "https://www.kaggle.com/api/v1/datasets/download/$DATASET/$FILE" -o "$DATA_DIR/$FILE.download"
  # Kaggle renvoie le fichier compressé en zip quand il est volumineux
  if unzip -tq "$DATA_DIR/$FILE.download" >/dev/null 2>&1; then
    unzip -o "$DATA_DIR/$FILE.download" -d "$DATA_DIR" && rm "$DATA_DIR/$FILE.download"
  else
    mv "$DATA_DIR/$FILE.download" "$DATA_DIR/$FILE"
  fi
else
  echo "Installez la CLI Kaggle (pip install kaggle) ou définissez KAGGLE_USERNAME et KAGGLE_KEY." >&2
  exit 1
fi

echo "Base NBA prête : $DATA_DIR/$FILE"
