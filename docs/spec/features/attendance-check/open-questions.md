# Open Questions

구현 전에 확정해야 하지만 아직 입력물이 없거나, 현재 DDL과 업무 의미 사이에 차이가 있는 항목을 정리한다.

## 현재까지 확정된 항목

- [x] 출석 단위는 `event_round` 기준의 일자성 회차다.
- [x] 월간 출석 이벤트는 같은 `event_id` 아래 날짜 수만큼 `event_round`를 가진다.
- [x] `event_applicant`는 이벤트 참여자 앵커로 사용된다.
- [x] `event_entry`는 출석 성공 및 향후 랜덤 리워드 참여 이력을 append-only로 저장한다.
- [x] `event_win`은 실제 지급 보상과 외부 보상 API 성공 이력을 저장한다.
- [x] 오늘 날짜 기준 `event_entry`가 이미 존재하면 이미 출석한 것으로 본다.
- [x] 중복 출석 시 프론트에는 `이미 출석했습니다`를 노출한다.
- [x] 출석체크 보상은 주로 point 중심으로 설계한다.
- [x] `prize`는 운영 세팅 완료 후 수정하지 않고, 변경이 필요하면 새 `prize`를 생성한다.
- [x] 현재 출석 성공은 외부 point API 성공까지 포함한다.
- [x] 외부 point API 실패 또는 무응답 시 `event_entry`, `event_win`은 롤백한다.
- [x] 현재는 외부 API 동기 응답 기반이며, 무응답 시 프론트는 출석체크 불가 오류를 표시한다.

## 우선 확인 항목

- [ ] `event_applicant.round_id`는 최초 참여 회차를 의미하는지, 다른 의미인지
- [ ] `event_applicant`를 이벤트 단위 앵커로 재사용할 때 조회 기준을 `(event_id, member_id)`로 고정할지
- [ ] 위 조회 기준이 맞다면 `(event_id, member_id)` 기준 unique/index 보강이 필요한지
- [ ] `event_entry`와 `event_round`를 어떻게 연결할지
- [ ] `event_entry.event_round_prize_id`를 출석체크에서 사용할지, `event_win.event_round_prize_id`만 사용할지
- [ ] 클라이언트가 `round_id`를 직접 보내는지, 서버가 `applied_at` 기준으로 활성 회차를 계산하는지
- [ ] 오늘 날짜 판정의 기준 타임존을 무엇으로 볼지
- [ ] 출석 결과 조회 API가 꼭 필요한가
- [ ] 운영자 전용 API 또는 조회 기능이 필요한가
- [ ] `is_visible = FALSE` 이벤트에 대해 직접 API 출석을 허용할지
- [ ] soft delete된 applicant/entry가 있을 때 재출석을 허용할지
- [ ] soft delete된 `prize` 또는 `event_round_prize`가 기존 집계/조회에 어떤 의미를 가지는지
- [ ] 참여자 식별은 인증 토큰 기반인가, 요청 필드 기반인가
- [ ] 취소/정정 기능이 필요한가
- [ ] 감사 로그 수준은 어디까지 필요한가
- [ ] 랜덤 리워드에서 꽝/미지급 케이스를 `event_win` 행으로 남길지, 행 없이 처리할지
- [ ] 외부 point API 타임아웃 기준 시간과 사용자 노출 에러 코드를 무엇으로 할지
- [ ] 향후 AWS 기반 큐 전환 시 `event_win` 생성 시점을 어떻게 가져갈지
- [ ] 외부 point API 성공 후 DB 커밋 실패 시 보상 보정 또는 재처리를 어떻게 할지
- [ ] 첫 출석에서 `event_applicant`가 생성된 뒤 외부 API가 실패하면 applicant도 함께 rollback할지

## API 목록 수신 후 확인 항목

- [ ] 최소 API가 조회 포함인지, 저장 중심인지
- [ ] 요청/응답 DTO 초안이 필요한지
- [ ] 공통 오류 포맷이 필요한지
- [ ] 출석 응답에 `entryId`, `applicantId`, `winId`, `roundId`, `appliedAt`, `pointAmount` 중 무엇을 포함할지

## DDL로 확인된 사항

- [x] `event_round`는 `(event_id, round_no)` unique index로 이벤트 내 회차 번호가 보호된다.
- [x] `event_applicant`는 `(round_id, member_id)` unique index가 있다.
- [x] 후속 랜덤 보상 기능이 사용할 `event_round_prize`, `event_win` 경로가 준비되어 있다.
- [x] `event_round_prize`는 `prize`를 FK로 참조한다.
- [x] `event_win`은 `event_entry`, `event_round`를 FK로 참조한다.
- [ ] `event_entry` 기준 동일 회차 중복을 직접 막는 DB 제약은 없다.
