package com.np3tech.hoopmind.shared.job;

import com.np3tech.hoopmind.shared.lock.DataOperationLock;

/** Les opérations longues lancées en arrière-plan, et le verrou des données que chacune prend. */
public enum JobType {
    NBA_DATABASE_IMPORT(DataOperationLock.Operation.NBA_DATABASE_IMPORT),
    /** Complément depuis stats.nba.com : il écrit les mêmes tables que l'import de la base NBA, sous le même verrou. */
    STATS_NBA_IMPORT(DataOperationLock.Operation.NBA_DATABASE_IMPORT),
    MODEL_TRAINING(DataOperationLock.Operation.MODEL_TRAINING);

    private final DataOperationLock.Operation lock;

    JobType(DataOperationLock.Operation lock) {
        this.lock = lock;
    }

    DataOperationLock.Operation lock() {
        return lock;
    }
}
