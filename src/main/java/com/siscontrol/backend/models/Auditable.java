package com.siscontrol.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
// 1. Agregar el Listener para que Spring "escuche" los cambios
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    // 2. Usar @CreatedDate (ya no necesitas el = LocalDateTime.now())
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    // 3. Usar @LastModifiedDate para que se actualice solo en cada save()
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}