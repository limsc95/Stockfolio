package com.stockfolio.domain.alert.repository;

import com.stockfolio.domain.alert.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    List<PriceAlert> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<PriceAlert> findAllByUserIdAndIsTriggeredOrderByCreatedAtDesc(Long userId, boolean isTriggered);

    @Query("SELECT a FROM PriceAlert a JOIN FETCH a.user WHERE a.isTriggered = false")
    List<PriceAlert> findAllUntriggeredWithUser();

    // 오늘 발동된 알림 수
    @Query("SELECT COUNT(a) FROM PriceAlert a WHERE a.isTriggered = true AND DATE(a.triggeredAt) = CURRENT_DATE")
    long countTriggeredToday();
}
