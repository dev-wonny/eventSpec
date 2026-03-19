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
@Table(name = "event_win", schema = "promotion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE promotion.event_win SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventWinEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("당첨 이력 ID")
    private Long id;

    @Comment("참여 이력 ID")
    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Comment("회차")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private EventRoundEntity round;

    @Comment("이벤트")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Comment("회원 ID")
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Comment("회차 보상 매핑 ID")
    @Column(name = "event_round_prize_id")
    private Long eventRoundPrizeId;

    @Builder
    private EventWinEntity(
            Long id,
            Long entryId,
            EventRoundEntity round,
            EventEntity event,
            Long memberId,
            Long eventRoundPrizeId
    ) {
        this.id = id;
        this.entryId = entryId;
        this.round = round;
        this.event = event;
        this.memberId = memberId;
        this.eventRoundPrizeId = eventRoundPrizeId;
    }

    public static EventWinEntity create(
            Long entryId,
            EventRoundEntity round,
            Long memberId,
            Long eventRoundPrizeId,
            Long actor
    ) {
        // win은 실제 보상 지급 대상이 생겼을 때만 저장한다.
        EventWinEntity entity = EventWinEntity.builder()
                .entryId(entryId)
                .round(round)
                .event(round.getEvent())
                .memberId(memberId)
                .eventRoundPrizeId(eventRoundPrizeId)
                .build();
        entity.initializeAudit(actor);
        return entity;
    }
}
