package com.event.domain.entity;

import com.event.domain.model.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@DynamicUpdate
@Table(name = "event", schema = "promotion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE promotion.event SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "event_url", length = 300)
    private String eventUrl;

    // 정기 추첨형 이벤트에서 당첨자 선정 주기를 나타내는 값이다.
    @Column(name = "winner_selection_cycle")
    private Integer winnerSelectionCycle;

    // 정기 추첨형 이벤트에서 선정 기준 시각으로 쓰는 값이다.
    @Column(name = "winner_selection_base_at")
    private Instant winnerSelectionBaseAt;

    // 노출 정렬 우선순위다. 숫자가 낮을수록 먼저 노출될 수 있다.
    @Column(name = "priority", nullable = false)
    private Integer priority;

    // 운영 중인 이벤트인지 여부다.
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    // 사용자 화면에 노출할지 여부다.
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible;

    // 별도 사용자 액션 없이 자동 응모 가능한지 여부다.
    @Column(name = "is_auto_entry", nullable = false)
    private Boolean isAutoEntry;

    // SNS 연동이 필요한 이벤트인지 여부다.
    @Column(name = "is_sns_linked", nullable = false)
    private Boolean isSnsLinked;

    // 당첨자 발표가 끝났는지 여부다.
    @Column(name = "is_winner_announced", nullable = false)
    private Boolean isWinnerAnnounced;

    // 같은 회원의 중복 당첨 허용 여부다.
    @Column(name = "is_duplicate_winner", nullable = false)
    private Boolean isDuplicateWinner;

    // 같은 회원의 다중 응모 허용 여부다.
    @Column(name = "is_multiple_entry", nullable = false)
    private Boolean isMultipleEntry;

    @Column(name = "description")
    private String description;

    @Builder
    private EventEntity(
            Long id,
            String eventName,
            EventType eventType,
            Instant startAt,
            Instant endAt,
            Long supplierId,
            String eventUrl,
            Integer winnerSelectionCycle,
            Instant winnerSelectionBaseAt,
            Integer priority,
            Boolean isActive,
            Boolean isVisible,
            Boolean isAutoEntry,
            Boolean isSnsLinked,
            Boolean isWinnerAnnounced,
            Boolean isDuplicateWinner,
            Boolean isMultipleEntry,
            String description
    ) {
        this.id = id;
        this.eventName = eventName;
        this.eventType = eventType;
        this.startAt = startAt;
        this.endAt = endAt;
        this.supplierId = supplierId;
        this.eventUrl = eventUrl;
        this.winnerSelectionCycle = winnerSelectionCycle;
        this.winnerSelectionBaseAt = winnerSelectionBaseAt;
        this.priority = priority;
        this.isActive = isActive;
        this.isVisible = isVisible;
        this.isAutoEntry = isAutoEntry;
        this.isSnsLinked = isSnsLinked;
        this.isWinnerAnnounced = isWinnerAnnounced;
        this.isDuplicateWinner = isDuplicateWinner;
        this.isMultipleEntry = isMultipleEntry;
        this.description = description;
    }
}
