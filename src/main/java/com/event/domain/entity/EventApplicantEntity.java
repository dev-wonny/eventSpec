package com.event.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "event_applicant", schema = "promotion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE promotion.event_applicant SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventApplicantEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("applicant ID")
    private Long id;

    @Comment("이벤트")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Comment("회차")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private EventRoundEntity round;

    @Comment("회원 ID")
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Builder
    private EventApplicantEntity(
            Long id,
            EventEntity event,
            EventRoundEntity round,
            Long memberId
    ) {
        this.id = id;
        this.event = event;
        this.round = round;
        this.memberId = memberId;
    }

    public static EventApplicantEntity create(
            EventEntity event,
            EventRoundEntity round,
            Long memberId,
            Long actor
    ) {
        // applicant는 "이 회원이 이 회차에 응모를 시도했다"는 선행 기록이다.
        EventApplicantEntity entity = EventApplicantEntity.builder()
                .event(event)
                .round(round)
                .memberId(memberId)
                .build();
        entity.initializeAudit(actor);
        return entity;
    }
}
