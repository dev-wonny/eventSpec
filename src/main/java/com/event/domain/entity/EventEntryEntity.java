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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "event_entry", schema = "promotion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE promotion.event_entry SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventEntryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("참여 이력 ID")
    private Long id;

    @Comment("applicant ID")
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Comment("이벤트")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Comment("회원 ID")
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Comment("응모 시각")
    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Comment("회차 보상 매핑 ID")
    @Column(name = "event_round_prize_id")
    private Long eventRoundPrizeId;

    @Comment("당첨 여부")
    @Column(name = "is_winner", nullable = false)
    private Boolean isWinner;

    @Builder
    private EventEntryEntity(
            Long id,
            Long applicantId,
            EventEntity event,
            Long memberId,
            Instant appliedAt,
            Long eventRoundPrizeId,
            Boolean isWinner
    ) {
        this.id = id;
        this.applicantId = applicantId;
        this.event = event;
        this.memberId = memberId;
        this.appliedAt = appliedAt;
        this.eventRoundPrizeId = eventRoundPrizeId;
        this.isWinner = isWinner;
    }

    public static EventEntryEntity create(
            EventApplicantEntity applicant,
            Long memberId,
            Long eventRoundPrizeId,
            boolean isWinner,
            Long actor
    ) {
        // entry는 실제 출석 완료 이력을 남기는 핵심 레코드다.
        EventEntryEntity entity = EventEntryEntity.builder()
                .applicantId(applicant.getId())
                .event(applicant.getEvent())
                .memberId(memberId)
                .appliedAt(Instant.now())
                .eventRoundPrizeId(eventRoundPrizeId)
                .isWinner(isWinner)
                .build();
        entity.initializeAudit(actor);
        return entity;
    }
}
