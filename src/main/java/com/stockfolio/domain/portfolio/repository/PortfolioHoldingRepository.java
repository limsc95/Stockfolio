package com.stockfolio.domain.portfolio.repository;

import com.stockfolio.domain.portfolio.entity.PortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHolding, Long> {

    Optional<PortfolioHolding> findByPortfolioIdAndStockCode(Long portfolioId, String stockCode);

    boolean existsByPortfolioIdAndStockCode(Long portfolioId, String stockCode);
}
