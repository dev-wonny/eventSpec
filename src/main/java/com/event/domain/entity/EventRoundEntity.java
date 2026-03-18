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
@Table(name = "event_round", schema = "promotion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE promotion.event_round SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventRoundEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    // 회차별 개별 시작 시각이다. 없으면 이벤트 시작일과 roundNo로 계산하기도 한다.
    @Column(name = "round_start_at")
    private Instant roundStartAt;

    // 회차별 개별 종료 시각이다.
    @Column(name = "round_end_at")
    private Instant roundEndAt;

    // 추첨 시점이 따로 있는 이벤트에서 사용하는 시각이다.
    @Column(name = "draw_at")
    private Instant drawAt;

    // 운영 확정 여부다. 배치/관리자 확정 플로우에서 사용할 수 있다.
    @Column(name = "is_confirmed", nullable = false)
    private Boolean isConfirmed;

    @Builder
    private EventRoundEntity(
            Long id,
            Long eventId,
            Integer roundNo,
            Instant roundStartAt,
            Instant roundEndAt,
            Instant drawAt,
            Boolean isConfirmed
    ) {
        this.id = id;
        this.eventId = eventId;
        this.roundNo = roundNo;
        this.roundStartAt = roundStartAt;
        this.roundEndAt = roundEndAt;
        this.drawAt = drawAt;
        this.isConfirmed = isConfirmed;
    }
}
