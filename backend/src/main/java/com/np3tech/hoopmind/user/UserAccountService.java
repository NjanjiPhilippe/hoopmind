package com.np3tech.hoopmind.user;

import com.np3tech.hoopmind.user.UserAccountEvent.LastAdminProtected.Attempt;
import com.np3tech.hoopmind.user.domain.Role;
import com.np3tech.hoopmind.user.domain.RoleName;
import com.np3tech.hoopmind.user.domain.RoleRepository;
import com.np3tech.hoopmind.user.domain.User;
import com.np3tech.hoopmind.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Point d'entrée du module utilisateurs, utilisé par la couche web et par le module d'authentification.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAccountService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    // --- Authentification -------------------------------------------------------------------

    /**
     * Recherche un compte par nom d'utilisateur ou e-mail, sans tenir compte de la casse.
     */
    public Optional<UserAccount> findAccountByLogin(String login) {
        return users.findByLogin(login.trim()).map(UserAccountService::toAccount);
    }

    public Optional<UserAccount> findAccountById(Long id) {
        return users.findById(id).map(UserAccountService::toAccount);
    }

    @Transactional
    public void recordSuccessfulLogin(Long userId) {
        users.findById(userId).ifPresent(user -> user.setLastLogin(clock.instant()));
    }

    @Transactional
    public UserAccount register(Registration registration) {
        User user = create(registration.username(), registration.email(), registration.password(), Set.of(RoleName.ROLE_USER));
        user.setFirstName(blankToNull(registration.firstName()));
        user.setLastName(blankToNull(registration.lastName()));
        events.publishEvent(new UserAccountEvent.Registered(user.getId(), user.getUsername()));
        return toAccount(user);
    }

    // --- Profil de l'utilisateur connecté ---------------------------------------------------

    public UserProfile getProfile(Long userId) {
        return UserProfile.of(load(userId));
    }

    @Transactional
    public UserProfile updateProfile(Long userId, ProfileUpdate update) {
        User user = load(userId);
        // Seuls les noms des champs modifiés sont tracés, jamais leurs valeurs.
        List<String> changed = new ArrayList<>();
        if (update.firstName() != null && changed(user.getFirstName(), blankToNull(update.firstName()), "firstName", changed)) {
            user.setFirstName(blankToNull(update.firstName()));
        }
        if (update.lastName() != null && changed(user.getLastName(), blankToNull(update.lastName()), "lastName", changed)) {
            user.setLastName(blankToNull(update.lastName()));
        }
        if (update.bio() != null && changed(user.getBio(), blankToNull(update.bio()), "bio", changed)) {
            user.setBio(blankToNull(update.bio()));
        }
        if (update.favoriteTeamId() != null && changed(user.getFavoriteTeamId(), update.favoriteTeamId(), "favoriteTeamId", changed)) {
            user.setFavoriteTeamId(update.favoriteTeamId());
        }
        if (!changed.isEmpty()) {
            events.publishEvent(new UserAccountEvent.ProfileUpdated(user.getId(), user.getUsername(), changed));
        }
        return UserProfile.of(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = load(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            events.publishEvent(new UserAccountEvent.PasswordChangeRejected(user.getId(), user.getUsername()));
            throw UserException.wrongPassword();
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        events.publishEvent(new UserAccountEvent.PasswordChanged(user.getId(), user.getUsername()));
        events.publishEvent(new UserCredentialsRevoked(userId, UserCredentialsRevoked.Reason.PASSWORD_CHANGED));
    }

    // --- Administration ---------------------------------------------------------------------

    public Page<UserProfile> search(String search, Pageable pageable) {
        return users.search(StringUtils.hasText(search) ? search.trim() : "", pageable).map(UserProfile::of);
    }

    @Transactional
    public UserProfile setEnabled(Long userId, boolean enabled) {
        User user = load(userId);
        if (!enabled && user.isEnabled() && user.hasRole(RoleName.ROLE_ADMIN)) {
            ensureAnotherAdminRemains(Attempt.DISABLE, user);
        }
        boolean wasEnabled = user.isEnabled();
        user.setEnabled(enabled);
        if (wasEnabled != enabled) {
            events.publishEvent(new UserAccountEvent.EnabledChanged(user.getId(), user.getUsername(), enabled));
        }
        if (!enabled) {
            events.publishEvent(new UserCredentialsRevoked(userId, UserCredentialsRevoked.Reason.ACCOUNT_DISABLED));
        }
        return UserProfile.of(user);
    }

    @Transactional
    public UserProfile setRoles(Long userId, Collection<RoleName> roleNames) {
        User user = load(userId);
        Set<RoleName> wanted = roleNames.isEmpty() ? EnumSet.of(RoleName.ROLE_USER) : EnumSet.copyOf(roleNames);
        wanted.add(RoleName.ROLE_USER);
        if (user.hasRole(RoleName.ROLE_ADMIN) && !wanted.contains(RoleName.ROLE_ADMIN) && user.isEnabled()) {
            ensureAnotherAdminRemains(Attempt.CHANGE_ROLES, user);
        }
        Set<RoleName> before = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        if (before.equals(wanted)) {
            // Rien ne change : ni entrée d'audit, ni déconnexion forcée de l'utilisateur.
            return UserProfile.of(user);
        }
        user.getRoles().clear();
        user.getRoles().addAll(resolveRoles(wanted));
        events.publishEvent(new UserAccountEvent.RolesChanged(user.getId(), user.getUsername(), before, wanted));
        events.publishEvent(new UserCredentialsRevoked(userId, UserCredentialsRevoked.Reason.ROLES_CHANGED));
        return UserProfile.of(user);
    }

    @Transactional
    public void delete(Long userId) {
        User user = load(userId);
        if (user.isEnabled() && user.hasRole(RoleName.ROLE_ADMIN)) {
            ensureAnotherAdminRemains(Attempt.DELETE, user);
        }
        events.publishEvent(new UserAccountEvent.Deleted(user.getId(), user.getUsername()));
        // Les jetons de rafraîchissement doivent disparaître avant l'utilisateur (clé étrangère) :
        // l'écouteur du module auth s'exécute de façon synchrone dans cette transaction.
        events.publishEvent(new UserCredentialsRevoked(userId, UserCredentialsRevoked.Reason.ACCOUNT_DELETED));
        users.delete(user);
    }

    /**
     * Crée l'administrateur initial si aucun administrateur actif n'existe.
     *
     * @return {@code true} si un compte a été créé
     */
    @Transactional
    public boolean createAdminIfNoneExists(String username, String email, String password) {
        if (users.countEnabledWithRole(RoleName.ROLE_ADMIN) > 0) {
            return false;
        }
        User admin = create(username, email, password, EnumSet.of(RoleName.ROLE_USER, RoleName.ROLE_ADMIN));
        events.publishEvent(new UserAccountEvent.AdminBootstrapped(admin.getId(), admin.getUsername()));
        return true;
    }

    // ----------------------------------------------------------------------------------------

    private User create(String username, String email, String password, Set<RoleName> roleNames) {
        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(normalizedUsername)) throw UserException.usernameTaken();
        if (users.existsByEmailIgnoreCase(normalizedEmail)) throw UserException.emailTaken();

        User user = new User(normalizedUsername, normalizedEmail, passwordEncoder.encode(password));
        user.getRoles().addAll(resolveRoles(roleNames));
        return users.save(user);
    }

    private Set<Role> resolveRoles(Set<RoleName> names) {
        return names.stream()
                .map(name -> roles.findByName(name)
                        .orElseThrow(() -> new IllegalStateException("Rôle absent de la base : " + name)))
                .collect(Collectors.toSet());
    }

    private void ensureAnotherAdminRemains(Attempt attempt, User user) {
        if (users.countEnabledWithRole(RoleName.ROLE_ADMIN) <= 1) {
            events.publishEvent(new UserAccountEvent.LastAdminProtected(user.getId(), user.getUsername(), attempt));
            throw UserException.lastAdmin();
        }
    }

    private static boolean changed(Object current, Object wanted, String field, List<String> changed) {
        if (Objects.equals(current, wanted)) {
            return false;
        }
        changed.add(field);
        return true;
    }

    private User load(Long userId) {
        return users.findById(userId).orElseThrow(() -> UserException.notFound(userId));
    }

    private static UserAccount toAccount(User user) {
        Set<RoleName> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return new UserAccount(user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash(),
                roleNames, user.canAuthenticate());
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
