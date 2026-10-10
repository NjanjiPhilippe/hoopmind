package com.np3tech.hoopmind.auth.internal;

import com.np3tech.hoopmind.user.UserAccount;
import com.np3tech.hoopmind.user.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Utilisé uniquement par l'authentification HTTP Basic de la documentation Swagger.
 */
@Service
@RequiredArgsConstructor
class AccountUserDetailsService implements UserDetailsService {

    private final UserAccountService accounts;

    @Override
    public UserDetails loadUserByUsername(String login) {
        UserAccount account = accounts.findAccountByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Compte inconnu"));
        return User.withUsername(account.username())
                .password(account.passwordHash())
                .authorities(account.roles().stream().map(role -> new SimpleGrantedAuthority(role.name())).toList())
                .disabled(!account.active())
                .build();
    }
}
