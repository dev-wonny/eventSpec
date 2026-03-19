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

@Getter
@Entity
@Table(name = "event_round_prize", schema = "promotion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE promotion.event_round_prize SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
public class EventRoundPrizeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("회차 보상 매핑 ID")
    private Long id;

    @Comment("회차")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private EventRoundEntity round;

    @Comment("보상 ID")
    @Column(name = "prize_id", nullable = false)
    private Long prizeId;

    @Comment("보상 우선순위")
    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Comment("일일 지급 제한 수량")
    @Column(name = "daily_limit")
    private Integer dailyLimit;

    @Comment("전체 지급 제한 수량")
    @Column(name = "total_limit")
    private Integer totalLimit;

    @Comment("보상 매핑 활성 여부")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder
    private EventRoundPrizeEntity(
            Long id,
            EventRoundEntity round,
            Long prizeId,
            Integer priority,
            Integer dailyLimit,
            Integer totalLimit,
            Boolean isActive
    ) {
        this.id = id;
        this.round = round;
        this.prizeId = prizeId;
        this.priority = priority;
        this.dailyLimit = dailyLimit;
        this.totalLimit = totalLimit;
        this.isActive = isActive;
    }
}
