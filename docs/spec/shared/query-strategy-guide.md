# Query Strategy Guide

이 문서는 이벤트 서비스에서 `JpaRepository`와 QueryDSL을 어떤 기준으로 나눠 사용할지 정의한다.

## 목적

- 단순 조회는 Spring Data JPA 파생 메서드로 빠르게 구현한다.
- 검색, 조인, 동적 조건이 필요한 조회만 QueryDSL로 분리한다.
- 저장소 구조만 봐도 어떤 조회가 왜 QueryDSL인지 바로 이해할 수 있게 한다.

## 기본 원칙

### 1. 기본값은 `JpaRepository`

아래 조건에 해당하면 `JpaRepository` 파생 메서드를 우선 사용한다.

- 단건 조회
- 고정 조건 조회
- 단순 존재 여부 확인
- 단순 count 조회
- 단일 테이블 기준 정렬 조회
- soft delete 제외 같은 정적 조건을 이름으로 표현 가능한 경우

예:

- `findByIdAndIsDeletedFalse`
- `findByEventIdAndMemberIdAndIsDeletedFalse`
- `countByEventIdAndIsDeletedFalse`

### 2. QueryDSL은 검색성 조회에만 사용한다

아래 조건 중 하나라도 해당하면 QueryDSL 사용을 권장한다.

- 요청값에 따라 where 조건이 달라지는 경우
- 조인이 필요한 경우
- projection 전용 조회가 필요한 경우
- 집계와 검색 조건이 함께 필요한 경우
- 페이지 검색에서 동적 조건을 조합해야 하는 경우

대표 예:

- `Page<EventEntity> findAll(Pageable pageable, EventSearchCondition condition)`

### 3. QueryDSL은 `impl/*Impl`에서 직접 사용한다

검색성 조회는 `repository` 하위에 Custom Repository를 추가하지 않고, Output Port 구현체인 `impl/*Impl` 안에서 직접 구현한다.

권장 구조:

```text
EventQueryPort
    -> EventQueryImpl
        -> EventJpaRepository
        -> EventEntityBuilder
        -> JPAQueryFactory
```

이 구조를 사용하면:

- 단순 조회는 `EventQueryImpl -> EventJpaRepository`로 끝난다.
- 검색 조회는 `EventQueryImpl -> EventEntityBuilder + JPAQueryFactory`로 분기된다.
- `repository` 패키지에는 `*JpaRepository.java`만 유지된다.

## 구현 기준

### 정적 조회 구현 위치

- 위치: `infrastructure.persistence.database.repository`
- 방식: Spring Data JPA 파생 메서드

예:

```java
public interface EventJpaRepository extends JpaRepository<EventEntity, Long> {
}
```

### 검색 조회 구현 위치

- 위치: `infrastructure.persistence.database.impl/*Impl`
- 방식: QueryDSL + `EventEntityBuilder` + `JPAQueryFactory`

예:

```java
@RequiredArgsConstructor
public class EventQueryImpl implements EventQueryPort {

    private final EventJpaRepository eventJpaRepository;
    private final JPAQueryFactory queryFactory;
    private final EventEntityBuilder eventEntityBuilder;

    @Override
    public Page<EventEntity> findAll(Pageable pageable, EventSearchCondition condition) {
        BooleanBuilder where = eventEntityBuilder.buildWhere(condition);

        List<EventEntity> content = queryFactory
                .selectFrom(eventEntity)
                .where(where)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(eventEntity.priority.asc(), eventEntity.createdAt.desc())
                .fetch();

        Long total = queryFactory
                .select(eventEntity.count())
                .from(eventEntity)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, Objects.nonNull(total) ? total : 0L);
    }
}
```

## Builder 사용 규칙

- 검색 조건 조합은 `builder` 패키지에서 관리한다.
- `impl/*Impl`은 조건 해석을 직접 하지 않고 `EventEntityBuilder` 같은 builder에 위임한다.
- `null` condition이 들어와도 빈 검색으로 안전하게 동작해야 한다.
- soft delete 제외 같은 공통 필터는 builder 또는 QueryDSL where 절에서 항상 보장한다.

## 페이징 구현 규칙

- 페이지 검색은 `Pageable`을 받는다.
- 기본 정렬이 정해져 있으면 QueryDSL 쿼리에 명시한다.
- total count는 별도 count 쿼리로 조회한다.
- 정렬 허용 범위를 열어야 할 때는 `pageable.getSort()`를 그대로 신뢰하지 말고 허용 필드만 매핑한다.
- `fetchJoin`은 사용하지 않고 필요한 엔티티와 데이터를 각각 조회해 application 계층에서 조립한다.

## 현재 프로젝트 적용 범위

- 현재 외부 공개 API는 출석체크 API 2개만 범위에 포함한다.
- 이벤트 검색 persistence 패턴은 향후 admin/운영 조회 또는 내부 확장용으로 미리 준비한다.
- 외부 API 노출 여부와 무관하게 조회 전략은 이 문서를 기준으로 통일한다.

## 체크리스트

- 단순 조회인데 QueryDSL을 쓰고 있지 않은가
- 검색 조건이 동적인데 파생 메서드 이름으로 억지 구현하고 있지 않은가
- `repository` 하위에 `*JpaRepository.java` 외의 파일이 생기지 않았는가
- `impl/*Impl`이 `JpaRepository + Builder + JPAQueryFactory` 구조를 따르고 있는가
- builder가 조건 해석을 담당하고 있는가
- soft delete 제외 규칙이 빠지지 않았는가
- 페이지 검색의 count 쿼리가 누락되지 않았는가
