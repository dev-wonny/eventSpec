package com.event.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
    private Long id;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "event_round_prize_id")
    private Long eventRoundPrizeId;

    @Column(name = "is_winner", nullable = false)
    private Boolean isWinner;

    @Builder
    private EventEntryEntity(
            Long id,
            Long applicantId,
            Long eventId,
            Long memberId,
            Instant appliedAt,
            Long eventRoundPrizeId,
            Boolean isWinner
    ) {
        this.id = id;
        this.applicantId = applicantId;
        this.eventId = eventId;
        this.memberId = memberId;
        this.appliedAt = appliedAt;
        this.eventRoundPrizeId = eventRoundPrizeId;
        this.isWinner = isWinner;
    }

    public static EventEntryEntity create(
            Long applicantId,
            Long eventId,
            Long memberId,
            Long eventRoundPrizeId,
            boolean isWinner,
            Long actor
    ) {
        EventEntryEntity entity = EventEntryEntity.builder()
                .applicantId(applicantId)
                .eventId(eventId)
                .memberId(memberId)
                .appliedAt(Instant.now())
                .eventRoundPrizeId(eventRoundPrizeId)
                .isWinner(isWinner)
                .build();
        entity.initializeAudit(actor);
        return entity;
    }
}
