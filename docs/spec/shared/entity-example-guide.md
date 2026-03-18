# Entity Example Guide

이 문서는 이벤트 플랫폼에서 사용할 JPA Entity 예제 코드를 정리한다.

목적:

- Entity가 실제로 어떤 모양으로 생기는지 예시를 제공한다.
- 현재 합의한 설계 기준을 코드 형태로 고정한다.
- `QueryDSL 조회 + JPA 저장` 구조에서 Entity의 책임 범위를 명확히 한다.

## 1. 기본 원칙

현재 프로젝트의 Entity 원칙은 아래와 같다.

- Entity는 persistence model이다.
- 저장과 상태 변경은 JPA Entity 기준으로 처리한다.
- 조회는 QueryDSL로 필요한 데이터를 각각 조회한다.
- JPA 연관관계 기반 join fetch, entity graph, lazy 컬렉션 순회는 기본 전략으로 사용하지 않는다.
- FK를 두지 않으므로 Entity도 연관 객체 대신 ID 필드를 기본으로 사용한다.
- 도메인 정책과 비즈니스 규칙은 Entity보다 `DomainService`, `Policy`, `Processor`에 둔다.

정리:

- `Entity = 저장 모델`
- `QueryDSL = 조회 모델`
- `Application Service = 조립`

## 2. 권장 패키지 예시

```text
com.event.domain
├── entity
│   ├── base
│   │   └── BaseEntity
│   ├── EventEntity
│   ├── EventRoundEntity
│   ├── EventApplicantEntity
│   ├── EventEntryEntity
│   ├── EventWinEntity
│   ├── EventRoundPrizeEntity
│   └── PrizeEntity
└── value
    └── EventType
```

## 3. EventType enum 예시

현재 실제 사용 이벤트 타입은 `ATTENDANCE`, `RANDOM_REWARD` 두 개다.

```java
package com.event.domain.value;

public enum EventType {
    ATTENDANCE,
    RANDOM_REWARD
}
```

## 4. BaseEntity 예시

공통 감사 컬럼은 `@MappedSuperclass`로 분리하는 방식을 권장한다.

```java
package com.event.domain.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false)
    protected LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    protected Long createdBy;

    @Column(name = "updated_at", nullable = false)
    protected LocalDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    protected Long updatedBy;

    @Column(name = "deleted_at")
    protected LocalDateTime deletedAt;

    @PrePersist
    protected void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

주의:

- 시간은 현재 운영 기준상 한국 시간(`Asia/Seoul`) 기준으로 해석한다.
- 감사 컬럼 입력 방식은 JPA Auditing 또는 서비스 직접 세팅 중 하나로 프로젝트에서 통일하면 된다.

## 5. EventEntity 예시

`EventEntity`는 이벤트 공통 상태를 저장하는 대표 Entity다.

```java
package com.event.domain.entity;

import com.event.domain.entity.base.BaseEntity;
import com.event.domain.value.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "event_url", length = 300)
    private String eventUrl;

    @Column(name = "winner_selection_cycle")
    private Integer winnerSelectionCycle;

    @Column(name = "winner_selection_base_at")
    private LocalDateTime winnerSelectionBaseAt;

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

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Builder
    private EventEntity(
            Long id,
            String eventName,
            EventType eventType,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Long supplierId,
            String eventUrl,
            Integer winnerSelectionCycle,
            LocalDateTime winnerSelectionBaseAt,
            Integer priority,
            Boolean isActive,
            Boolean isVisible,
            Boolean isAutoEntry,
            Boolean isSnsLinked,
            Boolean isWinnerAnnounced,
            Boolean isDuplicateWinner,
            Boolean isMultipleEntry,
            String description,
            Boolean isDeleted
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
        this.isDeleted = isDeleted;
    }
}
```

포인트:

- `eventType`은 `String`보다 enum을 사용한다.
- `eventRounds` 같은 컬렉션 필드는 현재 기준에서 두지 않는다.
- `@EqualsAndHashCode(of = "id")`는 현재 기준에서 사용하지 않는다.
- `supplierId`는 외부 시스템 식별자다.

## 6. EventRoundEntity 예시

`EventRoundEntity`는 회차를 나타내지만 `EventEntity`를 직접 참조하지 않고 `eventId`로 연결한다.
다만 이 회차의 실제 의미는 이벤트 타입마다 달라질 수 있다. 출석에서는 날짜, 추첨형 이벤트에서는 배치, 게임에서는 세션으로 해석할 수 있다.

```java
package com.event.domain.entity;

import com.event.domain.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
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
    private LocalDateTime roundStartAt;

    @Column(name = "round_end_at")
    private LocalDateTime roundEndAt;

    @Column(name = "draw_at")
    private LocalDateTime drawAt;

    @Column(name = "is_confirmed", nullable = false)
    private Boolean isConfirmed;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;
}
```

포인트:

- DB에 FK가 없으므로 JPA도 `EventEntity event` 대신 `Long eventId`를 기본으로 둔다.
- 정합성 검증은 서비스에서 수행한다.

## 7. EventApplicantEntity 예시

`EventApplicantEntity`는 회차별 applicant 기준 레코드다.

```java
package com.event.domain.entity;

import com.event.domain.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "event_applicant", schema = "event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE event.event_applicant SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventApplicantEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;
}
```

포인트:

- 현재 의미는 이벤트 단위 사전 참여 가능 대상 풀이 아니라 회차별 applicant 기준 데이터다.
- `(event_id, round_id, member_id)` 최소 unique는 DDL에서 관리한다.

## 8. EventEntryEntity 예시

출석 성공 시 실제 응모권/참여 이력을 저장하는 핵심 Entity다.

```java
package com.event.domain.entity;

import com.event.domain.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "event_entry", schema = "event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE event.event_entry SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventEntryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "event_round_prize_id")
    private Long eventRoundPrizeId;

    @Column(name = "is_winner", nullable = false)
    private Boolean isWinner;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;
}
```

포인트:

- 출석 중복 기준은 `(event_id, round_id, member_id)`다.
- partial unique index는 JPA annotation보다 DDL로 관리하는 것이 맞다.

## 9. EventWinEntity 예시

실제 보상 지급 성공 이력을 저장한다.

```java
package com.event.domain.entity;

import com.event.domain.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "event_win", schema = "event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE event.event_win SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class EventWinEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "event_round_prize_id")
    private Long eventRoundPrizeId;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;
}
```

포인트:

- 출석체크에서는 `event_entry`와 함께 로컬 트랜잭션 안에서 저장한다.
- 외부 point API 실패 여부와는 별개로 당첨/보상 확정 이력을 나타낸다.

## 10. PrizeEntity 예시

`Prize`는 운영상 immutable 정책으로 다루는 마스터다.

```java
package com.event.domain.entity;

import com.event.domain.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "prize", schema = "event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE event.prize SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = FALSE")
public class PrizeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prize_name", nullable = false, length = 100)
    private String prizeName;

    @Column(name = "reward_type", nullable = false, length = 20)
    private String rewardType;

    @Column(name = "point_amount")
    private Integer pointAmount;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "prize_description")
    private String prizeDescription;
}
```

포인트:

- 기술적으로 수정 가능 구조여도 운영 정책은 immutable이다.
- 변경이 필요하면 기존 row 수정 대신 새 `Prize`를 생성하는 방향을 사용한다.

## 11. 왜 연관관계 대신 ID 필드를 쓰는가

현재 프로젝트는 아래 이유로 연관 객체보다 ID 필드를 기본으로 사용한다.

- DB FK를 사용하지 않는다.
- QueryDSL로 개별 조회 후 조립하는 방식을 사용한다.
- JPA 연관 join, lazy 컬렉션 순회, join fetch 의존을 피하고 싶다.
- 서비스에서 정합성을 명시적으로 검증하려고 한다.

예:

```java
if (!round.getEventId().equals(event.getId())) {
    throw new IllegalArgumentException("ROUND_EVENT_MISMATCH");
}
```

## 12. Entity에서 하지 않는 것

Entity에서 하지 않는 것은 아래와 같다.

- 복잡한 비즈니스 규칙 판단
- 외부 API 호출
- QueryDSL 조회 조립
- 응답 DTO 생성
- 여러 Aggregate에 걸친 정책 판단

이 책임은 아래 계층으로 보낸다.

- `DomainService`
- `AttendanceProcessor`
- `ApplicationService`
- `QueryDSL Repository/Builder`

## 13. 구현 시 체크포인트

- partial unique index는 JPA annotation보다 DDL로 관리한다.
- soft delete는 `@SQLDelete`, `@SQLRestriction` 패턴을 권장한다.
- 조회는 QueryDSL 개별 조회, 저장은 JPA Entity를 기준으로 한다.
- Entity 컬렉션 연관관계는 현재 기본 구조에서 두지 않는다.
- Event 타입 확장은 JPA 상속보다 `EventType + Policy`로 처리한다.

## 14. 한 줄 정리

현재 이벤트 플랫폼 Entity는 `연관 객체 중심 JPA 모델`보다 `ID 필드 중심 persistence model`로 설계하는 것이 맞다.
