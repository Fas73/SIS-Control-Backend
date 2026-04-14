package com.siscontrol.backend.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "installations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "installation", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Checkpoint> checkpoints;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    
}
