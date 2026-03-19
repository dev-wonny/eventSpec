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
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "event_round_prize_probability", schema = "promotion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE promotion.event_round_prize_probability SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventRoundPrizeProbabilityEntity extends BaseEntity {

    private static final Integer DEFAULT_WEIGHT = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("확률 규칙 ID")
    private Long id;

    @Comment("회차")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private EventRoundEntity round;

    @Comment("회차 보상 매핑 ID")
    @Column(name = "event_round_prize_id", nullable = false)
    private Long eventRoundPrizeId;

    @Comment("보상 당첨 확률")
    @Column(name = "probability", nullable = false, precision = 5, scale = 2)
    private BigDecimal probability;

    @Comment("가중치")
    @Column(name = "weight", nullable = false)
    private Integer weight;

    @Comment("확률 규칙 활성 여부")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder
    private EventRoundPrizeProbabilityEntity(
            Long id,
            EventRoundEntity round,
            Long eventRoundPrizeId,
            BigDecimal probability,
            Integer weight,
            Boolean isActive
    ) {
        this.id = id;
        this.round = round;
        this.eventRoundPrizeId = eventRoundPrizeId;
        this.probability = probability;
        this.weight = Objects.nonNull(weight) ? weight : DEFAULT_WEIGHT;
        this.isActive = isActive;
    }
}
