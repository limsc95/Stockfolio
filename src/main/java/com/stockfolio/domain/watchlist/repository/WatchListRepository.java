package com.stockfolio.domain.watchlist.repository;

import com.stockfolio.domain.watchlist.entity.WatchListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchListRepository extends JpaRepository<WatchListItem, Long> {

    // stock 정보까지 한 번에 fetch (N+1 방지)
    @Query("SELECT w FROM WatchListItem w JOIN FETCH w.stock WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<WatchListItem> findAllByUserIdWithStock(@Param("userId") Long userId);

    Optional<WatchListItem> findByUserIdAndStockCode(Long userId, String stockCode);

    boolean existsByUserIdAndStockCode(Long userId, String stockCode);
}
