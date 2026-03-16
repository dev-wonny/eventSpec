# Open Questions

구현 전에 확정해야 하지만 아직 입력물이 없거나, 현재 DDL과 업무 의미 사이에 차이가 있는 항목을 정리한다.

## 현재까지 확정된 항목

- [x] 출석 단위는 `event_round` 기준의 일자성 회차다.
- [x] 월간 출석 이벤트는 같은 `event_id` 아래 날짜 수만큼 `event_round`를 가진다.
- [x] `event_applicant`는 이벤트 참여 가능 대상자 풀로 사용된다.
- [x] `event_applicant.round_id`는 필수값이다.
- [x] 이벤트 생성 시 `event_round`는 최소 1개 생성되므로 `event_applicant.round_id`는 기준 회차를 가진다.
- [x] `event_entry`는 출석 성공 및 향후 랜덤 리워드 참여 이력을 append-only로 저장한다.
- [x] `event_win`은 실제 지급 보상과 외부 보상 API 성공 이력을 저장한다.
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
- [x] `event_applicant`는 `(event_id, member_id)` unique로 동작한다.
- [x] 출석 중복의 비즈니스 기준은 같은 `event_id + round_id + member_id` 재요청이다.
- [x] 출석 이벤트에서 `event_entry`는 매일 출석 여부 판정을 위해 `event_id`, `round_id`를 가진다.
- [x] 중복 출석 시 프론트에는 `이미 출석했습니다`를 노출한다.
- [x] FK는 두지 않고 Service 검증으로 참조 정합성을 보장한다.
- [x] DB에는 최소 unique만 유지한다.
- [x] 출석체크 보상은 주로 point 중심으로 설계한다.
- [x] 출석체크는 회차당 보상 매핑이 `0..1`개다.
- [x] 출석 회차에 보상 매핑이 없으면 point를 지급하지 않는다.
- [x] 랜덤 리워드는 회차당 여러 보상을 둘 수 있다.
- [x] `prize`는 운영 세팅 완료 후 수정하지 않고, 변경이 필요하면 새 `prize`를 생성한다.
- [x] 현재 출석 성공은 보상 매핑이 있는 경우 외부 point API 성공까지 포함한다.
- [x] 외부 point API 실패 또는 무응답 시 `event_entry`, `event_win`은 롤백한다.
- [x] 현재는 외부 API 동기 응답 기반이며, 무응답 시 프론트는 출석체크 불가 오류를 표시한다.
- [x] 애플리케이션 로그는 이번 범위에서 ELK로 적재한다.

## 우선 확인 항목

- [ ] `event_entry.event_round_prize_id`를 출석체크에서 사용할지, `event_win.event_round_prize_id`만 사용할지
- [ ] 랜덤 리워드에서 여러 active `event_round_prize`와 확률 정책을 어떤 계산 규칙으로 적용할지
- [ ] `GET`의 `TODAY / MISSED / FUTURE` 판정 기준 타임존을 무엇으로 볼지
- [ ] `GET`에서 `X-Member-Id`가 없을 때 `attendanceSummary`를 생략할지, `totalDays`만 줄지
- [ ] `is_visible = FALSE` 이벤트에 대해 직접 API 출석을 허용할지
- [ ] soft delete된 applicant/entry가 있을 때 재출석을 허용할지
- [ ] soft delete된 `prize` 또는 `event_round_prize`가 기존 집계/조회에 어떤 의미를 가지는지
- [ ] 취소/정정 기능이 필요한가
- [ ] 감사 로그 수준은 어디까지 필요한가
- [ ] 랜덤 리워드에서 꽝/미지급 케이스를 `event_win` 행으로 남길지, 행 없이 처리할지
- [ ] 외부 point API 타임아웃 기준 시간과 사용자 노출 에러 코드를 무엇으로 할지
- [ ] 향후 AWS 기반 큐 전환 시 `event_win` 생성 시점을 어떻게 가져갈지
- [ ] 외부 point API 성공 후 DB 커밋 실패 시 보상 보정 또는 재처리를 어떻게 할지

## API 상세 확정 항목

- [x] API 응답 code는 `CommonCode` 기준으로 반환한다.
- [x] 성공 응답 code는 `SUCCESS`를 사용한다.
- [ ] GET 응답에 `createdAt`, `supplierId`, `eventUrl` 등을 모두 고정 노출할지

## DDL로 확인된 사항

- [x] `event_round`는 `(event_id, round_no)` unique index로 이벤트 내 회차 번호가 보호된다.
- [x] 제공된 원본 DDL의 `event_applicant` unique는 `(round_id, member_id)`였다.
- [x] 후속 랜덤 보상 기능이 사용할 `event_round_prize`, `event_win` 경로가 준비되어 있다.
- [x] 신규 환경용 schema draft는 FK를 두지 않는다.
- [x] 신규 환경용 schema draft는 `event_applicant (event_id, member_id)`와 `NOT NULL round_id`를 반영했다.
- [x] 신규 환경용 schema draft는 `event_entry.event_id`, `event_entry.round_id`를 반영했다.
- [x] 신규 환경용 schema draft는 `uq_event_round_event_round_no`, `uq_event_applicant_event_member_id`, `uq_event_entry_event_round_member`, `uq_event_win_entry_id`만 최소 unique로 유지한다.
