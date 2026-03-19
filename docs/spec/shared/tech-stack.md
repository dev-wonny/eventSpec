# Tech Stack

## 확정 스택

- Build: Gradle
- Project Name: `ds-event-backend`
- Group: `com.event`
- Backend Framework: Spring Boot `3.3.2`
- Language: Java `21`
- Dependency Management: Spring Dependency Management `1.1.6`
- Web: Spring Web
- Validation: Spring Validation
- Persistence Mapping / Write: Spring Data JPA
- Read Query: QueryDSL `5.1.0`
- Database: PostgreSQL
- Monitoring: Spring Boot Actuator
- API Docs: Swagger / OpenAPI (`springdoc-openapi-starter-webmvc-ui`, Spring Boot `3.3.x` 기준 `2.6.x` 라인)
- Logging: ELK + `logstash-logback-encoder` `7.4`
- Observability: Spring Boot Actuator + ELK
- Deployment: ECS
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
- JWT, Spring Security, Redis, Spring Cloud는 현재 범위에 포함하지 않는다.
- API 문서화는 Swagger UI + OpenAPI 기반으로 제공한다.

## 구현 시사점

- 패키지 루트는 현재 Gradle `group` 기준인 `com.event`를 사용한다.
- 외부 포인트 지급 클라이언트는 Spring Cloud 없이 pure OpenFeign 기반으로 구현한다.
- `@FeignClient`는 도입하지 않고, ECS/ALB 환경 전제에서 Feign builder 기반 client를 사용한다.
- QueryDSL Q 클래스는 Gradle generated source 설정을 기준으로 관리한다.
- QueryDSL 검색 조건 빌더는 `...EntityBuilder` 클래스와 `BooleanBuilder` 기반 조합을 기본으로 한다.
- 조회는 JPA 연관 join보다 QueryDSL 개별 조회를 기본 경로로 사용한다.
- 저장과 상태 변경은 JPA Entity 기반으로 처리한다.
- 검색 조건 DTO의 `eventType`은 문자열보다 enum 사용을 권장한다.
- 애플리케이션 로그는 구조화 로그로 남기고 ELK 적재를 이번 범위에 포함한다.
- ECS 배포 환경을 기준으로 stdout 중심 로그 수집을 고려한다.
- Controller와 DTO는 `@Tag`, `@Operation`, `@Schema` 등 Swagger annotation을 사용해 문서화한다.

## 권장 최소 build.gradle

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.2'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.event'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'org.postgresql:postgresql'

    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
    implementation 'io.github.openfeign:feign-core:13.5'
    implementation 'io.github.openfeign:feign-jackson:13.5'

    implementation 'com.querydsl:querydsl-jpa:5.1.0:jakarta'
    annotationProcessor 'com.querydsl:querydsl-apt:5.1.0:jakarta'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 권장 패키지 방향

- 기본 패키지 구조는 팀 예시와 맞춘 Layered + Hexagonal Hybrid 구조를 사용한다.
- 상세 기준은 `shared/package-structure.md`를 따른다.
- 로그/관측성 상세 기준은 `shared/logging-observability-spec.md`를 따른다.
