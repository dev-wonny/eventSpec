package com.event.domain.entity;

import com.event.domain.model.RewardType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "prize", schema = "promotion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE promotion.prize SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
public class PrizeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prize_name", nullable = false, length = 100)
    private String prizeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 50)
    private RewardType rewardType;

    // 포인트형 보상일 때만 사용되는 수량이다.
    @Column(name = "point_amount")
    private Integer pointAmount;

    // 쿠폰형 보상일 때 연결되는 외부/내부 쿠폰 ID다.
    @Column(name = "coupon_id")
    private Long couponId;

    // 운영 중인 보상인지 여부다.
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    // 운영자 메모나 상세 설명으로 사용할 수 있는 필드다.
    @Column(name = "prize_description")
    private String prizeDescription;

    @Builder
    private PrizeEntity(
            Long id,
            String prizeName,
            RewardType rewardType,
            Integer pointAmount,
            Long couponId,
            Boolean isActive,
            String prizeDescription
    ) {
        this.id = id;
        this.prizeName = prizeName;
        this.rewardType = rewardType;
        this.pointAmount = pointAmount;
        this.couponId = couponId;
        this.isActive = isActive;
        this.prizeDescription = prizeDescription;
    }
}
