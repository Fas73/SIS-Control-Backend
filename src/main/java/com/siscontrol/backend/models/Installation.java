package com.siscontrol.backend.models;
import com.siscontrol.backend.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "installations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Installation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;    // Usado en Postman
    private String clientName; // Usado en Postman
    private String location;   // Usado en Service

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;
}