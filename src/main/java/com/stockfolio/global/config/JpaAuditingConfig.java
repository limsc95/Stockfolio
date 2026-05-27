package com.stockfolio.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 전용 설정 클래스
 *
 * @EnableJpaAuditing 을 StockfolioApplication 에서 분리한 이유:
 *   - @WebMvcTest 는 Spring MVC 계층만 로드하므로 JPA 컨텍스트(엔티티 메타모델)가 없다.
 *   - @EnableJpaAuditing 이 @SpringBootApplication 에 붙어 있으면,
 *     @WebMvcTest 슬라이스도 그 애노테이션을 처리하려 시도 →
 *     "JPA metamodel must not be empty" 오류 발생.
 *   - 이 클래스를 @WebMvcTest 의 excludeFilters 로 제외하면 문제를 피할 수 있다.
 *   - @DataJpaTest 는 컴포넌트 스캔 경로 내 @Configuration 을 로드하므로
 *     이 클래스를 통해 JPA Auditing 이 정상 동작한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
