# Event Platform Core Domain Design

이 문서는 Event 플랫폼에서 가장 중요한 도메인 설계를 설명한다.

핵심 구성 요소:

- `Event` Aggregate
- `EventRound` 구조
- `Reward / Prize` 지급 구조

이 세 가지는 이벤트 시스템에서 가장 많은 버그와 복잡도를 만드는 영역이므로 설계 원칙을 명확히 한다.

## 1. JPA 상속 대신 DomainService 전략

이벤트 플랫폼은 `AttendanceEventEntity extends EventEntity` 같은 JPA 상속 구조보다 `EventEntity + DomainService` 전략을 우선 사용한다.

이유:

- `event` 테이블은 하나로 유지한다.
- 이벤트 타입이 늘어날수록 엔티티 상속 계층은 빠르게 복잡해진다.
- 실제 차이는 DB 구조보다 참여 규칙, 당첨 판정, 보상 지급 정책에서 더 크게 발생한다.

원칙:

- `EventEntity`는 공통 데이터와 persistence 상태만 가진다.
- 이벤트 타입별 행위와 규칙은 `DomainService` 또는 타입별 processor에서 처리한다.
- 출석체크처럼 현재 구현 범위가 하나뿐인 경우에는 공통 추상화보다 전용 processor를 우선한다.

## 2. Event Aggregate 설계

`Event`는 이벤트 시스템의 최상위 Aggregate Root다.

`Event Aggregate`의 책임:

- 이벤트 기본 정보 관리
- 이벤트 상태 관리
- 이벤트 타입 관리
- 이벤트 규칙 판단 기준 제공

현재 기준의 Aggregate 내부 구성:

- `Event`
- `EventRound`

`EventDisplayAsset`은 추후 전시 기능이 들어오면 같은 Aggregate 안에서 확장 가능한 후보로 본다.

`EventEntry`와 `EventWin`은 별도 Aggregate로 분리한다.

이유:

- 응모 기록은 대량 데이터가 발생한다.
- 이벤트 조회와 응모 데이터는 접근 패턴이 다르다.
- Aggregate 크기 비대화와 성능 문제를 방지한다.

구조 요약:

```text
Event Aggregate

Event
 -> EventRound

Separate Aggregate

EventEntry
EventWin
```

## 3. Event 상태 모델

`Event`는 아래와 같은 도메인 상태를 가질 수 있다.

- `CREATED`
- `ACTIVE`
- `ENDED`
- `ARCHIVED`

현재 DDL에는 별도 상태 컬럼이 없으므로, 상태는 도메인에서 계산된 값으로 본다.

`ACTIVE` 판단 기준:

- `isActive = true`
- `startAt <= now`
- `endAt >= now`

이벤트가 `ACTIVE` 상태일 때만 참여 가능하다.

이 상태 판단은 `Domain Service`에서 수행한다.

## 4. EventRound 설계

`EventRound`는 이벤트의 회차 개념이다.

중요:

- `event_round`는 공통 테이블이지만, `round`의 실제 의미는 `event_type`에 따라 달라진다.

타입별 해석 예:

| 이벤트 타입 | `round` 실제 의미 | 예시 |
| --- | --- | --- |
| `ATTENDANCE` | 날짜 | `2024-01-01`, `2024-01-02` |
| 응모형 이벤트 | 추첨 단위 | `1차 추첨`, `2차 추첨` |
| `RANDOM_REWARD` 또는 랜덤 게임형 이벤트 | 이벤트 scope / 기간 | `이벤트 기간 전체` |
| 게임 이벤트 | 라운드 / 세션 | `1라운드`, `2라운드` |
| 응모 후 당첨 이벤트 | 추첨 배치 | `3월 1차 배치`, `3월 2차 배치` |

예시:

- 출석 이벤트
  - 1일차
  - 2일차
  - 3일차
- 응모 이벤트
  - 1회차 추첨
  - 2회차 추첨
- 랜덤 게임 이벤트
  - 이벤트 기간 1개 회차

현재 DDL 기준 주요 컬럼:

- `id`
- `event_id`
- `round_no`
- `round_start_at`
- `round_end_at`
- `draw_at`
- `is_confirmed`

회차 도메인 책임:

- 회차 참여 가능 여부 판단
- 회차 진행 기간 판단
- 회차별 보상 규칙 연결
- 회차별 추첨/확정 기준 제공

즉, `event_round`는 "항상 날짜"가 아니라 "이벤트 타입별 참여 단위"로 보는 것이 더 정확하다.

참고:

- `entry_limit_per_member`, `winner_count` 같은 필드는 개념적으로 유효하지만 현재 DDL에는 없다.
- 이 값이 필요해지면 `EventRound` 확장 또는 별도 정책 테이블로 분리할 수 있다.

`EventRound`는 `Event Aggregate` 내부에 속한다.

## 5. EventEntry 구조

`EventEntry`는 사용자의 참여 기록이다.

`EventEntry`는 별도 Aggregate다.

현재 기준 주요 컬럼:

- `id`
- `event_id`
- `applicant_id`
- `member_id`
- `applied_at`

역할:

- 참여 기록 저장
- applicant 기준 회차 파생
- 참여 통계 생성의 기준 데이터 제공

중복 출석 방지:

- `event_applicant`의 DB Unique Constraint 사용
- Application validation 사용

현재 기준:

```text
UNIQUE(event_applicant.round_id, event_applicant.member_id)
```

복수 응모 이벤트인 경우에는 `is_multiple_entry` 정책을 함께 확인한다.

## 6. EventWin 구조

`EventWin`은 당첨 기록과 보상 지급 이력을 저장한다.

현재 기준 주요 컬럼:

- `id`
- `entry_id`
- `event_round_prize_id`
- `member_id`
- `round_id`
- `event_id`

역할:

- 당첨 기록 관리
- 보상 지급 이력 관리
- 당첨자 조회 기준 제공

구조:

```text
EventEntry -> EventWin
```

## 7. Prize / Reward 구조

현재 Reward 구조는 아래 테이블을 기준으로 한다.

### Prize

주요 정보:

- `reward_type`
- `point_amount`
- `coupon_id`
- `prize_name`

`Prize`는 immutable 정책으로 운영한다.

### EventRoundPrize

주요 정보:

- `round_id`
- `prize_id`
- `priority`
- `daily_limit`
- `total_limit`

### EventRoundPrizeProbability

주요 정보:

- `round_id`
- `event_round_prize_id`
- `probability`
- `weight`

이 구조를 통해 아래를 구현할 수 있다.

- 확률 기반 보상
- 수량 제한 보상
- 랜덤 리워드

현재 출석체크 범위에서는 재고/수량 제한을 사용하지 않는다. 포인트 보상 매핑이 있으면 모두 지급한다.

## 8. Reward 지급 흐름

Reward 지급 과정:

1. 이벤트 참여 요청
2. `EventEntry` 생성
3. Domain Service 검증
4. 당첨 여부 결정
5. `EventWin` 생성
6. Reward 지급 또는 지급 성공 이력 반영

즉시 당첨 이벤트 흐름:

```text
EventEntry 생성
 -> 확률 계산
 -> 당첨 여부 결정
 -> EventWin 저장
```

추첨 이벤트 흐름:

```text
EventEntry 생성
 -> 추첨 대상 등록
 -> Batch 추첨
 -> EventWin 생성
```

현재 출석체크는 보상 매핑이 있는 경우 `EventWin`까지 먼저 커밋하고, 외부 point API는 트랜잭션 밖에서 후행 호출하는 동기식 구조다.

## 9. 랜덤 리워드 설계

랜덤 리워드는 기본적으로 `weight` 기반으로 구현한다.

원칙:

- `weight` 기본값은 DB 스키마 기준 `1`이다.
- 모든 보상의 `weight`가 `1`이면 균등 추첨으로 해석한다.
- 특정 보상의 당첨 비중을 높이고 싶을 때만 `weight`를 증가시킨다.
- 현재 기준에서 계산의 기본축은 `probability`보다 `weight`다.
- Java 코드에서는 이 기본값을 숫자 리터럴로 직접 쓰지 않고 `DEFAULT_WEIGHT` 같은 상수로 관리한다.

예:

- Prize A -> weight 50
- Prize B -> weight 30
- Prize C -> weight 20

총 weight:

- `100`

랜덤 값 생성:

- `0 ~ 99`

weight 구간에 따라 Prize를 선택한다.

예:

- Prize A -> weight 1
- Prize B -> weight 1
- Prize C -> weight 3

위 경우 최종 비중은 `1 : 1 : 3`이다.

현재 스키마에서는 `event_round_prize_probability.weight` 기본값을 `1`로 두고, Java 코드에서는 같은 의미를 `DEFAULT_WEIGHT` 상수로 관리한다. `probability`는 향후 보정 규칙이 필요할 때 확장적으로 해석할 수 있다.

## 10. 동시성 문제

이벤트 시스템에서 가장 흔한 문제:

- 동시에 여러 사용자가 응모
- 경품 수량 초과
- 중복 당첨

해결 전략:

1. DB Unique Constraint
2. Application / Domain Service 검증
3. 향후 랜덤 리워드 재고 확인

수량 초과 방지 예:

- `현재 당첨 수 < 최대 수량` 확인 후 당첨 처리

중요:

- 현재 출석체크는 재고 검증을 하지 않는다.
- 향후 랜덤 리워드에서 재고 조회는 `Application Service`, 재고 가능 여부 판단은 `Domain Service`가 담당한다.

## 11. 이벤트 참여 흐름

전체 흐름:

```text
사용자 -> Controller
Controller -> ApplicationService
ApplicationService -> Repository 조회
ApplicationService -> DomainService 검증
ApplicationService -> Entry 저장
ApplicationService -> Win 저장
```

현재 구현 정책은 출석체크 전용 흐름을 우선한다.

- 지금은 출석체크만 개발 범위이므로 `entry / draw / reward`를 공통 인터페이스로 먼저 분리하지 않는다.
- 대신 `AttendanceProcessor` 같은 출석체크 전용 처리 컴포넌트 안에서 `entry -> draw -> reward`를 순서대로 수행하는 구조를 권장한다.
- 랜덤 리워드나 추첨 이벤트가 실제 구현 범위에 들어와 두 번째 구체 구현이 생기면, 그 시점에 공통 추상화가 정말 필요한지 다시 검토한다.

예:

```text
AttendEventService
 -> AttendanceDomainService.validate(...)
 -> AttendanceProcessor.process(...)
    -> entry
    -> draw
    -> reward
```

조회 흐름도 같은 원칙을 따른다.

```text
Controller -> ApplicationService
ApplicationService -> QueryDSL로 Event / Round / Win / Prize 개별 조회
ApplicationService -> DTO 조립
```

중요:

- 조회 DTO는 JPA 연관 컬렉션을 직접 따라가지 않는다.
- `EventDetailDto` 같은 DTO는 `ApplicationService`가 준비한 `rounds` 값을 받아 조립하는 것을 권장한다.
- 현재 프로젝트는 QueryDSL로 필요한 엔티티와 조회 결과를 각각 불러와 조립하는 방식을 기본으로 한다.

## 12. 이벤트 타입별 구조

현재 실제 사용 이벤트 타입:

- `ATTENDANCE`
- `RANDOM_REWARD`

향후 확장 가능 이벤트 타입:

- `GAME`
- `LOTTERY`

이벤트 타입에 따라 적용되는 DomainService가 달라질 수 있다.

### Attendance Event

- 출석 정책
- 하루 1회 참여
- 출석 누적 계산

### Random Reward Event

- 랜덤 보상 정책
- 확률 기반 보상
- 수량 제한 보상

### Game Event

- 게임 결과 기반 보상

### Lottery Event

- 응모 후 추첨

주의:

- 위 타입 구분은 JPA 상속 엔티티 구조를 의미하지 않는다.
- 실제 확장 기준은 `EventType + DomainService` 조합이다.

## 13. 이벤트 타입별 DomainService 구조

현재 기준 기본 단위는 별도 정책 인터페이스가 아니라 타입별 `DomainService`다.

예:

- `AttendanceDomainService`
- `RandomRewardDomainService`
- `GameDomainService`
- `LotteryDomainService`

`ApplicationService`는 유스케이스에 필요한 `DomainService`를 직접 호출한다.

다만 현재 구현 범위에서는 공통 인터페이스를 실제 코드로 먼저 도입하기보다, 출석체크 전용 `AttendanceProcessor`와 `AttendanceDomainService`를 우선 구현하는 방향이 더 단순하다.
즉 별도 정책 계층보다 타입별 `DomainService`를 먼저 두고, 공통 추상화는 실제 중복이 생길 때 검토한다.

## 14. 확장 방식

새 이벤트 타입이 추가되면 해당 타입 전용 `DomainService`를 추가한다.

예:

- `ATTENDANCE -> AttendanceDomainService`
- `RANDOM_REWARD -> RandomRewardDomainService`
- `GAME -> GameDomainService`

새 이벤트 타입 추가 시:

1. `EventType` 추가
2. `DomainService` 구현 추가
3. 해당 `ApplicationService` 흐름에 연결

## 15. 이벤트 시스템 설계 핵심

1. Event 테이블은 하나로 유지한다.
2. `EventType`으로 이벤트를 분기한다.
3. `DomainService`로 이벤트 규칙과 도메인 검증을 수행한다.
4. `ApplicationService`로 유스케이스를 실행한다.
5. `*Impl`로 DB 접근을 수행한다.
6. 이벤트 타입 확장은 JPA 상속보다 `EventEntity + DomainService` 전략을 우선한다.

## 16. 최종 아키텍처 요약

```text
Controller
 -> ApplicationService
   -> DomainService
   -> RepositoryPort
     -> *Impl
       -> Database
```

이 구조를 유지하면 아래 유형을 하나의 `Event` 테이블 기반으로 확장할 수 있다.

- 출석 이벤트
- 랜덤 리워드 이벤트
- 게임 이벤트
- 추첨 이벤트
