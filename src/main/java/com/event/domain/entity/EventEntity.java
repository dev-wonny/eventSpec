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
import org.hibernate.annotations.Comment;
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
    @Comment("이벤트 ID")
    private Long id;

    @Comment("이벤트명")
    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Enumerated(EnumType.STRING)
    @Comment("이벤트 유형")
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Comment("이벤트 시작 시각")
    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Comment("이벤트 종료 시각")
    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Comment("공급사 ID")
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Comment("이벤트 URL")
    @Column(name = "event_url", length = 300)
    private String eventUrl;

    @Comment("당첨자 선정 주기")
    @Column(name = "winner_selection_cycle")
    private Integer winnerSelectionCycle;

    @Comment("당첨자 선정 기준 시각")
    @Column(name = "winner_selection_base_at")
    private Instant winnerSelectionBaseAt;

    @Comment("노출 우선순위")
    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Comment("이벤트 활성화 여부")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Comment("이벤트 노출 여부")
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible;

    @Comment("자동 응모 여부")
    @Column(name = "is_auto_entry", nullable = false)
    private Boolean isAutoEntry;

    @Comment("SNS 연동 여부")
    @Column(name = "is_sns_linked", nullable = false)
    private Boolean isSnsLinked;

    @Comment("당첨자 발표 완료 여부")
    @Column(name = "is_winner_announced", nullable = false)
    private Boolean isWinnerAnnounced;

    @Comment("중복 당첨 허용 여부")
    @Column(name = "is_duplicate_winner", nullable = false)
    private Boolean isDuplicateWinner;

    @Comment("다중 응모 허용 여부")
    @Column(name = "is_multiple_entry", nullable = false)
    private Boolean isMultipleEntry;

    @Comment("이벤트 설명")
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
