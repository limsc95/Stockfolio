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
    // DATE(sentAt) = CURRENT_DATE → Hibernate 6 TypecheckUtil이 Object vs java.sql.Date 타입 불일치를 감지
    // → 네이티브 쿼리로 JPQL 파서를 우회한다
    @Query(value = "SELECT COUNT(*) FROM alert_histories WHERE DATE(sent_at) = CURRENT_DATE", nativeQuery = true)
    long countSentToday();

    // 오늘 실패 수 — enum 을 문자열 리터럴로 비교하면 Hibernate 6 타입 체크를 통과하지 못하므로
    // 네이티브 쿼리 사용 (status 컬럼은 VARCHAR 'FAILED' 값으로 저장됨)
    @Query(value = "SELECT COUNT(*) FROM alert_histories WHERE status = 'FAILED' AND DATE(sent_at) = CURRENT_DATE", nativeQuery = true)
    long countFailedToday();
}
