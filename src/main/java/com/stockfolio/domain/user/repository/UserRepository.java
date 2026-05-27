package com.stockfolio.domain.user.repository;

import com.stockfolio.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByEmail(String email);

    // 관리자 통계
    // NOTE: boolean 필드 isActive 에 대한 파생 쿼리가 Hibernate 6의 엄격한 타입 체크를 통과하지 못하므로
    //       명시적 JPQL 사용 (SemanticException 방지)
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countByIsActiveTrue();

    // 관리자 검색 (이메일 or 이름)
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<User> searchByEmailOrName(@Param("q") String query, Pageable pageable);
}
