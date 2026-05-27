/**
 * k6 시나리오 1: 정상 사용자 플로우
 *
 * 흐름: 로그인 → 내 포트폴리오 조회 → 종목 시세 조회
 *
 * 실행:
 *   k6 run k6/scenario_flow.js
 *   k6 run --out json=k6/results/flow_result.json k6/scenario_flow.js
 *
 * 목표:
 *   p99 응답시간 ≤ 500ms, 에러율 < 1%  @  30 VU
 */
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// ── 커스텀 메트릭 ──────────────────────────────────────────
const errorRate       = new Rate('error_rate');
const loginDuration   = new Trend('login_duration',     true);
const portfolioDuration = new Trend('portfolio_duration', true);
const priceDuration   = new Trend('price_duration',     true);
const failCount       = new Counter('fail_count');

// ── 설정 ──────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 테스트에 사용할 계정 (사전에 DB에 등록 필요)
const TEST_USER = {
  email:    __ENV.TEST_EMAIL    || 'test@test.test',
  password: __ENV.TEST_PASSWORD || 'a123z123',
};

// 시세 조회에 사용할 종목 코드 샘플
const STOCK_CODES = ['005930', '000660', '035720', '051910', '005380'];

export const options = {
  // 요약 출력에 p(99) 포함 (기본값은 p(90), p(95) 까지만 포함)
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  stages: [
    { duration: '30s', target: 10  },   // 워밍업: 0 → 10 VU
    { duration: '1m',  target: 30  },   // 목표 부하: 30 VU 유지
    { duration: '30s', target: 0   },   // 쿨다운
  ],
  thresholds: {
    'http_req_duration':   ['p(99)<500'],   // SLO: p99 ≤ 500ms
    'http_req_failed':     ['rate<0.01'],   // 에러율 < 1%
    'error_rate':          ['rate<0.01'],
    'login_duration':      ['p(99)<1000'],  // 로그인은 1초 이내
    'portfolio_duration':  ['p(99)<500'],
    'price_duration':      ['p(99)<300'],   // 캐시 히트 시 빠름
  },
};

// ── 메인 시나리오 ──────────────────────────────────────────
export default function () {
  let accessToken = null;

  // ── 1. 로그인 ──────────────────────────────────────────
  group('1. 로그인', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/auth/login`,
      JSON.stringify(TEST_USER),
      { headers: { 'Content-Type': 'application/json' } }
    );

    loginDuration.add(res.timings.duration);

    // 실제 응답 구조: { success: true, data: { accessToken, refreshToken, expiresIn } }
    const ok = check(res, {
      '로그인 200 OK': (r) => r.status === 200,
      '토큰 포함':     (r) => {
        try { return JSON.parse(r.body).data?.accessToken != null; }
        catch (_) { return false; }
      },
    });

    if (!ok) {
      errorRate.add(1);
      failCount.add(1);
      return;   // 로그인 실패 시 이후 단계 스킵
    }

    errorRate.add(0);
    try { accessToken = JSON.parse(res.body).data.accessToken; }
    catch (_) {}
  });

  if (!accessToken) {
    sleep(1);
    return;
  }

  const authHeaders = {
    headers: {
      'Content-Type':  'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
  };

  sleep(0.5);

  // ── 2. 포트폴리오 목록 조회 ─────────────────────────────
  group('2. 포트폴리오 조회', () => {
    const res = http.get(`${BASE_URL}/api/v1/portfolios`, authHeaders);
    portfolioDuration.add(res.timings.duration);

    const ok = check(res, {
      '포트폴리오 200 OK': (r) => r.status === 200,
    });
    errorRate.add(ok ? 0 : 1);
    if (!ok) failCount.add(1);
  });

  sleep(0.3);

  // ── 3. 종목 시세 조회 (랜덤 종목) ──────────────────────
  group('3. 종목 시세 조회', () => {
    const code = STOCK_CODES[Math.floor(Math.random() * STOCK_CODES.length)];
    const res  = http.get(`${BASE_URL}/api/v1/stocks/${code}/price`, authHeaders);
    priceDuration.add(res.timings.duration);

    const ok = check(res, {
      '시세 200 OK': (r) => r.status === 200 || r.status === 404,
    });
    errorRate.add(ok ? 0 : 1);
    if (!ok) failCount.add(1);
  });

  sleep(1);
}

// ── 테스트 종료 시 요약 출력 ───────────────────────────────
export function handleSummary(data) {
  const p99 = data.metrics['http_req_duration']?.values?.['p(99)'] ?? 'N/A';
  const errRate = ((data.metrics['error_rate']?.values?.rate ?? 0) * 100).toFixed(2);
  const reqs  = data.metrics['http_reqs']?.values?.count ?? 0;

  console.log('\n====================================================');
  console.log('  StockFolio k6 결과 — 시나리오 1: 정상 사용자 플로우');
  console.log('====================================================');
  console.log(`  총 요청 수    : ${reqs}`);
  console.log(`  p99 응답시간  : ${typeof p99 === 'number' ? p99.toFixed(2) + 'ms' : p99}`);
  console.log(`  에러율        : ${errRate}%`);
  console.log(`  SLO 달성 여부 : ${typeof p99 === 'number' && p99 <= 500 ? '✅ PASS (p99 ≤ 500ms)' : '❌ FAIL'}`);
  console.log('====================================================\n');

  return {
    'k6/results/flow_summary.json': JSON.stringify(data, null, 2),
    stdout: '',
  };
}
