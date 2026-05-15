package com.siscontrol.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.siscontrol.backend.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String rut; // Formato: 12345678-K

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    private String password;

    private String fullName;

    @Column(length = 12)
    private String phoneNumber; // Formato: +56912345678

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private Integer status = 1; // 1: Activo, 0: Inactivo
}