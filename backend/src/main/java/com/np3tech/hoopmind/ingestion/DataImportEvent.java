package com.np3tech.hoopmind.ingestion;

import com.np3tech.hoopmind.auth.CurrentUser;

import java.util.List;

/**
 * Fin d'un import de données, publiée avec {@code ApplicationEventPublisher}.
 * <p>
 * Un import peut se terminer dans un fil d'arrière-plan, sans utilisateur connecté : l'administrateur qui l'a
 * lancé est donc capturé au démarrage et transporté dans l'événement ({@code null} pour l'import au démarrage).
 */
public sealed interface DataImportEvent {

    CurrentUser requestedBy();

    default boolean failed() {
        return false;
    }

    record NbaDatabaseImported(CurrentUser requestedBy, boolean downloaded, List<String> seasons, int games)
            implements DataImportEvent {
        public NbaDatabaseImported {
            seasons = List.copyOf(seasons);
        }
    }

    /** {@code error} : message déjà présentable à un administrateur (ni chemin, ni trace technique). */
    record NbaDatabaseImportFailed(CurrentUser requestedBy, boolean download, int seasons, String error)
            implements DataImportEvent {
        @Override
        public boolean failed() {
            return true;
        }
    }

    record StatsNbaImported(CurrentUser requestedBy, List<String> seasons, int games) implements DataImportEvent {
        public StatsNbaImported {
            seasons = List.copyOf(seasons);
        }
    }

    /** {@code error} : message déjà présentable à un administrateur (ni chemin, ni trace technique). */
    record StatsNbaImportFailed(CurrentUser requestedBy, String fromSeason, String toSeason, String error)
            implements DataImportEvent {
        @Override
        public boolean failed() {
            return true;
        }
    }

    record ReferenceCsvImported(CurrentUser requestedBy, int files, int importedFiles, int failedFiles,
                                int insertedRows, int updatedRows) implements DataImportEvent {
    }
}
