# Tech Stack

## 확정 스택

- Backend: Spring Boot
- Language: Java 21
- ORM: JPA
- Query: QueryDSL
- Database: PostgreSQL
- Runtime/Local Infra: Docker

## spec와 구현의 연결 기준

- `api-spec.md`는 Controller/Request/Response 설계의 기준이 된다.
- `data-spec.md`는 Entity/Table/Constraint 해석의 기준이 된다.
- `business-rules.md`는 Service 계층 검증 로직의 기준이 된다.
- `test-scenarios.md`는 단위/통합 테스트 케이스의 기준이 된다.

## 구현 전제

- DB 스키마는 이미 문서에 반영되었고, 현재 출석체크 spec은 그 DDL 해석을 포함한다.
- 출석체크용 최소 API 목록은 추후 제공될 예정이다.
- 현재 문서는 구현 착수 전에 합의해야 하는 초안 저장소 역할을 한다.

## 권장 패키지 방향

- 기본 패키지 구조는 팀 예시와 맞춘 레이어드 + 포트/어댑터 혼합 구조를 사용한다.
- 상세 기준은 `shared/package-structure.md`를 따른다.
