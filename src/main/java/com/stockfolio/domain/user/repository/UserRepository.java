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
    // NOTE: 'boolean isActive' 필드명에 대해 Hibernate 6이 JPQL 타입 체크(TypecheckUtil)를
    //       통과하지 못하는 SemanticException 이슈가 있다.
    //       nativeQuery = true 로 JPQL 파서를 완전히 우회해 해결한다.
    //       테이블명 'users', 컬럼명 'is_active' 는 @Table/@Column 매핑과 일치한다.
    @Query(value = "SELECT COUNT(*) FROM users WHERE is_active = TRUE", nativeQuery = true)
    long countByIsActiveTrue();

    // 관리자 검색 (이메일 or 이름)
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<User> searchByEmailOrName(@Param("q") String query, Pageable pageable);
}
