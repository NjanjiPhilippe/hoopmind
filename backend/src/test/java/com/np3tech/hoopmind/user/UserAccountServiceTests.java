package com.np3tech.hoopmind.user;

import com.np3tech.hoopmind.user.domain.Role;
import com.np3tech.hoopmind.user.domain.RoleName;
import com.np3tech.hoopmind.user.domain.RoleRepository;
import com.np3tech.hoopmind.user.domain.User;
import com.np3tech.hoopmind.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountServiceTests {

    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final UserAccountService service =
            new UserAccountService(users, roles, NoOpPasswordEncoder.getInstance(), events, Clock.systemUTC());

    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User("admin", "admin@example.com", "secret");
        ReflectionTestUtils.setField(admin, "id", 1L);
        admin.getRoles().addAll(Set.of(role(RoleName.ROLE_USER), role(RoleName.ROLE_ADMIN)));
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        when(roles.findByName(any())).thenAnswer(call -> Optional.of(role(call.getArgument(0))));
    }

    @Test
    void lastAdminCannotBeDisabled() {
        when(users.countEnabledWithRole(RoleName.ROLE_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.setEnabled(1L, false)).isInstanceOf(UserException.class);
        assertThat(admin.isEnabled()).isTrue();
        verify(events, never()).publishEvent(any(UserCredentialsRevoked.class));
    }

    @Test
    void lastAdminCannotLoseTheAdminRole() {
        when(users.countEnabledWithRole(RoleName.ROLE_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.setRoles(1L, Set.of(RoleName.ROLE_USER))).isInstanceOf(UserException.class);
        assertThat(admin.hasRole(RoleName.ROLE_ADMIN)).isTrue();
    }

    @Test
    void lastAdminCannotBeDeleted() {
        when(users.countEnabledWithRole(RoleName.ROLE_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(UserException.class);
        verify(users, never()).delete(any());
    }

    @Test
    void adminCanBeDisabledWhenAnotherAdminRemains() {
        when(users.countEnabledWithRole(RoleName.ROLE_ADMIN)).thenReturn(2L);

        service.setEnabled(1L, false);

        assertThat(admin.isEnabled()).isFalse();
        verify(events).publishEvent(new UserCredentialsRevoked(1L, UserCredentialsRevoked.Reason.ACCOUNT_DISABLED));
    }

    @Test
    void userRoleIsAlwaysKept() {
        when(users.countEnabledWithRole(RoleName.ROLE_ADMIN)).thenReturn(2L);

        UserProfile profile = service.setRoles(1L, Set.of());

        assertThat(profile.roles()).containsExactly(RoleName.ROLE_USER);
    }

    private static Role role(RoleName name) {
        Role role = org.springframework.beans.BeanUtils.instantiateClass(Role.class);
        ReflectionTestUtils.setField(role, "name", name);
        return role;
    }
}
