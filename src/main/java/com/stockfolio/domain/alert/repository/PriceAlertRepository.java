package com.stockfolio.domain.alert.repository;

import com.stockfolio.domain.alert.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    List<PriceAlert> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT a FROM PriceAlert a WHERE a.user.id = :userId AND a.triggered = :triggered ORDER BY a.createdAt DESC")
    List<PriceAlert> findAllByUserIdAndIsTriggeredOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("triggered") boolean isTriggered);

    @Query("SELECT a FROM PriceAlert a JOIN FETCH a.user WHERE a.triggered = false")
    List<PriceAlert> findAllUntriggeredWithUser();

    // 오늘 발동된 알림 수
    // DATE(triggeredAt) = CURRENT_DATE → Hibernate 6 TypecheckUtil이 Object vs java.sql.Date 타입 불일치를 감지
    // → 네이티브 쿼리로 JPQL 파서를 우회한다
    @Query(value = "SELECT COUNT(*) FROM price_alerts WHERE is_triggered = TRUE AND DATE(triggered_at) = CURRENT_DATE", nativeQuery = true)
    long countTriggeredToday();
}
