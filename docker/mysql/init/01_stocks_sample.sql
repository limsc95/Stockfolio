-- ──────────────────────────────────────────────────────────
-- stocks 샘플 데이터 (Docker 최초 실행 시 자동 삽입)
-- ──────────────────────────────────────────────────────────
INSERT IGNORE INTO stocks (code, name, market, sector, created_at, updated_at) VALUES
-- KOSPI
('005930', '삼성전자',   'KOSPI',  '반도체',       NOW(), NOW()),
('000660', 'SK하이닉스', 'KOSPI',  '반도체',       NOW(), NOW()),
('035420', 'NAVER',      'KOSPI',  'IT서비스',     NOW(), NOW()),
('035720', '카카오',     'KOSPI',  'IT서비스',     NOW(), NOW()),
('005380', '현대차',     'KOSPI',  '자동차',       NOW(), NOW()),
('051910', 'LG화학',     'KOSPI',  '화학',         NOW(), NOW()),
('006400', '삼성SDI',    'KOSPI',  '전기전자',     NOW(), NOW()),
('068270', '셀트리온',   'KOSPI',  '바이오',       NOW(), NOW()),
-- KOSDAQ
('247540', '에코프로비엠', 'KOSDAQ', '전기전자',   NOW(), NOW()),
('086520', '에코프로',    'KOSDAQ', '전기전자',   NOW(), NOW()),
('091990', '셀트리온헬스케어', 'KOSDAQ', '바이오', NOW(), NOW()),
('196170', '알테오젠',    'KOSDAQ', '바이오',     NOW(), NOW());
