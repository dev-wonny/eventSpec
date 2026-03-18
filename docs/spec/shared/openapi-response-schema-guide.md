# OpenAPI Response Schema Guide

이 문서는 `presentation.dto.response` 하위 응답 DTO에 적용하는 OpenAPI `@Schema` 작성 기준을 정리한다. 목표는 Swagger UI에서 응답 구조를 필드명만으로 추측하지 않도록 하고, null 가능성까지 문서에서 바로 이해할 수 있게 만드는 것이다.

## 1. 목적

- 외부 API 응답 필드의 의미를 Swagger UI에서 바로 이해할 수 있게 한다.
- 프론트엔드와 연동 시 필드 해석 비용을 줄인다.
- nullable 필드의 조건을 문서에 남겨 오해를 줄인다.
- 응답 DTO 리팩터링 이후에도 문서 품질을 코드와 함께 유지한다.

## 2. 적용 대상

현재 기준 적용 대상은 `src/main/java/com/event/presentation/dto/response` 하위 응답 DTO다.

- `BaseResponse`
- `EventDetailResponse`
- `EventRoundResponse`
- `EventRoundWinResponse`
- `EventEntryResponse`
- `EventEntryWinResponse`
- `AttendanceSummaryResponse`

원칙:

- 외부 API 응답으로 직접 노출되는 DTO는 타입 레벨 `@Schema`를 붙인다.
- 응답 필드는 레코드 컴포넌트 단위로 `@Schema`를 붙인다.
- 중첩 객체 필드도 별도 DTO로 분리되어 있으면 그 DTO 자체에 다시 `@Schema`를 붙인다.

## 3. 작성 원칙

### 타입 설명

- DTO의 역할을 한 문장으로 설명한다.
- 예: `출석 이벤트 상세 조회 응답`
- 예: `출석 요약 응답`

### 필드 설명

- 필드명 번역이 아니라 API 소비자가 알아야 할 의미를 적는다.
- 지나치게 장황하게 쓰지 않는다.
- 내부 구현 용어보다 외부 계약 기준 표현을 우선한다.

좋은 예:

- `@Schema(description = "당첨 여부")`
- `@Schema(description = "출석 요약 정보. 회원 식별 헤더가 없으면 null")`
- `@Schema(description = "회차 당첨 정보. 미당첨 시 null")`

피해야 할 예:

- `@Schema(description = "isWinner")`
- `@Schema(description = "Boolean 값")`
- `@Schema(description = "당첨 여부를 나타내는 필드")`

### null 가능성 설명

응답에서 null이 의미를 가지는 필드는 설명에 조건을 함께 적는다.

예:

- 회원 헤더가 없어서 계산하지 못하는 경우
- 미당첨이라 값이 없는 경우
- 특정 보상 타입이 아니어서 값이 없는 경우

예시 문구:

- `회원 식별 헤더가 없으면 null`
- `미당첨 시 null`
- `쿠폰 보상이 아니면 null`

### 컬렉션 필드 설명

- 리스트는 내부 요소 타입 이름을 반복하기보다 목록의 의미를 설명한다.
- 예: `이벤트 회차 목록`

### enum 필드 설명

- enum 이름 자체보다 비즈니스 의미를 설명한다.
- 필요한 경우 Swagger 화면에서 값 후보를 추가로 이해할 수 있도록 enum 타입명을 유지한다.

## 4. Record 기준 작성 방식

이 프로젝트의 응답 DTO는 주로 `record`를 사용하므로, `@Schema`는 필드 선언이 아니라 레코드 컴포넌트에 붙인다.

예:

```java
@Schema(description = "출석 이벤트 응모 응답")
@Builder
public record EventEntryResponse(
        @Schema(description = "응모 ID")
        Long entryId,

        @Schema(description = "응모 시각")
        Instant appliedAt,

        @Schema(description = "당첨 여부")
        Boolean isWinner,

        @Schema(description = "당첨 정보. 미당첨 시 null")
        EventEntryWinResponse win
) {
}
```

## 5. BaseResponse 작성 기준

`BaseResponse`는 모든 API의 공통 envelope이므로 아래 필드는 항상 설명을 유지한다.

- `code`: 응답 코드
- `message`: 응답 메시지
- `timestamp`: 응답 시각
- `data`: 응답 데이터

이 문구는 기능별 응답 DTO 설명보다 더 일반적인 표현을 사용한다.

## 6. attendance-check 현재 반영 기준

출석체크 기능에서 현재 반영한 응답 문서화 포인트는 아래와 같다.

- `EventDetailResponse.attendanceSummary`: `X-Member-Id`가 없으면 null 가능
- `EventRoundResponse.status`: 회원 식별 헤더가 없으면 null 가능
- `EventRoundResponse.win`: 회차 보상이 없거나 당첨 정보가 없으면 null 가능
- `EventEntryResponse.win`: 미당첨 시 null 가능
- `EventEntryWinResponse.couponCode`: 쿠폰 보상이 아니면 null 가능

즉, Swagger UI에서 단순 타입 정보만이 아니라 응답 해석 규칙도 같이 드러나야 한다.

## 7. 유지보수 체크리스트

응답 DTO를 수정할 때 아래를 함께 확인한다.

1. 새 필드가 외부 API 계약에 노출되는가
2. 타입 레벨 또는 필드 레벨 `@Schema`가 빠지지 않았는가
3. null 가능성이 생겼다면 설명에 그 조건이 반영되었는가
4. 기능 문서의 API 예시와 충돌하지 않는가
5. 중첩 DTO를 분리했다면 새 DTO에도 `@Schema`가 붙었는가

## 8. 관련 문서

- `docs/spec/shared/code-package-response-spec.md`
- `docs/spec/features/attendance-check/api-spec.md`

