package com.np3tech.hoopmind.shared.lock;

import com.np3tech.hoopmind.shared.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verrou unique des opérations qui réécrivent ou lisent en masse les données NBA : import de la base Walsh,
 * import de CSV, entraînement du modèle. Deux de ces opérations en parallèle se marcheraient dessus (verrous
 * de lignes, modèle entraîné sur une base à moitié importée), on refuse donc la seconde avec un 409.
 * <p>
 * Le verrou n'est pas lié à un fil d'exécution : un job peut le prendre dans la requête HTTP et le rendre
 * dans son fil d'arrière-plan. Il est local à l'instance (une seule instance de l'API aujourd'hui).
 */
@Component
public class DataOperationLock {

    public enum Operation {
        NBA_DATABASE_IMPORT("un import de la base NBA"),
        CSV_IMPORT("un import de CSV"),
        MODEL_TRAINING("un entraînement du modèle");

        private final String label;

        Operation(String label) {
            this.label = label;
        }
    }

    private final AtomicReference<Permit> running = new AtomicReference<>();

    /**
     * Prend le verrou ou lève une {@link AppException} 409 {@code DATA_OPERATION_RUNNING}.
     * À rendre avec {@link Permit#close()}, idéalement dans un try-with-resources.
     */
    public Permit acquire(Operation operation) {
        Permit permit = new Permit(operation);
        if (!running.compareAndSet(null, permit)) {
            Permit other = running.get();
            String label = other == null ? "une autre opération" : other.operation.label;
            throw new AppException("Opération impossible : " + label + " est en cours.", HttpStatus.CONFLICT,
                    "DATA_OPERATION_RUNNING");
        }
        return permit;
    }

    public Optional<Operation> current() {
        return Optional.ofNullable(running.get()).map(permit -> permit.operation);
    }

    public final class Permit implements AutoCloseable {

        private final Operation operation;

        private Permit(Operation operation) {
            this.operation = operation;
        }

        /** Rend le verrou ; sans effet s'il a déjà été rendu (et ne libère jamais celui d'une autre opération). */
        @Override
        public void close() {
            running.compareAndSet(this, null);
        }
    }
}
