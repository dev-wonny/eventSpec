package com.event.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "event_round_prize_probability", schema = "promotion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE promotion.event_round_prize_probability SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventRoundPrizeProbabilityEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "event_round_prize_id", nullable = false)
    private Long eventRoundPrizeId;

    // 보상 당첨 확률이다. 백분율 또는 운영 정의값으로 해석한다.
    @Column(name = "probability", nullable = false, precision = 5, scale = 2)
    private BigDecimal probability;

    // 동일 확률 내 추가 가중치가 필요할 때 사용하는 값이다.
    @Column(name = "weight", nullable = false)
    private Integer weight;

    // 현재 확률 규칙이 활성 상태인지 여부다.
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder
    private EventRoundPrizeProbabilityEntity(
            Long id,
            Long roundId,
            Long eventRoundPrizeId,
            BigDecimal probability,
            Integer weight,
            Boolean isActive
    ) {
        this.id = id;
        this.roundId = roundId;
        this.eventRoundPrizeId = eventRoundPrizeId;
        this.probability = probability;
        this.weight = weight != null ? weight : 1;
        this.isActive = isActive;
    }
}
