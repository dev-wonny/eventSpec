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
@Table(name = "event_round", schema = "promotion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE promotion.event_round SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventRoundEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("회차 ID")
    private Long id;

    @Comment("이벤트")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Comment("회차 번호")
    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    @Comment("회차 시작 시각")
    @Column(name = "round_start_at")
    private Instant roundStartAt;

    @Comment("회차 종료 시각")
    @Column(name = "round_end_at")
    private Instant roundEndAt;

    @Comment("추첨 시각")
    @Column(name = "draw_at")
    private Instant drawAt;

    @Comment("회차 확정 여부")
    @Column(name = "is_confirmed", nullable = false)
    private Boolean isConfirmed;

    @Builder
    private EventRoundEntity(
            Long id,
            EventEntity event,
            Integer roundNo,
            Instant roundStartAt,
            Instant roundEndAt,
            Instant drawAt,
            Boolean isConfirmed
    ) {
        this.id = id;
        this.event = event;
        this.roundNo = roundNo;
        this.roundStartAt = roundStartAt;
        this.roundEndAt = roundEndAt;
        this.drawAt = drawAt;
        this.isConfirmed = isConfirmed;
    }
}
