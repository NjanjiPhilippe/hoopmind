package com.np3tech.hoopmind.user.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u where lower(u.username) = lower(:login) or lower(u.email) = lower(:login)")
    Optional<User> findByLogin(@Param("login") String login);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    long countByEnabledTrue();

    @Query("""
            select u from User u
            where lower(u.username) like lower(concat('%', :search, '%'))
               or lower(u.email) like lower(concat('%', :search, '%'))
               or lower(coalesce(u.firstName, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(u.lastName, '')) like lower(concat('%', :search, '%'))
            """)
    /** {@code search} vide : tous les utilisateurs. */
    Page<User> search(@Param("search") String search, Pageable pageable);

    @Query("select count(u) from User u join u.roles r where r.name = :role and u.enabled = true")
    long countEnabledWithRole(@Param("role") RoleName role);
}
