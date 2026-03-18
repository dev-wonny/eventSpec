package com.event.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    // 소프트 삭제 여부다. 실제 삭제 대신 플래그와 시각을 남긴다.
    @Column(name = "is_deleted", nullable = false)
    protected Boolean isDeleted = Boolean.FALSE;

    // 최초 등록 시각이다. JPA auditing이 저장 시 자동으로 채운다.
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    protected Instant createdAt;

    // 최초 등록 주체다.
    @Column(name = "created_by", nullable = false)
    protected Long createdBy;

    // 마지막 수정 시각이다. JPA auditing이 저장/수정 시 자동으로 갱신한다.
    @LastModifiedDate
    @Column(name = "updated_at")
    protected Instant updatedAt;

    // 마지막 수정 주체다.
    @Column(name = "updated_by", nullable = false)
    protected Long updatedBy;

    // 소프트 삭제 시각이다.
    @Column(name = "deleted_at")
    protected Instant deletedAt;

    @PrePersist
    protected void prePersist() {
        if (isDeleted == null) {
            isDeleted = Boolean.FALSE;
        }
    }

    protected void initializeAudit(Long actor) {
        // 생성 팩토리에서는 actor 관련 audit만 채우고 시각은 auditing에 맡긴다.
        this.createdBy = actor;
        this.updatedBy = actor;
        this.isDeleted = Boolean.FALSE;
    }
}
