package com.stockfolio.domain.stock.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class StockPriceResponse {

    private final String code;
    private final String name;
    private final BigDecimal currentPrice;
    private final boolean priceAvailable;   // false면 KIS 미연동 또는 조회 실패
    private final LocalDateTime queriedAt;

    private StockPriceResponse(String code, String name, BigDecimal currentPrice) {
        this.code = code;
        this.name = name;
        this.currentPrice = currentPrice;
        this.priceAvailable = currentPrice.compareTo(BigDecimal.ZERO) > 0;
        this.queriedAt = LocalDateTime.now();
    }

    public static StockPriceResponse of(String code, String name, BigDecimal price) {
        return new StockPriceResponse(code, name, price);
    }
}
