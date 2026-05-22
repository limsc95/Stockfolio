package com.stockfolio.domain.stock.repository;

import com.stockfolio.domain.stock.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, String> {

    // 종목명 OR 종목코드 검색 (대소문자 무시)
    @Query("SELECT s FROM Stock s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.code) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Stock> searchByNameOrCode(@Param("query") String query, Pageable pageable);

    @Query("SELECT s FROM Stock s WHERE s.market = :market AND " +
           "(LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.code) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Stock> searchByNameOrCodeAndMarket(@Param("query") String query,
                                             @Param("market") Stock.Market market,
                                             Pageable pageable);
}
