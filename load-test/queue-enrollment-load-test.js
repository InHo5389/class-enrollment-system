import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
  scenarios: {
    queue_enrollment_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 300 },
        { duration: '5m', target: 500 },
        { duration: '2m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed:   ['rate<0.05'],
  },
};

const BASE_URL = 'http://localhost:8080';

const TOTAL_USERS = 1000;
const HOT_COURSE_ID = 1;
const OTHER_COURSE_IDS = [2, 3, 4, 5];

const POLL_INTERVAL_SECONDS = 3;
const POLL_MAX_ATTEMPTS = 20;

function randomUserId() {
  return Math.floor(Math.random() * TOTAL_USERS) + 1;
}

function pickCourseId() {
  if (Math.random() < 0.9) {
    return HOT_COURSE_ID;
  }
  return OTHER_COURSE_IDS[Math.floor(Math.random() * OTHER_COURSE_IDS.length)];
}

function enterQueue(courseId, userId) {
  return http.post(
    `${BASE_URL}/api/queue/courses/${courseId}/enter`,
    null,
    { headers: { 'X-User-Id': String(userId) } },
  );
}

function pollQueueStatus(courseId, userId) {
  return http.get(
    `${BASE_URL}/api/queue/courses/${courseId}/status`,
    { headers: { 'X-User-Id': String(userId) } },
  );
}

function enrollCourse(courseId, userId) {
  return http.post(
    `${BASE_URL}/api/enrollments`,
    JSON.stringify({ courseId }),
    { headers: { 'Content-Type': 'application/json', 'X-User-Id': String(userId) } },
  );
}

export default function () {
  const userId = randomUserId();
  const courseId = pickCourseId();

  // 1. 강의 목록 조회
  http.get(`${BASE_URL}/api/courses?status=OPEN`);
  sleep(0.5);

  // 2. 강의 상세 조회
  http.get(
    `${BASE_URL}/api/courses/${courseId}`,
    { headers: { 'X-User-Id': String(userId) } },
  );
  sleep(1);

  // 3. 70% 이탈
  if (Math.random() < 0.70) {
    sleep(1);
    return;
  }

  // 4. 대기열 진입
  const enterRes = enterQueue(courseId, userId);
  if (enterRes.status !== 200) {
    sleep(1);
    return;
  }

  const enterBody = JSON.parse(enterRes.body);

  // 5. 즉시 ACTIVE면 바로 수강 신청, WAITING이면 폴링
  let isActive = enterBody.status === 'ACTIVE';

  if (!isActive) {
    for (let attempt = 0; attempt < POLL_MAX_ATTEMPTS; attempt++) {
      sleep(POLL_INTERVAL_SECONDS);

      const statusRes = pollQueueStatus(courseId, userId);
      if (statusRes.status !== 200) {
        break;
      }

      const statusBody = JSON.parse(statusRes.body);
      if (statusBody.status === 'ACTIVE') {
        isActive = true;
        break;
      }

      if (statusBody.status === 'NOT_IN_QUEUE') {
        break;
      }
    }
  }

  if (!isActive) {
    sleep(1);
    return;
  }

  // 6. 수강 신청
  const enrollRes = enrollCourse(courseId, userId);

  check(enrollRes, {
    'enrollment succeeded': (r) => r.status === 200 || r.status === 201,
  });

  if (enrollRes.status !== 200 && enrollRes.status !== 201) {
    sleep(1);
    return;
  }

  const enrollmentId = JSON.parse(enrollRes.body).id;
  sleep(0.5);

  // 7. 결제 확정
  http.post(
    `${BASE_URL}/api/enrollments/${enrollmentId}/confirm`,
    null,
    { headers: { 'X-User-Id': String(userId) } },
  );

  // 8. 5% 취소
  if (Math.random() < 0.05) {
    sleep(1);
    http.del(
      `${BASE_URL}/api/enrollments/${enrollmentId}`,
      null,
      { headers: { 'X-User-Id': String(userId) } },
    );
  }

  sleep(1);
}
