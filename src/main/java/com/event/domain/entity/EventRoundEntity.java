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
@Table(name = "event_round", schema = "event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE event.event_round SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventRoundEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    @Column(name = "round_start_at")
    private Instant roundStartAt;

    @Column(name = "round_end_at")
    private Instant roundEndAt;

    @Column(name = "draw_at")
    private Instant drawAt;

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

