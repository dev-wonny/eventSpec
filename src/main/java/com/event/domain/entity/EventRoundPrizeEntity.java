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

@Getter
@Entity
@Table(name = "event_round_prize", schema = "promotion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE promotion.event_round_prize SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
public class EventRoundPrizeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "prize_id", nullable = false)
    private Long prizeId;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "daily_limit")
    private Integer dailyLimit;

    @Column(name = "total_limit")
    private Integer totalLimit;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder
    private EventRoundPrizeEntity(
            Long id,
            Long roundId,
            Long prizeId,
            Integer priority,
            Integer dailyLimit,
            Integer totalLimit,
            Boolean isActive
    ) {
        this.id = id;
        this.roundId = roundId;
        this.prizeId = prizeId;
        this.priority = priority;
        this.dailyLimit = dailyLimit;
        this.totalLimit = totalLimit;
        this.isActive = isActive;
    }
}
