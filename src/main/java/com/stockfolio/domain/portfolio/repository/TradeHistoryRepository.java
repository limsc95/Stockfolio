package com.stockfolio.domain.portfolio.repository;

import com.stockfolio.domain.portfolio.entity.TradeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long> {

    Page<TradeHistory> findAllByPortfolioHoldingId(Long holdingId, Pageable pageable);
}
