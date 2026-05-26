package com.stockfolio.domain.stock.entity;

import com.stockfolio.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Market market;

    @Column(length = 100)
    private String sector;

    @Builder
    public Stock(String code, String name, Market market, String sector) {
        this.code = code;
        this.name = name;
        this.market = market;
        this.sector = sector;
    }

    // ── 도메인 메서드 ──────────────────────────────────────
    public void updateInfo(String name, String sector) {
        this.name = name;
        this.sector = sector;
    }

    public enum Market {
        KOSPI, KOSDAQ, NYSE, NASDAQ
    }
}
