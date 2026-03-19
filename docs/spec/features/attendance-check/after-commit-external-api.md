# After Commit External API

이 문서는 출석체크에서 외부 point API를 왜 로컬 DB 트랜잭션과 분리해야 하는지, 그리고 Spring에서 어떤 구조로 구현할지 정리한다.

## 목적

- 로컬 DB commit 성공 이후에만 외부 point API가 호출되도록 구조를 고정한다.
- 외부 API 호출 책임을 출석 트랜잭션 서비스에서 분리한다.
- point API 실패가 로컬 rollback 이슈가 아니라 운영 후처리 이슈라는 점을 코드 구조로도 드러낸다.

## 왜 필요한가

출석체크의 로컬 성공 기준은 `event_applicant`, `event_entry`, `event_win` 저장과 commit 성공이다.

문제는 외부 point API를 트랜잭션 안에서 호출하면 아래 같은 불일치가 생길 수 있다는 점이다.

```text
1. DB save
2. point API 성공
3. DB commit 실패
```

결과:

- 포인트는 이미 지급됨
- 로컬 DB는 롤백됨

즉, 외부 시스템과 로컬 DB가 서로 다른 상태가 된다.

그래서 현재 구조에서는 "로컬 트랜잭션 안에서는 저장만 수행하고, 외부 API 호출은 commit 이후 단계로 분리한다"를 원칙으로 삼는다.

## 현재 권장 구조

### 문제 가능 구조

```java
@Transactional
public void attend() {
    save();
    callPointApi();
}
```

### 권장 구조

```java
@Transactional
public AttendEventResult attend(...) {
    save();
    eventPublisher.publishEvent(pointGrantCommand);
    return result;
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handle(PointGrantCommand command) {
    callPointApi();
}
```

핵심:

- `publishEvent(...)`는 트랜잭션 안에서 호출한다.
- `@TransactionalEventListener(AFTER_COMMIT)`는 commit 성공 시점에만 실행된다.
- rollback 되면 listener는 실행되지 않는다.
- listener 안에서 point API 실패를 잡아 로그와 운영 알림으로 전환하면, 사용자 응답은 로컬 성공 기준을 그대로 유지할 수 있다.

## 현재 attendance 적용 기준

출석체크에서는 아래 구조를 권장한다.

```text
AttendEventService
  -> AttendEventTransactionService (@Transactional)
      -> event / round / applicant / entry / win 저장
      -> PointGrantCommand 생성
      -> publishEvent(PointGrantCommand)
  -> return AttendEventResult

PointRewardAfterCommitListener
  -> @TransactionalEventListener(AFTER_COMMIT)
  -> PointRewardPort.grant(command)
  -> PointRewardImpl (Feign adapter)
  -> 실패 시 ERROR 로그 + PointRewardFailureAlertPort
```

정리:

- 출석 응답 성공 기준은 로컬 commit 성공이다.
- 외부 point API는 after-commit listener가 담당한다.
- point API 실패는 타임아웃을 포함한 외부 연동 실패로 보고, 사용자 오류 응답으로 바꾸지 않는다.
- 무보상 출석이면 event 자체를 발행하지 않는다.

## Spring 구현 기준

### 1. 이벤트 발행

- `ApplicationEventPublisher`를 `AttendEventTransactionService`에 주입한다.
- 보상 매핑이 있을 때만 `PointGrantCommand`를 만들어 발행한다.

### 2. after-commit listener

- `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`를 사용한다.
- listener는 point API 호출, 로그, 운영 알림을 담당한다.
- 외부 예외는 listener 내부에서 catch 하고 다시 던지지 않는다.

### 3. 비동기 여부

- 기본은 동기 listener로 둔다.
- 필요하면 나중에 `@Async`를 붙여 비동기화할 수 있다.
- 다만 지금은 운영 로그와 실패 알림을 같은 요청 흐름 직후 남기는 쪽이 이해하기 쉬우므로 동기 구성을 우선한다.

## 언제 이 패턴을 쓰나

아래처럼 "로컬 트랜잭션과 외부 시스템 부작용을 분리해야 하는 경우"에 사용한다.

- 외부 API 호출
- 메시지 발행(Kafka, SQS 등)
- 알림 발송(SMS, 이메일)

## 구현 메모

- `PointGrantCommand`는 외부 point API 호출 계약이면서, 현재 구조에서는 after-commit event payload 역할도 함께 맡는다.
- 로컬 트랜잭션 서비스는 외부 포트를 직접 호출하지 않는다.
- 외부 연동 실패 후 재처리가 필요해지면 같은 `idempotency_key`를 사용한다.
