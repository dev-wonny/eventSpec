package com.event.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "is_deleted", nullable = false)
    protected Boolean isDeleted = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    protected Instant createdAt;

    @Column(name = "created_by", length = 50)
    protected String createdBy;

    @Column(name = "updated_at")
    protected Instant updatedAt;

    @Column(name = "updated_by", length = 50)
    protected String updatedBy;

    @Column(name = "deleted_at")
    protected Instant deletedAt;

    @PrePersist
    protected void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (isDeleted == null) {
            isDeleted = Boolean.FALSE;
        }
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = Instant.now();
    }

    protected void initializeAudit(String actor) {
        Instant now = Instant.now();
        this.createdAt = now;
        this.createdBy = actor;
        this.updatedAt = now;
        this.updatedBy = actor;
        this.isDeleted = Boolean.FALSE;
    }
}

