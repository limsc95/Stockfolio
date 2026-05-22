package com.stockfolio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class StockfolioApplicationTests {

    @Test
    void contextLoads() {
        // Spring Application Context가 정상 로드되는지 확인
    }
}
