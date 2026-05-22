-- ──────────────────────────────────────────────────────────
-- 초기 관리자 계정 (Docker 최초 실행 시 자동 삽입)
-- 비밀번호: admin1234!  (BCrypt 해시)
-- 운영 배포 전 반드시 비밀번호 변경할 것
-- ──────────────────────────────────────────────────────────
INSERT IGNORE INTO users (email, password, name, role, is_active, created_at, updated_at)
VALUES (
    'admin@stockfolio.com',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoOe5UjTuEJE4EnLcFRThBygGMsqQ.We8e',
    '관리자',
    'ADMIN',
    true,
    NOW(),
    NOW()
);
