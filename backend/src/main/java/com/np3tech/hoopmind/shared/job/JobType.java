package com.np3tech.hoopmind.shared.job;

import com.np3tech.hoopmind.shared.lock.DataOperationLock;

/** Les opérations longues lancées en arrière-plan, et le verrou des données que chacune prend. */
public enum JobType {
    NBA_DATABASE_IMPORT(DataOperationLock.Operation.NBA_DATABASE_IMPORT),
    /** Saisons récentes lues dans les fichiers d'Eoin Moore : mêmes tables, même verrou. */
    EOIN_MOORE_IMPORT(DataOperationLock.Operation.NBA_DATABASE_IMPORT),
    MODEL_TRAINING(DataOperationLock.Operation.MODEL_TRAINING);

    private final DataOperationLock.Operation lock;

    JobType(DataOperationLock.Operation lock) {
        this.lock = lock;
    }

    DataOperationLock.Operation lock() {
        return lock;
    }
}
