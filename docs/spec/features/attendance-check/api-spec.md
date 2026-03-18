# API Spec

이 문서는 현재 확정된 외부용 이벤트 API 계약을 정리한다. 이번 프로젝트는 외부 호출용 API만 제공하며, admin CRUD/search는 별도 admin 프로젝트에서 담당한다.

## 범위

- 외부용 API만 제공한다.
- admin 프로젝트는 현재 범위 밖이다.
- 이번 프로젝트에서 노출하는 API는 총 2개다.
- 다만 구현체 내부에는 `event`, `prize` 관련 Entity, DTO, QueryDSL 구성요소를 둔다.

## 공통 계약

### BaseResponse

모든 응답은 아래 공통 형태를 따른다.

- 성공 응답의 `code`는 `SUCCESS`를 사용한다.
- 실패 응답의 `code`는 각 오류의 실제 code를 사용한다.
- Business 예외는 domain code를 그대로 반환한다.

```json
{
  "code": "STRING_CODE",
  "message": "사용자 메시지",
  "timestamp": "2026-02-02T10:00:00Z",
  "data": {}
}
```

### Header

- `X-Member-Id: {memberId}`
- 현재는 신뢰된 호출 주체가 호출한다는 전제로 `X-Member-Id`를 우선 사용한다.
- JWT, Spring Security는 현재 범위에 포함하지 않는다.
- `X-Member-Id`는 interceptor가 아니라 controller `@RequestHeader`로 처리한다.

### memberId

- 이벤트 플랫폼은 자체 회원을 가지지 않는다.
- 현재는 위드가 보유한 `memberId`를 사용한다.
- 이후 돌쇠네 쇼핑몰이 완성되면 해당 회원 체계로 연결될 예정이다.
- 버터 외주사는 위드의 회원 데이터를 마이그레이션할 예정이다.

### supplierId

- `supplierId`는 이벤트의 외부 값참조 식별자다.
- 현재는 위드 DB 기준 값을 사용한다.
- 이후 버터가 돌쇠네 쇼핑몰 개발을 완료하면 위드 데이터를 버터 DB로 마이그레이션해 사용할 예정이다.
- 현재 `supplierId`는 돌쇠네 자체 서비스 범위에만 적용한다.

### 환경별 URL

#### 이벤트 응모

- `https://event-api.dolfarmer.com/event/v1/events/{eventId}/rounds/{roundId}/entries`
- `https://dev-event-api.dolfarmer.com/event/v1/events/{eventId}/rounds/{roundId}/entries`
- `https://stg-event-api.dolfarmer.com/event/v1/events/{eventId}/rounds/{roundId}/entries`

#### 이벤트 조회

- `https://event-api.dolfarmer.com/event/v1/events/{eventId}`
- `https://dev-event-api.dolfarmer.com/event/v1/events/{eventId}`
- `https://stg-event-api.dolfarmer.com/event/v1/events/{eventId}`

## ATT-API-001 이벤트 응모

- Method: `POST`
- Path: `/event/v1/events/{eventId}/rounds/{roundId}/entries`
- 접근 권한: 돌쇠네 쇼핑몰 서버
- Header: `X-Member-Id` 필수

### Request

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `eventId` | `Long` | Y | 이벤트 식별자 |
| `roundId` | `Long` | Y | 회차 식별자 |

Request Body는 없다.

Controller 권장 시그니처:

```java
@PostMapping("/event/v1/events/{eventId}/rounds/{roundId}/entries")
public BaseResponse<EventEntryResponse> enterEvent(
        @PathVariable Long eventId,
        @PathVariable Long roundId,
        @RequestHeader("X-Member-Id") Long memberId
) {
    ...
}
```

### 동작 규칙

- `event_applicant`는 사전 참여 가능 대상자 테이블이 아니라 회차별 applicant 기준 테이블이다.
- `event_applicant`는 `(round_id, member_id)` unique로 동작한다.
- `event_applicant`는 별도 관리 API 없이 출석 요청 시 `eventId + roundId + memberId` 기준으로 생성한다.
- 회원별 사전 참여 가능 대상 체크는 하지 않고, applicant insert와 unique 충돌로 중복 출석을 제어한다.
- `event_round.event_id`와 `event_applicant.round_id`는 FK로 보호한다.
- 서버는 요청 `eventId == round.event_id`, `event_applicant.event_id == eventId` 같은 값참조 정합성은 Service에서 검증해야 한다.
- `event_round` 조회는 `roundId` 단독이 아니라 `roundId + eventId`로 함께 조회하는 방식을 권장한다.
- `event_entry`는 실제 응모권/참여 이력 테이블이다.
- 출석 이벤트에서 `event_entry`는 `event_id`, `applicant_id`, `member_id`를 저장하고 회차는 `applicant_id -> event_applicant.round_id`로 해석한다.
- 같은 회차에도 여러 `event_entry`가 들어갈 수 있고, 추첨형 이벤트에서는 `is_winner`가 나중에 update될 수 있다.
- 출석 중복 체크의 비즈니스 기준은 `event_applicant`의 `roundId + memberId`다.
- 출석체크형 이벤트에서는 `roundId = 해당 날짜 회차`다.
- 출석체크형 이벤트는 회차당 보상 매핑이 최대 1개다.
- 회차에 보상 매핑이 있으면 `event_entry`, `event_win`까지 먼저 저장하고 로컬 트랜잭션 커밋 후 외부 point API를 호출한다.
- 회차에 보상 매핑이 없으면 외부 point API를 호출하지 않고 `win = null`로 응답한다.
- 출석체크형 이벤트는 출석 + 응모 + 로컬 당첨 결과를 한 번에 응답한다.
- 보상 매핑이 있는 회차에서 외부 point API가 실패하거나 무응답이어도, 로컬 커밋이 완료되었다면 응답은 성공으로 유지한다.
- 출석 이벤트는 즉시 보상이면 `event_entry.is_winner = true`, 무보상 출석이면 `false`로 응답한다.
- 즉시 당첨 여부는 항상 `isWinner`에 포함한다.
- 즉시 당첨이 아니면 `isWinner: null` 또는 `false`가 될 수 있으며, 이는 이벤트 유형에 따라 달라진다.

### 성공 응답

- `code`: `SUCCESS`
- `message`: 출석 이벤트는 `출석 체크가 완료되었습니다.`

#### 출석체크형 이벤트 예시

```json
{
  "code": "SUCCESS",
  "message": "출석 체크가 완료되었습니다.",
  "timestamp": "2026-02-02T10:00:00Z",
  "data": {
    "entryId": 200,
    "appliedAt": "2026-02-02T10:00:00Z",
    "roundNo": 2,
    "isWinner": true,
    "win": {
      "winId": 1,
      "prizeName": "2월 출석 체크 포인트 기본 세팅",
      "rewardType": "POINT",
      "pointAmount": 30
    },
    "attendance": {
      "attendedDays": 2,
      "totalDays": 28
    }
  }
}
```

#### 출석체크형 이벤트 무보상 예시

```json
{
  "code": "SUCCESS",
  "message": "출석 체크가 완료되었습니다.",
  "timestamp": "2026-02-03T10:00:00Z",
  "data": {
    "entryId": 201,
    "appliedAt": "2026-02-03T10:00:00Z",
    "roundNo": 3,
    "isWinner": false,
    "win": null,
    "attendance": {
      "attendedDays": 3,
      "totalDays": 28
    }
  }
}
```

### `win` 객체

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `winId` | `Long` | 당첨 식별자 |
| `prizeName` | `String` | 경품명 |
| `rewardType` | `String` | `POINT` / `COUPON` |
| `pointAmount` | `Integer` | `POINT`일 때만 포함 |
| `couponCode` | `String` | `COUPON`일 때만 선택 포함 |

### `attendance` 객체

출석 이벤트일 때만 포함한다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `attendedDays` | `Integer` | 누적 출석 일수 |
| `totalDays` | `Integer` | 이벤트 전체 일수 |

### 오류 방향

- `X-Member-Id` 누락
  - `code`: `INVALID_REQUEST`
  - `message`: `잘못된 요청입니다.`
  - `data` 예시: `{ "X-Member-Id": "X-Member-Id 헤더는 필수입니다." }`
- 이벤트 없음
  - `code`: `EVENT_NOT_FOUND`
- 회차 없음
  - `code`: `EVENT_ROUND_NOT_FOUND`
- 이벤트-회차 불일치
  - `code`: `ROUND_EVENT_MISMATCH`
- 운영 중단/급정지 이벤트
  - `code`: `EVENT_NOT_ACTIVE`
  - `message`: `현재 참여가 잠시 중단되었어요.`
- 시작 전 이벤트
  - `code`: `EVENT_NOT_STARTED`
  - `message`: `이벤트 오픈 전이에요. 조금만 기다려 주세요.`
- 참여 마감/종료 이벤트
  - `code`: `EVENT_EXPIRED`
  - `message`: `이 이벤트는 참여가 마감되었어요.`
- 이미 출석함
  - `code`: `ENTRY_ALREADY_APPLIED`
  - `message`: `이미 출석했습니다.`
- 보상 매핑이 있는 회차에서 외부 point API 실패/무응답은 로컬 응답 오류가 아니라 운영 보정 대상이다.

## ATT-API-002 이벤트 상세 및 참여 상태 조회

- Method: `GET`
- Path: `/event/v1/events/{eventId}`
- 접근 권한: 돌쇠네 쇼핑몰 서버
- Header: `X-Member-Id` 선택

### Request

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `eventId` | `Long` | Y | 이벤트 식별자 |

Controller 권장 시그니처:

```java
@GetMapping("/event/v1/events/{eventId}")
public BaseResponse<EventDetailResponse> getEvent(
        @PathVariable Long eventId,
        @RequestHeader(value = "X-Member-Id", required = false) Long memberId
) {
    ...
}
```

### 동작 규칙

- `eventType = ATTENDANCE`일 때 전체 회차 목록과 출석 상태를 반환한다.
- 페이지네이션은 없다.
- `X-Member-Id`가 있으면 해당 회원의 출석 상태를 포함한다.
- `ATTENDED / MISSED / TODAY / FUTURE` 계산 기준 시각은 한국 시간(`Asia/Seoul`)이다.
- `X-Member-Id`가 없으면 전체 회차 기본 정보만 반환한다.
- `X-Member-Id`가 없을 때는 `rounds[].status = null`, `rounds[].win = null`이다.
- GET 응답에서는 `createdAt`은 노출하지 않는다.
- GET 응답에서는 `supplierId`, `eventUrl`은 고정 노출한다.
- 출석 완료 회차라도 보상 매핑이 없었다면 `status = ATTENDED`, `win = null`일 수 있다.

### 성공 응답

- `code`: `SUCCESS`
- `message`: `이벤트를 조회했습니다.`

#### 출석 이벤트 예시

```json
{
  "code": "SUCCESS",
  "message": "이벤트를 조회했습니다.",
  "timestamp": "2026-02-09T10:00:00Z",
  "data": {
    "eventId": 1,
    "eventName": "2월 출석체크 이벤트",
    "eventType": "ATTENDANCE",
    "startAt": "2026-02-01T00:00:00Z",
    "endAt": "2026-02-28T23:59:59Z",
    "supplierId": 1,
    "eventUrl": "https://...",
    "priority": 1,
    "isActive": true,
    "isVisible": true,
    "description": "매일 출석하고 포인트를 받으세요!",
    "totalCount": 28,
    "rounds": [
      { "roundId": 1, "roundNo": 1, "roundDate": "2026-02-01", "status": "ATTENDED", "win": { "prizeName": "출석 포인트", "rewardType": "POINT", "pointAmount": 30 } },
      { "roundId": 2, "roundNo": 2, "roundDate": "2026-02-02", "status": "MISSED", "win": null },
      { "roundId": 3, "roundNo": 3, "roundDate": "2026-02-03", "status": "ATTENDED", "win": null },
      { "roundId": 4, "roundNo": 4, "roundDate": "2026-02-04", "status": "ATTENDED", "win": { "prizeName": "출석 포인트", "rewardType": "POINT", "pointAmount": 30 } },
      { "roundId": 5, "roundNo": 5, "roundDate": "2026-02-05", "status": "ATTENDED", "win": { "prizeName": "출석 포인트", "rewardType": "POINT", "pointAmount": 30 } },
      { "roundId": 6, "roundNo": 6, "roundDate": "2026-02-06", "status": "MISSED", "win": null },
      { "roundId": 7, "roundNo": 7, "roundDate": "2026-02-07", "status": "ATTENDED", "win": { "prizeName": "출석 포인트", "rewardType": "POINT", "pointAmount": 30 } },
      { "roundId": 8, "roundNo": 8, "roundDate": "2026-02-08", "status": "ATTENDED", "win": { "prizeName": "출석 포인트", "rewardType": "POINT", "pointAmount": 30 } },
      { "roundId": 9, "roundNo": 9, "roundDate": "2026-02-09", "status": "TODAY", "win": null },
      { "roundId": 10, "roundNo": 10, "roundDate": "2026-02-10", "status": "FUTURE", "win": null },
      { "comment": "...이하 동일..." },
      { "roundId": 28, "roundNo": 28, "roundDate": "2026-02-28", "status": "FUTURE", "win": null }
    ],
    "attendanceSummary": {
      "attendedDays": 7,
      "totalDays": 28
    }
  }
}
```

### `rounds[].status`

| status | 조건 | 설명 |
| --- | --- | --- |
| `ATTENDED` | 과거 날짜 + 출석 완료 | 출석 완료 |
| `MISSED` | 과거 날짜 + 미출석 | 출석 누락 |
| `TODAY` | 오늘 날짜 | 현재 진행 중인 회차 |
| `FUTURE` | 오늘 이후 날짜 | 잠김 |

상태 계산의 오늘/과거/미래 기준은 한국 시간(`Asia/Seoul`)이다.

## 미래 참고 예시

아래 예시는 지금 당장 개발 범위는 아니지만, 이후 확장 시 참고하는 계약 예시다.

- 랜덤 리워드에서는 하나의 회차에 여러 `event_round_prize`를 두고, 각 보상별 확률 정책으로 실제 지급 대상을 계산할 수 있다.

### 즉시 당첨형 이벤트 당첨 예시

```json
{
  "code": "SUCCESS",
  "message": "응모가 완료되었습니다.",
  "timestamp": "2026-03-09T08:00:00Z",
  "data": {
    "entryId": 200,
    "appliedAt": "2026-03-09T08:00:00Z",
    "isWinner": true,
    "win": {
      "winId": 1,
      "prizeName": "스타벅스 아메리카노",
      "rewardType": "COUPON"
    }
  }
}
```

### 즉시 당첨형 이벤트 꽝 예시

```json
{
  "code": "SUCCESS",
  "message": "응모가 완료되었습니다.",
  "timestamp": "2026-03-09T08:00:00Z",
  "data": {
    "entryId": 201,
    "appliedAt": "2026-03-09T08:00:00Z",
    "isWinner": false,
    "win": null
  }
}
```

### 추첨형 이벤트 추첨 전 예시

```json
{
  "code": "SUCCESS",
  "message": "응모가 완료되었습니다.",
  "timestamp": "2026-03-09T08:00:00Z",
  "data": {
    "entryId": 200,
    "appliedAt": "2026-03-09T08:00:00Z",
    "isWinner": null,
    "win": null
  }
}
```

### 리워드 이벤트 조회 예시

```json
{
  "code": "SUCCESS",
  "message": "이벤트를 조회했습니다.",
  "timestamp": "2026-03-09T08:00:00Z",
  "data": {
    "eventId": 1,
    "eventName": "3월 경품 이벤트",
    "eventType": "RANDOM_REWARD",
    "startAt": "2026-03-01T00:00:00Z",
    "endAt": "2026-03-31T23:59:59Z",
    "supplierId": 1,
    "eventUrl": "https://...",
    "priority": 1,
    "isActive": true,
    "isVisible": true,
    "isAutoEntry": false,
    "isSnsLinked": false,
    "isDuplicateWinner": false,
    "isMultipleEntry": false,
    "isWinnerAnnounced": false,
    "description": "경품 이벤트 설명",
    "rounds": [
      {
        "roundId": 1,
        "roundNo": 1,
        "startAt": "2026-03-01T00:00:00Z",
        "endAt": "2026-03-31T23:59:59Z",
        "prizes": [
          { "prizeId": 1, "prizeName": "스타벅스 아메리카노", "rewardType": "COUPON", "quantity": 100 },
          { "prizeId": 2, "prizeName": "네이버 페이 3만원 쿠폰", "rewardType": "COUPON", "quantity": 10 }
        ]
      },
      {
        "roundId": 2,
        "roundNo": 2,
        "startAt": "2026-04-01T00:00:00Z",
        "endAt": "2026-04-30T23:59:59Z",
        "prizes": [
          { "prizeId": 3, "prizeName": "스타복 기프티콘 5만원", "rewardType": "COUPON", "quantity": 50 }
        ]
      }
    ]
  }
}
```

## 확정된 API 방향

- 외부 프로젝트는 2개 API만 제공한다.
- admin CRUD/search는 별도 프로젝트가 담당한다.
- `POST /entries`는 `X-Member-Id` 필수다.
- `GET /events/{eventId}`는 `X-Member-Id` 선택이다.
- `GET`에서 헤더가 없으면 `status = null`, `win = null`을 반환한다.

## API 기준 스키마 정합화 포인트

- 신규 환경용 schema draft는 `promotion` 스키마와 최소 FK를 반영한다.
- 신규 환경용 schema draft는 `event_applicant (round_id, member_id)` unique를 반영한다.
- 신규 환경용 schema draft는 `event_entry.event_id`를 유지하고 `round_id`는 제거하며, 회차는 `applicant_id -> event_applicant.round_id`로 파생한다.
  - 출석 이벤트에서 회차는 applicant 조인으로 복원할 수 있다.
  - 제공된 원본 DDL에는 `event_id` 컬럼이 없었고, 이번 정리에서는 `round_id`는 다시 제거했다.
- 신규 환경용 schema draft는 최소 unique로 `uq_event_round_event_round_no`, `uq_event_applicant_round_member_id`, `uq_event_win_entry_id`를 유지한다.
- `GET /events/{eventId}`는 회차별 출석 상태와 win 정보를 반환해야 한다.
  - 따라서 `event_entry`, `event_win`, `event_round` 연결 전략이 스키마와 조회 쿼리에 함께 반영되어야 한다.
