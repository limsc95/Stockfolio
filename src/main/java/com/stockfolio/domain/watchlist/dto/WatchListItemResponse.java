package com.stockfolio.domain.watchlist.dto;

import com.stockfolio.domain.watchlist.entity.WatchListItem;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class WatchListItemResponse {

    private final Long id;
    private final String stockCode;
    private final String stockName;
    private final String market;
    private final String sector;
    private final BigDecimal currentPrice;
    private final boolean priceAvailable;
    private final LocalDateTime addedAt;

    private WatchListItemResponse(WatchListItem item, BigDecimal currentPrice) {
        this.id = item.getId();
        this.stockCode = item.getStock().getCode();
        this.stockName = item.getStock().getName();
        this.market = item.getStock().getMarket().name();
        this.sector = item.getStock().getSector();
        this.currentPrice = currentPrice;
        this.priceAvailable = currentPrice.compareTo(BigDecimal.ZERO) > 0;
        this.addedAt = item.getCreatedAt();
    }

    public static WatchListItemResponse of(WatchListItem item, BigDecimal currentPrice) {
        return new WatchListItemResponse(item, currentPrice);
    }
}
