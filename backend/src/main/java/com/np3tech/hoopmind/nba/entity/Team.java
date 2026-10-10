package com.np3tech.hoopmind.nba.entity;

import com.np3tech.hoopmind.nba.entity.enummeration.Conference;
import com.np3tech.hoopmind.nba.entity.enummeration.Division;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "teams", indexes = {
        @Index(name = "idx_team_abbreviation", columnList = "abbreviation"),
        @Index(name = "idx_team_conference", columnList = "conference")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder(toBuilder = true)
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, unique = true, length = 10)
    private String abbreviation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Conference conference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Division division;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @Column(length = 200)
    private String arena;

    @Column(name = "head_coach", length = 200)
    private String headCoach;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
