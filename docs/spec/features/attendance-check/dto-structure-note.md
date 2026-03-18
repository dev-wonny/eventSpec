# Attendance DTO Structure Note

이 문서는 출석체크 기능을 작업하면서 DTO를 어떻게 나눴는지 쉽게 설명하기 위한 메모다.

중요:

- 이 문서는 현재 `attendance-check` 기능 기준 정리다.
- 프로젝트 전체 공용 규약으로 강제하려는 문서는 아니다.
- 팀 공통 규칙이 바뀌면 이 문서도 같이 바뀔 수 있다.

## 1. 왜 이 문서를 남기나

작업하면서 아래 질문이 계속 나왔다.

- `application` DTO는 어디까지가 맞을까
- `Entity`와 비슷하게 만들어야 할까
- `presentation response`에 맞춘 DTO가 `application`에 들어가도 될까
- 서비스 내부에서만 쓰는 값도 DTO로 빼야 할까

이번 출석체크 기능에서는 이 기준을 한 번 정리해 두는 것이 유지보수에 더 도움이 된다고 판단했다.

## 2. 가장 쉬운 기준

DTO를 볼 때는 "모양이 비슷한가"보다 "어느 경계를 넘는가"를 먼저 본다.

정리:

- `Entity`: DB에 저장되는 모델
- `Command/Query DTO`: 유스케이스 입력
- `Result DTO`: 유스케이스 출력
- `Internal DTO`: 서비스 내부 조립용
- `External DTO`: 외부 시스템 연동용
- `Response DTO`: 최종 HTTP 응답용

한 줄로 줄이면 아래처럼 이해하면 된다.

- DB에 저장할 모양이면 `Entity`
- 서비스에 일을 시킬 입력이면 `Command` 또는 `Query`
- 서비스가 일을 끝내고 돌려줄 결과면 `Result`
- 서비스 내부에서 잠깐 묶어 쓰는 값이면 `Internal`
- 외부 API/알림/연동에 넘길 값이면 `External`
- 클라이언트에게 응답으로 보여줄 값이면 `Response`

## 3. Controller, Service, Response 기본 흐름

이번 출석체크에서는 아래 흐름을 기본으로 본다.

```text
Controller -> Command DTO -> Service
Service -> Result DTO -> Controller
Controller -> Response DTO
```

조금 더 실제 코드 흐름에 맞춰 쓰면 아래와 같다.

```text
Controller
  -> AttendEventCommand
  -> AttendEventUseCase
  -> AttendEventResult
  -> EventEntryResponse
  -> BaseResponse
```

이 구조를 잡는 이유는 간단하다.

- Controller는 HTTP 입력을 그대로 서비스 전반에 퍼뜨리지 않는다.
- Service는 유스케이스 실행 결과를 application 관점의 결과 DTO로 돌려준다.
- Controller는 그 결과를 외부 계약에 맞는 response DTO로 바꾼다.

즉, `Command`, `Result`, `Response`는 이름만 다른 것이 아니라 경계가 다르다.

조회성 유스케이스는 `Command` 대신 `Query`라는 이름을 쓸 수 있다.

예:

- `POST /entries` -> `AttendEventCommand`
- `GET /events/{eventId}` -> `GetEventDetailQuery`

둘 다 역할은 같다.

- Controller가 HTTP 입력을 application 입력 DTO로 묶어 전달한다.

차이는 의도 표현이다.

- `Command`: 상태 변경/행위 실행
- `Query`: 조회

## 4. 왜 Controller에서 Command DTO로 넘기나

Controller -> Service 공개 경계에서는 원시 파라미터 나열보다 `Command DTO` 또는 `Query DTO`를 우선한다.

예:

```java
attendEventUseCase.attend(AttendEventCommand.of(eventId, roundId, memberId))
```

이렇게 두는 이유:

- 파라미터가 늘어나도 메서드 시그니처가 덜 흔들린다.
- 값의 의미가 이름으로 드러난다.
- 순서 실수를 줄일 수 있다.
- HTTP 입력과 유스케이스 입력을 분리할 수 있다.

## 5. 피하려는 방식

### 5.1 원시 파라미터 나열

```java
service.attend(eventId, roundId, memberId);
```

문제:

- 파라미터가 늘어나면 메서드가 쉽게 지저분해진다.
- 같은 타입이 반복되면 순서에 의존하게 된다.
- 호출부만 보고 각 값의 의미를 한 번에 읽기 어렵다.

### 5.2 Entity 전달

```java
service.attend(eventEntity);
```

문제:

- Controller가 persistence 모델에 기대게 된다.
- Service 공개 경계에 저장 모델이 새기 시작한다.
- 유지보수 시 presentation과 persistence가 같이 흔들릴 가능성이 커진다.

### 5.3 Map 전달

```java
service.attend(Map.of(...));
```

문제:

- 타입 안정성이 없다.
- 키 이름 오타를 컴파일 타임에 잡지 못한다.
- 리팩터링에 약하다.

정리하면 이번 출석체크 기준에서는 아래처럼 이해하면 된다.

- Controller -> Service는 `Command DTO` 또는 `Query DTO`
- Service -> Controller는 `Result DTO`
- Controller -> 외부 응답은 `Response DTO`
- `Entity`, `Map`, 원시 파라미터 나열은 공개 유스케이스 경계에서 지양한다.

## 6. 이번 출석체크에서 실제로 나눈 기준

`src/main/java/com/event/application/dto/attendance` 하위는 현재 아래처럼 나눴다.

### `command`

유스케이스 입력이다.

예:

- `AttendEventCommand`

의미:

- Controller가 path/header 값을 받아 서비스로 넘길 때 사용한다.
- HTTP 요청 자체를 그대로 들고 가는 것이 아니라, "출석 응모 유스케이스를 실행하기 위해 필요한 값"만 가진다.

### `result`

유스케이스 출력이다.

예:

- `AttendEventResult`
- `AttendanceWinDto`
- `AttendanceSummaryDto`

의미:

- 서비스가 출석 응모를 끝낸 뒤 Controller 쪽으로 돌려주는 값이다.
- 아직 최종 HTTP 응답은 아니지만, 호출자가 이해할 수 있는 결과 형태다.

주의:

- 지금 구조에서는 `result`가 `presentation response`와 꽤 비슷하다.
- 이것은 "완전히 잘못됐다"기보다, 현재 유스케이스 결과가 외부 응답과 많이 닮아 있다는 뜻에 가깝다.

### `internal`

서비스 내부 조립용이다.

예:

- `AttendanceRewardInfo`
- `AttendEventTransactionResult`

의미:

- Service 내부 흐름을 정리하려고 만든 값 묶음이다.
- Controller나 외부 시스템이 직접 알 필요는 없다.
- 유스케이스 계약보다는 오케스트레이션 편의를 위한 DTO다.

### `external`

외부 시스템 연동용이다.

예:

- `PointGrantCommand`
- `PointGrantResult`

의미:

- point API 같은 외부 포트와 데이터를 주고받기 위한 DTO다.
- DB Entity나 HTTP 응답 형태와 맞출 필요가 없다.
- 외부 시스템이 요구하는 계약에 맞추는 것이 우선이다.

## 7. 왜 Entity와 똑같은 DTO로 안 갔나

가장 많이 헷갈리는 지점은 여기다.

결론:

- `application` DTO는 `Entity`와 닮아야 하는 것이 아니다.
- `application` DTO는 유스케이스에 맞아야 한다.

예를 들어 `EventEntryEntity`는 저장을 위한 값들을 가진다.

- `applicantId`
- `eventId`
- `memberId`
- `eventRoundPrizeId`
- `isWinner`

반면 출석 응모 결과는 호출자가 알고 싶은 값이 다르다.

- `entryId`
- `appliedAt`
- `roundNo`
- `win`
- `attendance`

즉, 저장 모델과 유스케이스 결과 모델은 자연스럽게 다를 수 있다.

그래서 이번 구조에서는:

- 저장은 `Entity`
- 유스케이스 결과는 `result DTO`
- 최종 응답은 `presentation response DTO`

이렇게 역할을 나눴다.

## 8. 그럼 Entity와 거의 같은 DTO는 언제 쓰나

완전히 같거나 아주 비슷한 DTO는 보통 아래 상황에서만 쓴다.

- QueryDSL/JDBC 조회 projection
- 캐시 저장용 snapshot
- 외부 이벤트/Kafka payload
- 특정 어댑터에서 Entity를 직접 밖으로 노출하고 싶지 않을 때

즉, "Entity랑 비슷한 DTO를 만들어야 한다"가 기본 원칙은 아니다.

오히려 아래를 먼저 의심하는 편이 낫다.

- 정말 다른 경계를 넘는가
- 아니면 그냥 `Entity`나 기존 `Result DTO`면 충분한가

## 9. point API 연동은 별도 흐름으로 본다.

```text
AttendEventTransactionService
  -> PointGrantCommand
  -> PointRewardPort
  -> PointGrantResult
```

## 10. 지금 구조를 볼 때 읽는 법

패키지만 봐도 대충 의미를 알 수 있게 의도했다.

- `attendance.command`: 서비스 입력
- `attendance.result`: 서비스 결과
- `attendance.internal`: 서비스 내부 보조값
- `attendance.external`: 외부 연동 계약

즉, DTO 이름만 보지 말고 패키지까지 같이 보면 된다.

## 11. 다음에 DTO를 추가할 때 체크할 질문

새 DTO를 만들 때는 아래 순서대로 보면 된다.

1. 이 값은 누가 받는가
2. 이 값은 DB 저장용인가, 유스케이스 입출력인가, 내부 조립용인가, 외부 연동용인가
3. 기존 DTO를 재사용해도 의미가 맞는가
4. 이름보다 패키지가 역할을 더 잘 설명해 주는가
5. 굳이 새 DTO가 필요 없는 건 아닌가

## 12. 이번 정리에서 기대한 효과

- `application/dto/attendance` 내부 역할 구분이 눈에 들어온다.
- `service result`와 `service internal`이 덜 섞인다.
- point API 연동 DTO가 별도 경계로 보인다.
- 이후 DTO를 추가할 때 "왜 여기 두는지" 설명하기 쉬워진다.
