package com.stockfolio.domain.user.repository;

import com.stockfolio.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * UserRepository 통합 테스트
 *
 * @DataJpaTest:
 *   - H2 인메모리 DB를 자동으로 사용한다. (build.gradle testRuntimeOnly 'com.h2database:h2')
 *   - JPA/Hibernate 계층만 로드하므로 실행이 매우 빠르다.
 *   - 각 테스트는 트랜잭션 안에서 실행 후 자동 롤백된다. (테스트 간 독립성 보장)
 *
 * @Import(TestJpaConfig.class):
 *   - BaseEntity의 createdAt/updatedAt 자동 채우기(@EnableJpaAuditing)를 활성화한다.
 */
@DataJpaTest
@DisplayName("UserRepository 통합 테스트")
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    // ── 테스트용 User 생성 헬퍼 ───────────────────────────
    private User buildUser(String email) {
        return User.builder()
                .email(email)
                .password("encoded_password")
                .name("테스터")
                .role(User.Role.USER)
                .build();
    }

    // ════════════════════════════════════════════════════
    // findByEmailAndDeletedAtIsNull()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("findByEmailAndDeletedAtIsNull()")
    class FindByEmailTest {

        @Test
        @DisplayName("활성 유저는 이메일로 조회되어야 한다")
        void active_user_found_by_email() {
            // given
            userRepository.save(buildUser("active@test.com"));

            // when
            Optional<User> result = userRepository.findByEmailAndDeletedAtIsNull("active@test.com");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("active@test.com");
        }

        @Test
        @DisplayName("소프트 삭제된 유저는 조회되지 않아야 한다")
        void soft_deleted_user_not_found_by_email() {
            // given: 저장 후 소프트 삭제
            User user = userRepository.save(buildUser("deleted@test.com"));
            user.softDelete();
            userRepository.save(user);

            // when
            Optional<User> result = userRepository.findByEmailAndDeletedAtIsNull("deleted@test.com");

            // then: deletedAt이 채워진 row는 쿼리에서 제외
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 이메일 조회 시 빈 Optional을 반환해야 한다")
        void unknown_email_returns_empty() {
            Optional<User> result = userRepository.findByEmailAndDeletedAtIsNull("ghost@test.com");

            assertThat(result).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════
    // existsByEmail()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("existsByEmail()")
    class ExistsByEmailTest {

        @Test
        @DisplayName("이미 저장된 이메일이면 true를 반환해야 한다")
        void returns_true_for_existing_email() {
            // given
            userRepository.save(buildUser("exists@test.com"));

            // when & then
            assertThat(userRepository.existsByEmail("exists@test.com")).isTrue();
        }

        @Test
        @DisplayName("저장되지 않은 이메일이면 false를 반환해야 한다")
        void returns_false_for_unknown_email() {
            assertThat(userRepository.existsByEmail("nobody@test.com")).isFalse();
        }
    }

    // ════════════════════════════════════════════════════
    // findByIdAndDeletedAtIsNull()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("findByIdAndDeletedAtIsNull()")
    class FindByIdTest {

        @Test
        @DisplayName("활성 유저는 ID로 조회되어야 한다")
        void active_user_found_by_id() {
            // given
            User saved = userRepository.save(buildUser("byid@test.com"));

            // when
            Optional<User> result = userRepository.findByIdAndDeletedAtIsNull(saved.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("소프트 삭제된 유저는 ID로 조회되지 않아야 한다")
        void deleted_user_not_found_by_id() {
            // given
            User user = userRepository.save(buildUser("deletedbyid@test.com"));
            user.softDelete();
            userRepository.save(user);

            // when
            Optional<User> result = userRepository.findByIdAndDeletedAtIsNull(user.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════
    // countByIsActiveTrue()
    // ════════════════════════════════════════════════════
    @Test
    @DisplayName("countByIsActiveTrue() — 비활성 계정은 카운트에서 제외되어야 한다")
    void count_excludes_inactive_users() {
        // given: 활성 1명, 비활성 1명
        userRepository.save(buildUser("active_count@test.com"));
        User inactive = userRepository.save(buildUser("inactive_count@test.com"));
        inactive.deactivate();
        userRepository.save(inactive);

        // when
        long activeCount = userRepository.countByIsActiveTrue();
        long totalCount  = userRepository.count();

        // then: 비활성 유저 수만큼 차이가 나야 한다
        assertThat(activeCount).isLessThan(totalCount);
        assertThat(totalCount - activeCount).isGreaterThanOrEqualTo(1);
    }
}
