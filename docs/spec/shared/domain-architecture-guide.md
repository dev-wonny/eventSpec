# Event Platform Domain Architecture Guide

## 1. 설계 목적

Event 플랫폼은 하나의 이벤트 테이블을 기반으로 여러 이벤트 유형을 지원한다.

지원 이벤트 유형 예:

- 출석 이벤트 (`ATTENDANCE`)
- 랜덤 리워드 이벤트 (`RANDOM_REWARD`)

향후 확장 가능 이벤트 유형 예:

- 게임 이벤트 (`GAME`)
- 응모 후 추첨 이벤트 (`LOTTERY`)

이벤트는 하나의 DB 테이블(`event`)에 저장되지만, `event_type`에 따라 서로 다른 도메인 동작과 정책을 가진다. 따라서 아래 목표를 가진 구조를 설계한다.

- Event 테이블은 하나로 유지
- `EventType`에 따라 도메인 정책 분리
- 이벤트별 로직 분리
- 확장 가능한 이벤트 구조
- JPA / QueryDSL 기반 persistence 유지

## 2. JPA 상속 대신 Policy 전략 채택

이벤트 플랫폼은 `JPA 상속`보다 `Policy / Strategy` 패턴을 우선 선택한다.

선택 이유:

- 이벤트 타입이 늘어날수록 `AttendanceEventEntity`, `RandomRewardEventEntity`, `GameEventEntity` 같은 클래스 폭발이 발생하기 쉽다.
- 이벤트별 차이는 대부분 DB 구조보다 비즈니스 규칙과 운영 정책에서 발생한다.
- 현재 플랫폼은 단일 `event` 테이블을 유지해야 하므로, 타입별 행위를 Entity 상속 계층에 담는 방식보다 `EventEntity + Policy` 조합이 더 단순하고 확장에 유리하다.

권장 구조:

```text
EventEntity (data)
EventPolicy (behavior)
ApplicationService (orchestration)
```

정리:

- `EventEntity`: 공통 데이터와 persistence 상태
- `EventPolicy`: 이벤트 타입별 행위와 규칙
- `DomainService`: 공통 도메인 규칙 판단
- `ApplicationService`: 유스케이스 실행 흐름 조립

## 3. 전체 아키텍처 구조

이 프로젝트는 Spring Boot 기반 레이어드 아키텍처에 Hexagonal Architecture(Ports & Adapters) 패턴을 일부 적용한 하이브리드 구조를 따른다.

```text
Presentation
    -> Application
        -> Domain

Application Output Port
    -> Infrastructure
```

계층 역할:

- Presentation: HTTP 요청 처리
- Application: 유스케이스 실행
- Domain: 비즈니스 규칙과 정책 판단
- Infrastructure: JPA / QueryDSL / 외부 기술 구현

## 4. 패키지 구조

추천 구조:

```text
com.event
├── presentation
│   └── controller
├── application
│   ├── service
│   ├── port
│   └── dto
├── domain
│   ├── entity
│   ├── model
│   ├── policy
│   ├── service
│   └── exception
└── infrastructure
    ├── persistence
    ├── repository
    ├── builder
    └── config
```

패키지 역할:

- `domain.entity`: JPA Entity 같은 persistence model
- `domain.model`: 도메인 해석 모델
- `domain.policy`: 이벤트 타입별 정책
- `domain.service`: 여러 Entity에 걸친 비즈니스 규칙 판단
- `application.port`: 입력/출력 포트
- `infrastructure.persistence`: JPA / QueryDSL 구현

## 5. Event 테이블 설계

Event 플랫폼은 단일 `event` 테이블을 기반으로 한다.

주요 컬럼:

- `id`
- `event_name`
- `event_type`
- `start_at`
- `end_at`
- `supplier_id`
- `priority`
- `is_active`
- `is_visible`
- `is_duplicate_winner`
- `is_multiple_entry`

이벤트 유형 예:

- `ATTENDANCE`
- `RANDOM_REWARD`

향후 확장 후보:

- `GAME`
- `LOTTERY`

이벤트 유형에 따라 도메인 정책이 달라진다.

`event_round` 해석도 이벤트 유형에 따라 달라진다.

| 이벤트 타입 | `event_round` 의미 |
| --- | --- |
| `ATTENDANCE` | 날짜 단위 |
| 응모 이벤트 | 추첨 단위 |
| `RANDOM_REWARD` 또는 랜덤 게임형 이벤트 | 이벤트 scope / 기간 |
| `GAME` | 라운드 / 세션 |
| `LOTTERY` | 추첨 배치 |

## 6. EventEntity 역할

`EventEntity`는 persistence model(JPA Entity)이다.

책임:

- DB 매핑
- 이벤트 공통 상태 보관
- 연관 관계 관리

포함되는 정보:

- 이벤트 기본 정보
- 이벤트 기간
- 이벤트 타입
- 이벤트 운영 정책

중요 원칙:

- `EventEntity`는 도메인 정책을 가지지 않는다.
- `EventEntity`는 persistence model이다.
- `EventEntity`를 이벤트 타입별 JPA 상속 루트로 확장하지 않는다.

## 7. Domain Model 분리 전략

DB 모델과 도메인 모델은 다를 수 있다.

DB 모델:

- `EventEntity`

도메인 해석/정책 모델:

- `Event`
- `EventPolicy`
- `EventStatus`
- `RewardDecision`

주의:

- `AttendanceEvent`, `RandomRewardEvent` 같은 타입별 도메인 객체를 반드시 두지는 않는다.
- 이벤트 타입별 확장의 기본 수단은 `JPA 상속`이 아니라 `EventPolicy` 구현 분리다.
- 타입별 보조 모델이 필요하면 조회/계산 보조 객체로 둘 수 있지만, `EventEntity`의 상속 구조로 풀지는 않는다.

즉, DB 테이블은 하나지만 도메인 해석과 정책은 `EventType`에 따라 달라질 수 있다.

## 8. EventPolicy

`EventPolicy`는 이벤트 타입별 정책을 정의하는 핵심 인터페이스다.

역할:

- 이벤트 참여 가능 여부 검증
- 이벤트 타입별 규칙 수행
- 이벤트 정책 처리

예시 메서드:

- `validateEntry`
- `validateWinner`
- `determineReward`

## 9. EventPolicy 구현 예

### AttendanceEventPolicy

책임:

- 하루 1회 참여 제한
- 출석 누적 계산
- 출석 보상 지급 규칙

### RandomRewardPolicy

책임:

- 확률 기반 보상 지급
- 경품 재고 가능 여부 판단
- 중복 당첨 정책 검증

### GameEventPolicy

책임:

- 게임 점수 검증
- 보상 지급 규칙

### LotteryEventPolicy

책임:

- 응모 기록 판단
- 추첨 대상 등록 규칙

## 10. Policy Factory

이벤트 타입에 따라 사용할 정책은 `EventPolicyFactory`에서 선택한다.

입력:

- `eventType`

출력:

- `EventPolicy`

예:

- `ATTENDANCE` -> `AttendanceEventPolicy`
- `RANDOM_REWARD` -> `RandomRewardPolicy`

확장 시:

- `GAME` -> `GameEventPolicy`
- `LOTTERY` -> `LotteryEventPolicy`

`Application Service`는 Factory를 통해 적절한 정책 구현체를 가져온다.

## 11. Domain Service 역할

`Domain Service`는 도메인 정책 협력 로직과 여러 Entity에 걸친 비즈니스 규칙 판단을 담당한다.

예:

- `EventEntryDomainService`

책임:

- 이벤트 오픈 여부 검증
- 이벤트 기간 검증
- 중복 응모 검증
- 중복 당첨 검증
- 즉시 당첨 판정
- 향후 랜덤 리워드 재고 가능 여부 판단
- 이벤트 정책 호출

중요 원칙:

- `Domain Service`는 Repository를 직접 호출하지 않는다.
- `Application Service`가 필요한 데이터를 조회하고 `Domain Service`에 전달한다.
- 조회는 `Application Service`, 판단은 `Domain Service`가 담당한다.
- 조회는 JPA 연관 join 대신 QueryDSL로 필요한 엔티티와 값을 각각 조회해 전달하는 방식을 기본으로 한다.

현재 범위:

- 출석체크는 재고 관리를 하지 않는다.
- 포인트 보상 매핑이 있으면 별도 재고 검증 없이 지급한다.

예:

- 현재 출석체크: 보상 매핑 존재 여부 확인 후 지급
- 향후 랜덤 리워드: 현재 당첨 수 조회는 `Application Service`, 현재 당첨 수 vs 최대 당첨 수 비교는 `Domain Service`

## 12. Application Service 역할

`Application Service`는 유스케이스 실행 흐름을 담당한다.

예:

- `EventEntryService`

책임:

- 트랜잭션 관리
- Repository 호출
- `DomainService` 호출
- `Policy` 조회 및 호출
- 결과 DTO 생성

흐름:

1. Event 조회
2. 이벤트 정책 조회
3. Domain Service 검증
4. Entry 저장
5. 응답 반환

즉, 응모 규칙은 `Domain Service`와 `Policy`가 담당하고, 응모 유스케이스 실행은 `Application Service`가 담당한다.

현재 구현 범위는 출석체크 하나이므로, `entry / draw / reward`를 공통 인터페이스로 먼저 잘게 분리하기보다 `AttendanceProcessor` 같은 출석체크 전용 처리 컴포넌트로 묶는 쪽을 권장한다.
공통 `EventPolicy`, `DrawPolicy`, `RewardProcessor` 추상화는 두 번째 이벤트 구현이 실제로 들어오는 시점에 검토해도 늦지 않다.

조회 유스케이스에서도 같은 원칙을 따른다.

- `Application Service`가 `Event`, `EventRound`, `Prize`, 확률 정책 같은 조회 데이터를 모은다.
- DTO는 이미 준비된 값을 받아 조립한다.
- DTO 내부에서 `event.getEventRounds()` 같은 JPA 연관 컬렉션을 직접 순회하는 방식은 권장하지 않는다.
- 현재 기준에서는 QueryDSL로 `Event`, `EventRound`, `EventWin`, `Prize`를 각각 조회해 조립한다.

예:

```java
List<EventRoundDto> rounds = ...;
EventDetailDto result = EventDetailDto.of(event, rounds);
```

## 13. Repository 구조

Repository는 Hexagonal 구조를 따른다.

예:

- `EventQueryPort`
- `EventQueryImpl`
- `EventJpaRepository`

구조:

```text
EventQueryPort
    -> EventQueryImpl
        -> EventJpaRepository
```

원칙:

- Repository Port는 `application.port.output`에 둔다.
- Repository Impl은 `infrastructure.persistence`에 둔다.
- QueryDSL은 Impl 내부에서 사용한다.
- 저장은 JPA Entity 기반으로 처리하고, 조회는 QueryDSL 기반 개별 조회 포트를 조합하는 방식을 우선 사용한다.

## 14. QueryDSL 사용 규칙

QueryDSL은 `Infrastructure` 계층에서만 사용한다.

원칙:

- `Application` / `Domain` 계층에서는 QueryDSL을 사용하지 않는다.

사용 목적:

- 검색
- 필터링
- 페이징
- 관리자 조회

보조 원칙:

- 상태 변경 유스케이스는 JPA Entity를 기준으로 처리한다.
- 상세 응답 조립은 QueryDSL로 필요한 데이터를 각각 조회한 뒤 `Application Service`에서 DTO로 조립한다.

## 15. Condition Builder

검색 조건은 Builder로 분리한다.

예:

- `EventEntityBuilder`

책임:

- 검색 조건 생성
- `BooleanBuilder` 생성

목적:

- QueryDSL 조건 분리
- Repository 복잡도 감소

권장 규칙:

- 클래스명은 엔티티 기준 검색 조건을 드러내도록 `EventEntityBuilder` 형태를 사용한다.
- `impl/*Impl`은 builder가 만든 조건을 받아 QueryDSL 쿼리를 직접 구성한다.
- `EventSearchCondition.eventType`은 `String`보다 `EventType` enum 사용을 권장한다.
- 조회 DTO 조립은 QueryDSL builder가 아니라 `Application Service` 또는 assembler가 담당한다.

## 16. EventEntry 구조

`EventEntry`는 응모 기록을 저장한다.

주요 컬럼:

- `event_id`
- `applicant_id`
- `member_id`
- `applied_at`

출석 이벤트의 중복 참여 방지는 `event_applicant` unique와 application validation을 함께 사용한다.

예:

```text
UNIQUE(round_id, member_id)
```

## 17. 동시성 제어

이벤트 시스템의 주요 경쟁 상황:

- 중복 응모
- 경품 초과 지급
- 동시 당첨

해결 기준:

1. DB Unique Constraint
2. Exists query
3. Domain Service 검증

## 18. 시간 타입

DB 타입:

- `TIMESTAMP`

Java 타입 추천:

- 현재 구현은 `Instant`를 사용한다.
- DB/JDBC 타임존은 `Asia/Seoul`로 맞춘다.

이벤트 시스템은 한국 시간(`Asia/Seoul`) 기준 운영을 기본으로 한다.

## 19. Entity 설계 원칙

Entity는 persistence model이다.

원칙:

- DB 상태 보관
- JPA 매핑
- 연관 관계 관리
- 단일 `event` 테이블을 기준으로 공통 속성만 보관

주의:

- Entity에 도메인 정책을 넣지 않는다.
- 도메인 정책은 `Policy` 또는 `DomainService`에서 처리한다.
- 이벤트 타입별 행위 차이는 JPA 상속이 아니라 `Policy` 구현 분리로 해결한다.

## 20. Aggregate Boundary

이 프로젝트의 주요 Aggregate는 아래를 기준으로 본다.

| Aggregate | 설명 |
| --- | --- |
| `Event` | 이벤트 기본 정보 및 상태 |
| `EventRound` | 이벤트 회차 정보 |
| `EventEntry` | 사용자 응모 기록 |
| `EventWin` | 당첨 기록 |
| `Prize` | 경품 정보 |

원칙:

- 각 Aggregate는 독립적인 변경 단위를 가진다.
- Aggregate 간 직접 객체 참조보다 ID 참조를 기본으로 한다.
- 상태 변경은 `Application Service`를 통해 수행한다.
- 하나의 트랜잭션은 특정 Aggregate를 중심으로 설계하되, 현재 출석 성공 유스케이스처럼 `EventEntry` 생성과 `EventWin` 기록이 함께 커밋되는 실무형 예외는 허용한다.

예:

```text
EventEntry
 ├ eventId
 ├ roundId
 └ memberId
```

`EventEntry`는 `Event` 객체를 직접 참조하지 않고 `eventId`로 관계를 표현한다.

효과:

- Aggregate 간 결합도 감소
- 트랜잭션 경계 명확화
- 확장성 확보

## 21. 아키텍처 핵심 요약

Event 플랫폼은 아래 구조를 따른다.

```text
Controller
    -> ApplicationService
        -> DomainService
        -> EventPolicy
        -> RepositoryPort
            -> *Impl
                -> EventEntity
```

책임 정리:

- `EventEntity`: persistence model
- `EventPolicy`: 이벤트 타입별 정책
- `DomainService`: 도메인 규칙 판단
- `ApplicationService`: 유스케이스 실행
- `*Impl`: DB 접근

## 22. 설계 핵심 원칙

1. Event 테이블은 하나로 유지한다.
2. `EventType`에 따라 `Policy`를 분리한다.
3. `DomainService`는 도메인 규칙을 담당한다.
4. `ApplicationService`는 흐름을 담당한다.
5. QueryDSL은 `Infrastructure`에서만 사용한다.
6. Repository는 Port / Adapter 구조를 따른다.
7. 이벤트 타입 확장은 JPA 상속보다 `EventEntity + Policy` 전략을 우선한다.

## 23. 확장 전략

새 이벤트 유형 추가 시:

1. `EventType` 추가
2. `Policy` 구현 추가
3. `Factory` 매핑 추가

즉, DB 구조 변경 없이 이벤트 타입을 확장할 수 있다.
