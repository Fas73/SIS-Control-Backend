package com.siscontrol.backend.models;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorGuardId implements Serializable{

    @Column(name = "supervisor_id")
    private Long supervisorId;

    @Column(name = "guard_id")
    private Long guardId;

}
