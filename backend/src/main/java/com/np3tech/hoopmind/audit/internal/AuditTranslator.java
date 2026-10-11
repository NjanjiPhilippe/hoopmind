package com.np3tech.hoopmind.audit.internal;

import com.np3tech.hoopmind.audit.AuditAction;
import com.np3tech.hoopmind.auth.AuthenticationEvent;
import com.np3tech.hoopmind.auth.CurrentUser;
import com.np3tech.hoopmind.user.UserAccountEvent;
import com.np3tech.hoopmind.user.domain.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Écoute les événements publiés par les autres modules et les traduit en entrées du journal.
 * <p>
 * Une réussite n'est enregistrée que si la transaction de l'appelant est validée ; un échec l'est toujours,
 * y compris quand l'exception qui le signale annule la transaction. Les écouteurs s'exécutent dans le fil
 * de la requête : l'utilisateur connecté y est encore connu.
 */
@Component
@RequiredArgsConstructor
class AuditTranslator {

    private final AuditRecorder recorder;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true,
            condition = "!#event.failed()")
    void onCommitted(UserAccountEvent event) {
        recorder.record(translate(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION, fallbackExecution = true,
            condition = "#event.failed()")
    void onFailed(UserAccountEvent event) {
        recorder.record(translate(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true,
            condition = "!#event.failed()")
    void onCommitted(AuthenticationEvent event) {
        recorder.record(translate(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION, fallbackExecution = true,
            condition = "#event.failed()")
    void onFailed(AuthenticationEvent event) {
        recorder.record(translate(event));
    }

    static AuditEvent translate(UserAccountEvent event) {
        AuditTarget target = AuditTarget.user(event.userId(), event.username());
        AuditActor subject = AuditActor.user(event.userId(), event.username());
        // L'administrateur qui agit, ou à défaut le compte lui-même (inscription, appel hors requête HTTP).
        AuditActor actor = currentUser().orElse(subject);
        return switch (event) {
            case UserAccountEvent.Registered registered -> AuditEvent.success(AuditAction.USER_REGISTERED, subject, target);
            case UserAccountEvent.ProfileUpdated updated ->
                    AuditEvent.success(AuditAction.PROFILE_UPDATED, actor, target)
                            .withDetail("fields", String.join(",", updated.fields()));
            case UserAccountEvent.PasswordChanged passwordChanged -> AuditEvent.success(AuditAction.PASSWORD_CHANGED, actor, target);
            case UserAccountEvent.PasswordChangeRejected passwordChangeRejected ->
                    AuditEvent.failure(AuditAction.PASSWORD_CHANGE_FAILED, actor, target, "WRONG_CURRENT_PASSWORD");
            case UserAccountEvent.EnabledChanged changed -> AuditEvent.success(
                    changed.enabled() ? AuditAction.USER_ENABLED : AuditAction.USER_DISABLED, actor, target);
            case UserAccountEvent.RolesChanged changed ->
                    AuditEvent.success(AuditAction.USER_ROLES_CHANGED, actor, target)
                            .withDetail("before", roles(changed.before()))
                            .withDetail("after", roles(changed.after()));
            case UserAccountEvent.Deleted deleted -> AuditEvent.success(AuditAction.USER_DELETED, actor, target);
            case UserAccountEvent.AdminBootstrapped adminBootstrapped ->
                    AuditEvent.success(AuditAction.ADMIN_BOOTSTRAPPED, AuditActor.SYSTEM, target);
            case UserAccountEvent.LastAdminProtected refused -> AuditEvent.failure(switch (refused.attempt()) {
                case DISABLE -> AuditAction.USER_DISABLED;
                case CHANGE_ROLES -> AuditAction.USER_ROLES_CHANGED;
                case DELETE -> AuditAction.USER_DELETED;
            }, actor, target, "LAST_ADMIN");
        };
    }

    static AuditEvent translate(AuthenticationEvent event) {
        // Avant (ou au moment de) la session, l'auteur est le compte concerné.
        AuditActor actor = event.userId() == null ? null : AuditActor.user(event.userId(), event.username());
        AuditTarget target = event.userId() == null ? null : AuditTarget.user(event.userId(), event.username());
        return switch (event) {
            case AuthenticationEvent.LoginSucceeded loginSucceeded -> AuditEvent.success(AuditAction.LOGIN_SUCCEEDED, actor, target);
            case AuthenticationEvent.LoginFailed failed ->
                    AuditEvent.failure(AuditAction.LOGIN_FAILED, actor, target, failed.reason().name());
            case AuthenticationEvent.LoggedOut loggedOut -> AuditEvent.success(AuditAction.LOGOUT, actor, target);
            case AuthenticationEvent.RefreshTokenReused reused ->
                    AuditEvent.failure(AuditAction.REFRESH_TOKEN_REUSED, actor, target, "TOKEN_ALREADY_USED")
                            .withDetail("revokedTokens", String.valueOf(reused.revokedTokens()));
        };
    }

    private static Optional<AuditActor> currentUser() {
        return CurrentUser.get().map(user -> AuditActor.user(user.id(), user.username()));
    }

    private static String roles(Collection<RoleName> names) {
        return names.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }
}
