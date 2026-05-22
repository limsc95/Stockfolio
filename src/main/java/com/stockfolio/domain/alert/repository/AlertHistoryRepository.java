package com.stockfolio.domain.alert.repository;

import com.stockfolio.domain.alert.entity.AlertHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    Page<AlertHistory> findAllByUserIdOrderBySentAtDesc(Long userId, Pageable pageable);

    Page<AlertHistory> findAllByStatusOrderBySentAtDesc(AlertHistory.Status status, Pageable pageable);

    // 오늘 발송된 이력 수
    @Query("SELECT COUNT(h) FROM AlertHistory h WHERE DATE(h.sentAt) = CURRENT_DATE")
    long countSentToday();

    // 오늘 실패 수
    @Query("SELECT COUNT(h) FROM AlertHistory h WHERE h.status = 'FAILED' AND DATE(h.sentAt) = CURRENT_DATE")
    long countFailedToday();
}
