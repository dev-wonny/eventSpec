# Open Questions

구현 전에 확정해야 하지만 아직 입력물이 없거나, 현재 DDL과 업무 의미 사이에 차이가 있는 항목을 정리한다.

## 현재까지 확정된 항목

- [x] 출석 단위는 `event_round` 기준의 일자성 회차다.
- [x] 월간 출석 이벤트는 같은 `event_id` 아래 날짜 수만큼 `event_round`를 가진다.
- [x] `event_applicant`는 사전 참여 가능 대상자 테이블이 아니라 회차별 applicant 기준 테이블로 사용된다.
- [x] `event_applicant`는 `(round_id, member_id)` unique로 동작한다.
- [x] 이번 출석체크에서는 회원별 사전 참여 가능 대상 체크를 두지 않는다.
- [x] `event_entry`는 응모권/참여 이력 테이블이며 같은 회차에도 여러 건이 저장될 수 있다.
- [x] 추첨형 이벤트에서는 `event_entry.is_winner`가 나중에 update될 수 있다.
- [x] `event_win`은 로컬 당첨/보상 확정 이력을 저장한다.
- [x] `event.supplier_id`는 현재 위드 DB 기준 값을 사용한다.
- [x] 추후 버터 DB로 위드 데이터를 마이그레이션해 `supplier_id`를 이어받는다.
- [x] 현재 `supplier_id`는 돌쇠네 자체 서비스 범위에만 적용한다.
- [x] 외부용 프로젝트는 API 2개만 제공한다.
- [x] admin CRUD/search는 별도 프로젝트 범위다.
- [x] `POST /event/v1/events/{eventId}/rounds/{roundId}/entries`를 사용한다.
- [x] `GET /event/v1/events/{eventId}`를 사용한다.
- [x] `POST`는 `X-Member-Id`가 필수다.
- [x] `GET`은 `X-Member-Id`가 선택이다.
- [x] `GET`에서 `X-Member-Id`가 없으면 `status = null`, `win = null`을 반환한다.
- [x] 출석 중복의 비즈니스 기준은 같은 `round_id + member_id` applicant 재생성 시도다.
- [x] 출석 이벤트에서 `event_entry`는 `round_id`를 직접 가지지 않고 `applicant_id -> event_applicant.round_id`로 회차를 파생한다.
- [x] 출석체크형 이벤트는 즉시 보상이므로 `event_entry.is_winner = true`로 저장한다.
- [x] 중복 출석 시 프론트에는 `이미 출석했습니다`를 노출한다.
- [x] `event_round`, `event_round_prize`, `event_round_prize_probability`, `event_applicant`에는 최소 FK를 둔다.
- [x] FK가 보호하지 않는 값참조 정합성은 Service 검증으로 보장한다.
- [x] DB에는 최소 unique만 유지한다.
- [x] 출석체크 보상은 주로 point 중심으로 설계한다.
- [x] 출석체크는 회차당 보상 매핑이 `0..1`개다.
- [x] 출석 회차에 보상 매핑이 없으면 point를 지급하지 않는다.
- [x] 랜덤 리워드는 회차당 여러 보상을 둘 수 있다.
- [x] `prize`는 운영 세팅 완료 후 수정하지 않고, 변경이 필요하면 새 `prize`를 생성한다.
- [x] 현재 출석 성공은 로컬 트랜잭션 커밋 기준이다.
- [x] 외부 point API 실패 또는 무응답 시 로컬 데이터는 롤백하지 않고 운영 알림 대상으로 남긴다.
- [x] 현재는 외부 API를 같은 요청 흐름에서 동기 호출하지만 트랜잭션 밖에서 처리한다.
- [x] 애플리케이션 로그는 이번 범위에서 ELK로 적재한다.
- [x] `GET`의 `TODAY / MISSED / FUTURE` 판정 기준 타임존은 한국 시간(`Asia/Seoul`)이다.
- [x] `is_visible = FALSE`여도 직접 API 출석은 허용한다.

## 우선 확인 항목

- [x] `event_entry.event_round_prize_id`는 보조값으로만 사용하며 `NULL` 가능하다.
- [x] 로컬 보상 확정의 SoT는 `event_win.event_round_prize_id`다.
- [x] 랜덤 리워드의 기본 계산 규칙은 `weight` 기본값 `1`을 기준으로 해석한다.
- [ ] `GET`에서 `X-Member-Id`가 없을 때 `attendanceSummary`를 생략할지, `totalDays`만 줄지
- [x] soft delete된 applicant/entry가 있더라도 재출석은 허용한다.
- [x] soft delete된 `prize`, `event_round_prize`는 현재 활성 설정 조회에서는 제외한다.
- [x] soft delete된 `prize`, `event_round_prize`도 과거 지급 이력 조회와 집계에서는 필요 시 참조할 수 있어야 한다.
- [x] `prize`와 `event_round_prize`를 함께 생성하는 흐름에서 하나라도 실패하면 함께 롤백한다.
- [x] `event_round_prize`만 삭제해도 `prize`는 함께 삭제되지 않는다.
- [x] `prize`, `event_round_prize`를 함께 삭제하려는 경우에는 두 테이블 모두 soft delete한다.
- [ ] 취소/정정 기능이 필요한가
- [ ] 감사 로그 수준은 어디까지 필요한가
- [x] `created_by`, `updated_by`는 `BIGINT`로 유지한다.
- [x] system 처리도 문자열 `system`이 아니라 실행 주체로 해석하는 member id 값을 참조해 기록한다.
- [x] 랜덤 리워드의 꽝/미지급 케이스는 `event_win` 행 없이 처리한다.
- [x] 외부 point API 타임아웃은 `connection timeout = 1초`, `read timeout = 2초`, `총 대기 시간 = 최대 3초`를 사용한다.
- [x] 외부 point API 타임아웃은 외부 시스템 장애로 간주하고, 운영 알림 및 재처리 대상으로 관리한다.
- [ ] 향후 AWS 기반 큐 전환 시 `event_win` 생성 시점을 어떻게 가져갈지
- [x] 외부 point API 실패 후 자동 point 보정 차감은 하지 않는다.
- [x] 외부 point API는 `idempotency_key = event_id + round_id + member_id`를 사용한다.
- [x] 운영 재처리나 수동 재호출 시 같은 `idempotency_key`로 point API를 재호출한다.

## API 상세 확정 항목

- [x] 성공 응답 code는 `SUCCESS`를 사용한다.
- [x] 실패 응답 code는 각 오류의 `ResponseCode.code`를 사용한다.
- [x] business 예외는 domain code를, validation/system 예외는 `CommonCode`를 사용한다.
- [x] GET 응답에서는 `createdAt`을 제거한다.
- [x] GET 응답에서는 `supplierId`, `eventUrl`을 고정 노출한다.

## DDL로 확인된 사항

- [x] `event_round`는 `(event_id, round_no)` unique index로 이벤트 내 회차 번호가 보호된다.
- [x] 제공된 원본 DDL의 `event_applicant` unique는 `(round_id, member_id)`였다.
- [x] 후속 랜덤 보상 기능이 사용할 `event_round_prize`, `event_win` 경로가 준비되어 있다.
- [x] 신규 환경용 schema draft는 `promotion` 스키마를 사용한다.
- [x] 신규 환경용 schema draft는 최소 FK를 반영한다.
- [x] 신규 환경용 schema draft는 `event_applicant (round_id, member_id)` unique를 반영한다.
- [x] 신규 환경용 schema draft는 `event_entry.event_id`를 유지하고 `round_id`는 제거했다.
- [x] 신규 환경용 schema draft는 `TIMESTAMP` 컬럼을 사용하고 애플리케이션 기준 타임존은 `Asia/Seoul`이다.
- [x] 신규 환경용 schema draft는 `uq_event_round_event_round_no`, `uq_event_applicant_round_member_id`, `uq_event_win_entry_id`를 유지한다.
