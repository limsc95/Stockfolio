package com.stockfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing      // BaseEntity의 createdAt/updatedAt 자동 관리
@EnableScheduling       // 가격 알림 스케줄러 활성화
public class StockfolioApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockfolioApplication.class, args);
    }
}
