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
@Table(name = "event_win", schema = "promotion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE promotion.event_win SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventWinEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 어떤 회차 보상으로 당첨됐는지 가리키는 연결 키다.
    @Column(name = "event_round_prize_id")
    private Long eventRoundPrizeId;

    @Builder
    private EventWinEntity(
            Long id,
            Long entryId,
            Long roundId,
            Long eventId,
            Long memberId,
            Long eventRoundPrizeId
    ) {
        this.id = id;
        this.entryId = entryId;
        this.roundId = roundId;
        this.eventId = eventId;
        this.memberId = memberId;
        this.eventRoundPrizeId = eventRoundPrizeId;
    }

    public static EventWinEntity create(
            Long entryId,
            Long roundId,
            Long eventId,
            Long memberId,
            Long eventRoundPrizeId,
            Long actor
    ) {
        // win은 실제 보상 지급 대상이 생겼을 때만 저장한다.
        EventWinEntity entity = EventWinEntity.builder()
                .entryId(entryId)
                .roundId(roundId)
                .eventId(eventId)
                .memberId(memberId)
                .eventRoundPrizeId(eventRoundPrizeId)
                .build();
        entity.initializeAudit(actor);
        return entity;
    }
}
