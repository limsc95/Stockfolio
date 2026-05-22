package com.stockfolio.domain.stock.dto;

import com.stockfolio.domain.stock.entity.Stock;
import lombok.Getter;

@Getter
public class StockResponse {

    private final String code;
    private final String name;
    private final String market;
    private final String sector;

    private StockResponse(Stock stock) {
        this.code = stock.getCode();
        this.name = stock.getName();
        this.market = stock.getMarket().name();
        this.sector = stock.getSector();
    }

    public static StockResponse from(Stock stock) {
        return new StockResponse(stock);
    }
}
