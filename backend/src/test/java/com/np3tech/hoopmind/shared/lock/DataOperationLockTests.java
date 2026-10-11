package com.np3tech.hoopmind.shared.lock;

import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.shared.lock.DataOperationLock.Operation;
import com.np3tech.hoopmind.shared.lock.DataOperationLock.Permit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataOperationLockTests {

    private final DataOperationLock lock = new DataOperationLock();

    @Test
    void refusesASecondOperationWithAConflictNamingTheRunningOne() {
        try (Permit ignored = lock.acquire(Operation.NBA_DATABASE_IMPORT)) {
            assertThatThrownBy(() -> lock.acquire(Operation.MODEL_TRAINING))
                    .isInstanceOfSatisfying(AppException.class, e -> {
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(e.getErrorCode()).isEqualTo("DATA_OPERATION_RUNNING");
                    })
                    .hasMessageContaining("import de la base NBA");
        }
        assertThat(lock.current()).isEmpty();
    }

    @Test
    void aPermitClosedTwiceNeverReleasesTheNextOperation() {
        Permit first = lock.acquire(Operation.CSV_IMPORT);
        first.close();
        Permit second = lock.acquire(Operation.CSV_IMPORT);
        first.close();

        assertThat(lock.current()).contains(Operation.CSV_IMPORT);
        second.close();
        assertThat(lock.current()).isEmpty();
    }
}
