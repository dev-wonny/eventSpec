package com.event.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    // 출석을 시도한 회차 ID다. 중복 출석 제어의 기준으로도 사용된다.
    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Builder
    private EventApplicantEntity(Long id, Long eventId, Long roundId, Long memberId) {
        this.id = id;
        this.eventId = eventId;
        this.roundId = roundId;
        this.memberId = memberId;
    }

    public static EventApplicantEntity create(
            Long eventId,
            Long roundId,
            Long memberId,
            Long actor
    ) {
        // applicant는 "이 회원이 이 회차에 응모를 시도했다"는 선행 기록이다.
        EventApplicantEntity entity = EventApplicantEntity.builder()
                .eventId(eventId)
                .roundId(roundId)
                .memberId(memberId)
                .build();
        entity.initializeAudit(actor);
        return entity;
    }
}
