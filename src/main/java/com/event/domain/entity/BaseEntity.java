package com.event.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Comment("삭제 여부")
    @Column(name = "is_deleted", nullable = false)
    protected Boolean isDeleted = Boolean.FALSE;

    @Comment("생성 일시")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    protected Instant createdAt;

    @Comment("생성 주체 회원 ID")
    @Column(name = "created_by", nullable = false)
    protected Long createdBy;

    @Comment("수정 일시")
    @LastModifiedDate
    @Column(name = "updated_at")
    protected Instant updatedAt;

    @Comment("수정 주체 회원 ID")
    @Column(name = "updated_by", nullable = false)
    protected Long updatedBy;

    @Comment("삭제 일시")
    @Column(name = "deleted_at")
    protected Instant deletedAt;

    @PrePersist
    protected void prePersist() {
        if (Objects.isNull(isDeleted)) {
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
