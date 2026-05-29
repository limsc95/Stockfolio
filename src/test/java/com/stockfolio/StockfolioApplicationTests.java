package com.stockfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 애플리케이션 컨텍스트 스모크 테스트
 *
 * @SpringBootTest: 전체 Spring Application Context 를 로드해 빈 구성 오류를 검증한다.
 *
 * 인프라 모킹 전략:
 *   - 테스트 환경에는 실제 Redis/RabbitMQ 서버가 없으므로 ConnectionFactory 를 @MockBean 으로 교체한다.
 *   - RedisConnectionFactory  → RedisConfig.redisTemplate() 의 파라미터 충족
 *   - ConnectionFactory       → RabbitMQConfig.rabbitTemplate() 의 파라미터 충족
 *   - 두 Config 클래스 모두 ConnectionFactory 가 null 이 아닌지만 확인하므로 Mock 으로도 빈 생성이 성공한다.
 *
 * (src/test/resources/application.yml 에서 Redis/RabbitMQ Auto-Configuration 을 이미 제외했지만,
 *  RedisConfig·RabbitMQConfig 는 수동 @Configuration 이므로 auto-configuration 제외와 무관하게 로드된다.)
 */
@SpringBootTest
class StockfolioApplicationTests {

    /** RedisConfig.redisTemplate(RedisConnectionFactory) 의 의존성 충족 */
    @MockBean
    RedisConnectionFactory redisConnectionFactory;

    /** RabbitMQConfig.rabbitTemplate(ConnectionFactory) 의 의존성 충족 */
    @MockBean
    ConnectionFactory rabbitConnectionFactory;

    /** AlertMessageConsumer(JavaMailSender, ...) 의 의존성 충족 */
    @MockBean
    JavaMailSender javaMailSender;

    @Test
    @DisplayName("Spring Application Context 가 정상 로드되어야 한다")
    void contextLoads() {
        // 빈 구성 오류가 없으면 테스트 통과
    }
}
