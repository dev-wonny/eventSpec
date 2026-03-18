package com.event.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * createdAt, updatedAt 같은 공통 audit 시각은 JPA auditing이 자동으로 관리한다.
 * createdBy, updatedBy는 현재처럼 서비스/엔티티 팩토리에서 명시적으로 채운다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
}
