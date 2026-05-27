/**
 * k6 시나리오 2: 급격한 트래픽 증가 (Spike / Ramp-up)
 *
 * 목적: 트래픽 폭증 상황에서 SLO 유지 여부 및 복구 능력 측정
 * 흐름: VU 1 → 100 → 1  (빠른 증가 / 즉각 감소)
 *
 * 실행:
 *   k6 run k6/scenario_rampup.js
 *   k6 run --out json=k6/results/rampup_result.json k6/scenario_rampup.js
 */
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ── 커스텀 메트릭 ──────────────────────────────────────────
const errorRate    = new Rate('error_rate');
const reqDuration  = new Trend('req_duration', true);

// ── 설정 ──────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const TEST_USER = {
  email:    __ENV.TEST_EMAIL    || 'test@test.test',
  password: __ENV.TEST_PASSWORD || 'a123z123',
};

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  stages: [
    { duration: '10s', target: 1   },   // 기준선
    { duration: '20s', target: 100 },   // 급격한 증가
    { duration: '1m',  target: 100 },   // 최대 부하 유지
    { duration: '10s', target: 0   },   // 급격한 감소
    { duration: '30s', target: 0   },   // 복구 확인
  ],
  thresholds: {
    'http_req_duration': ['p(95)<800'],  // 스파이크 시 p95 ≤ 800ms (여유 있게)
    'http_req_failed':   ['rate<0.05'],  // 에러율 < 5% (스파이크 허용)
    'error_rate':        ['rate<0.05'],
  },
};

// 가장 가벼운 읽기 엔드포인트로 최대 부하 측정
export default function () {
  let token = null;

  group('로그인', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/auth/login`,
      JSON.stringify(TEST_USER),
      { headers: { 'Content-Type': 'application/json' } }
    );
    reqDuration.add(res.timings.duration);

    const ok = check(res, { '로그인 성공': (r) => r.status === 200 });
    errorRate.add(ok ? 0 : 1);

    // 실제 응답 구조: { success: true, data: { accessToken, ... } }
    try { token = JSON.parse(res.body).data.accessToken; } catch (_) {}
  });

  if (!token) { sleep(0.5); return; }

  group('헬스 체크 + 포트폴리오 조회', () => {
    const responses = http.batch([
      {
        method: 'GET',
        url: `${BASE_URL}/actuator/health`,
      },
      {
        method: 'GET',
        url: `${BASE_URL}/api/v1/portfolios`,
        params: { headers: { Authorization: `Bearer ${token}` } },
      },
    ]);

    responses.forEach((res) => {
      reqDuration.add(res.timings.duration);
      const ok = check(res, { '2xx 응답': (r) => r.status >= 200 && r.status < 300 });
      errorRate.add(ok ? 0 : 1);
    });
  });

  sleep(0.2);
}

// ── 테스트 종료 시 요약 ────────────────────────────────────
export function handleSummary(data) {
  const p95 = data.metrics['http_req_duration']?.values?.['p(95)'] ?? 'N/A';
  const p99 = data.metrics['http_req_duration']?.values?.['p(99)'] ?? 'N/A';
  const errRate = ((data.metrics['error_rate']?.values?.rate ?? 0) * 100).toFixed(2);
  const reqs  = data.metrics['http_reqs']?.values?.count ?? 0;
  const rps   = data.metrics['http_reqs']?.values?.rate  ?? 'N/A';

  const fmt = (v) => typeof v === 'number' ? v.toFixed(2) + 'ms' : v;

  console.log('\n====================================================');
  console.log('  StockFolio k6 결과 — 시나리오 2: Spike / Ramp-up');
  console.log('====================================================');
  console.log(`  총 요청 수      : ${reqs}`);
  console.log(`  최대 처리량(RPS): ${typeof rps === 'number' ? rps.toFixed(1) : rps}`);
  console.log(`  p95 응답시간    : ${fmt(p95)}`);
  console.log(`  p99 응답시간    : ${fmt(p99)}`);
  console.log(`  에러율          : ${errRate}%`);
  console.log('----------------------------------------------------');
  console.log(`  p95 SLO (≤800ms): ${typeof p95 === 'number' && p95 <= 800 ? '✅ PASS' : '❌ FAIL'}`);
  console.log(`  에러율 SLO (<5%): ${parseFloat(errRate) < 5 ? '✅ PASS' : '❌ FAIL'}`);
  console.log('====================================================\n');

  return {
    'k6/results/rampup_summary.json': JSON.stringify(data, null, 2),
    stdout: '',
  };
}
