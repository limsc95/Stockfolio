package com.stockfolio.domain.portfolio.repository;

import com.stockfolio.domain.portfolio.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findAllByUserId(Long userId);

    // holdings 까지 한 번에 fetch (N+1 방지)
    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.holdings WHERE p.id = :id")
    Optional<Portfolio> findByIdWithHoldings(@Param("id") Long id);
}
