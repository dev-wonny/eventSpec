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
@Table(name = "event", schema = "event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE event.event SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "event_url", length = 300)
    private String eventUrl;

    @Column(name = "winner_selection_cycle")
    private Integer winnerSelectionCycle;

    @Column(name = "winner_selection_base_at")
    private Instant winnerSelectionBaseAt;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible;

    @Column(name = "is_auto_entry", nullable = false)
    private Boolean isAutoEntry;

    @Column(name = "is_sns_linked", nullable = false)
    private Boolean isSnsLinked;

    @Column(name = "is_winner_announced", nullable = false)
    private Boolean isWinnerAnnounced;

    @Column(name = "is_duplicate_winner", nullable = false)
    private Boolean isDuplicateWinner;

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

