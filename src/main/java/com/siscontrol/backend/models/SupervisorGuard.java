package com.siscontrol.backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "supervisor_guard")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorGuard {

    @EmbeddedId
    private SupervisorGuardId id;

    @ManyToOne
    @MapsId("supervisorId")
    @JoinColumn(name = "supervisor_id")
    private User supervisor;

    @ManyToOne
    @MapsId("guardId")
    @JoinColumn(name = "guard_id")
    private User guard;

}
