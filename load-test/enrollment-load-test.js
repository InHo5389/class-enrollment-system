import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
  scenarios: {
    enrollment_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5m', target: 500 },  // 5분에 걸쳐 0 → 100 VU 점진적 증가
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'],  // 95%가 2초 이내
    http_req_failed:   ['rate<0.05'],   // 실패율 5% 미만
  },
};

const BASE_URL = 'http://localhost:8080';

const TOTAL_USERS = 1000;
const HOT_COURSE_ID = 1;
const OTHER_COURSE_IDS = [2, 3, 4, 5];

function randomUserId() {
  return Math.floor(Math.random() * TOTAL_USERS) + 1;
}

function pickCourseId() {
  if (Math.random() < 0.9) {
    return HOT_COURSE_ID;
  }
  return OTHER_COURSE_IDS[Math.floor(Math.random() * OTHER_COURSE_IDS.length)];
}

export default function () {
  const userId = randomUserId();
  const courseId = pickCourseId();

  // 1. 강의 목록 조회
  http.get(`${BASE_URL}/api/courses?status=OPEN`);
  sleep(0.5);

  // 2. 인기 강의 상세 조회 (90% 트래픽이 HOT_COURSE_ID로 집중)
  http.get(
    `${BASE_URL}/api/courses/${courseId}`,
    { headers: { 'X-User-Id': String(userId) } },
  );
  sleep(1);

  // 3. 분기: 70% 이탈 / 25% 수강신청+확정 / 5% 수강신청+확정+취소
  const random = Math.random();

  if (random < 0.70) {
    sleep(1);
    return;
  }

  // 수강신청
  const enrollRes = http.post(
    `${BASE_URL}/api/enrollments`,
    JSON.stringify({ courseId }),
    { headers: { 'Content-Type': 'application/json', 'X-User-Id': String(userId) } },
  );

  if (enrollRes.status !== 200 && enrollRes.status !== 201) {
    sleep(1);
    return;
  }

  const enrollmentId = JSON.parse(enrollRes.body).id;
  sleep(0.5);

  // 결제 확정
  http.post(
    `${BASE_URL}/api/enrollments/${enrollmentId}/confirm`,
    null,
    { headers: { 'X-User-Id': String(userId) } },
  );

  if (random >= 0.95) {
    // 5% - 결제 확정 후 취소
    sleep(1);
    http.del(
      `${BASE_URL}/api/enrollments/${enrollmentId}`,
      null,
      { headers: { 'X-User-Id': String(userId) } },
    );
  }

  sleep(1);
}
