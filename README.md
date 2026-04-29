# Class Enrollment System

## 📚 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [기술 스택](#기술-스택)
3. [실행 방법](#실행-방법)
4. [요구사항 해석 및 가정](#요구사항-해석-및-가정)
5. [설계 결정과 이유](#설계-결정과-이유)
6. [미구현 / 제약사항](#미구현--제약사항)
7. [AI 활용 범위](#ai-활용-범위)
8. [API 목록 및 예시](#api-목록-및-예시)
9. [데이터 모델](#데이터-모델)
10. [테스트 환경](#테스트-환경)

---

## 프로젝트 개요

Spring Boot 기반의 수강 신청 관리 시스템입니다. 크리에이터(강사)가 강의를 개설하고, 클래스메이트(수강생)가 수강 신청·결제 확정·취소까지 처리할 수 있습니다.


| 항목 | 내용 |
|------|------|
| 프로젝트 유형 | CRUD + 비즈니스 규칙 + 동시성 제어 |
| 핵심 기능 | 강의 관리, 수강 신청/확정/취소, 대기열, 캐시 |
| 동시성 전략 | Redis Atomic Increment + 대기열 토큰 검증 |
| 캐시 전략 | Redis 강의 상세 캐시 + Cache Penetration / Hot Key 방어 |

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Framework | Spring Boot 3.5.14 |
| Language | Java 17 |
| Build | Gradle |
| Database | MySQL 8.0 |
| Cache / Queue | Redis 7.2 |
| ORM | Spring Data JPA + Hibernate |
| 인증 | 커스텀 인터셉터 + `X-User-Id` 헤더 |
| 모니터링 | Spring Actuator + Micrometer + Prometheus + Grafana |
| 부하 테스트 | k6 |
| 테스트 | JUnit 5 + Mockito + AssertJ + H2 + Testcontainers |

---

## 실행 방법

### 사전 요구사항

- Java 17
- Docker & Docker Compose

### 1. 인프라 실행

```bash
docker-compose up -d
```

Spring(8080), MySQL(3306), Redis(6379), Prometheus(9090), Grafana(3000)가 함께 실행됩니다.


### 2. 모니터링 접속

| 서비스 | 주소 | 계정 |
|--------|------|------|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | - |
| Actuator | http://localhost:8080/actuator | - |

## 요구사항 해석 및 가정

### 인증 방식

별도 인증 서버가 없으므로, 클라이언트가 `X-User-Id` 헤더에 사용자 ID를 직접 전달하는 방식으로 대체했습니다. 실제 환경이라면 JWT 검증 또는 API Gateway가 이 헤더를 주입할 것입니다.

### 결제 연동

외부 결제 시스템 연동 없이 `POST /api/enrollments/{id}/confirm` 호출만으로 `PENDING → CONFIRMED` 상태 전이를 처리합니다.

### 취소 가능 기간

`CONFIRMED` 상태에서 결제 확정 후 7일 이내에만 취소할 수 있습니다. 기간은 `application.yaml`의 `enrollment.cancel-deadline-days`로 조정 가능합니다.

### 수강 인원 카운트

MySQL 행 잠금 대신 Redis `INCR`/`DECR`를 사용해 실시간 수강 인원을 관리합니다. 정원 초과 시 카운트를 즉시 롤백합니다.

### 대기열 즉시 통과 임계값

활성 대기열 인원이 2,100명 미만이고 대기 중인 사용자가 없을 때, 대기열을 거치지 않고 즉시 활성 토큰을 발급합니다. 이는 비인기 강의에 대한 불필요한 대기를 방지하기 위한 설계입니다.

---

## 설계 결정과 이유

### 1. 의존성 역전(DIP) 아키텍처

Service 계층은 JPA 구현체가 아닌 도메인 레이어의 Repository 인터페이스에만 의존합니다. 이를 통해 JPA를 다른 구현체로 교체하더라도 비즈니스 로직을 수정하지 않아도 됩니다.

```
Service → Repository Interface (domain layer)
               ↑ implements
         RepositoryImpl (infrastructure layer, JPA 사용)
```

### 2. 정원 관리: Redis Atomic Increment

정원 관리에서 가장 먼저 고려한 세 가지 방식과 각각의 문제점입니다.

#### synchronized 사용 문제점

JVM 레벨의 락이므로 단일 서버에서는 Race Condition을 막을 수 있습니다. 그러나 서버가 여러 대로 수평 확장되는 순간 각 서버의 메모리를 공유하지 않아 다시 Race Condition이 발생합니다.

#### 비관적 락(Pessimistic Lock) 문제점

`SELECT ... FOR UPDATE`로 DB 행 자체에 락을 걸면 정확하게 동시성을 제어할 수 있습니다. 그러나 정원 확인 시점마다 모든 요청이 같은 행에 대한 락을 경쟁하므로 락 대기 큐가 길어지고 DB 커넥션이 고갈됩니다. 특히 수강 신청이 폭발적으로 몰리는 순간 모든 스레드가 락 해제를 기다리며 응답 지연이 급격히 증가합니다.

#### 낙관적 락(Optimistic Lock) 문제점

`@Version` 필드로 충돌 시 재시도하는 방식입니다. 충돌이 드문 상황에서는 효율적이지만, 수강 신청처럼 동시 충돌이 높은 환경에서는 대부분의 요청이 `OptimisticLockException`으로 실패하고 재시도를 반복해 오히려 DB 부하가 더 높아집니다.

#### Redis Atomic Increment 선택 이유

Redis `INCR`는 싱글 스레드 기반으로 명령 자체가 원자적입니다. 별도의 락 없이 카운트를 증가시키고 즉시 결과를 반환하므로 DB 부하 없이 수평 확장 환경에서도 정원 초과를 방지할 수 있습니다. 정원 초과 시에는 `DECR`로 즉시 롤백합니다.

### 3. 대기열: Redis Sorted Set

대기열은 두 개의 Sorted Set으로 구성됩니다.

- `queue:waiting:course:{id}` — 진입 타임스탬프를 score로 사용, FIFO 순서 보장
- `queue:active:course:{id}` — 토큰 만료 시각을 score로 사용, 만료 토큰 자동 감지

스케줄러가 60초마다 대기열에서 활성 큐로 최대 2,100명을 이동시킵니다. 대기열 → 활성 큐 이동은 Lua 스크립트로 원자적으로 처리합니다.

### Waiting Queue에 Sorted Set을 선택한 이유

대기열에서 가장 중요한 것은 선착순 보장입니다. 진입 시점의 timestamp를 score로 저장하면 `ZRANGE`로 항상 먼저 들어온 순서대로 꺼낼 수 있고, `ZRANK`로 특정 사용자의 현재 대기 순위를 O(log N)에 조회할 수 있습니다.

일반 List나 Queue로도 순서는 보장되지만 "내가 몇 번째야?"라는 순위 조회를 O(1)~O(N)으로 처리할 수 없습니다. Sorted Set은 이를 O(log N)에 해결합니다.

### Active Queue에 Sorted Set을 선택한 이유

Active Queue는 사용자마다 만료 시간이 다릅니다. 스케줄러가 1분마다 실행되므로 먼저 진입한 사람과 나중에 진입한 사람의 토큰 만료 시각이 다릅니다.

만료 시간을 score로 저장하면 두 가지를 해결할 수 있습니다.

- `isInActiveQueue()`에서 `score > now` 조건으로 요청 시점에 즉시 만료 여부를 판단할 수 있습니다.
- `ZREMRANGEBYSCORE 0 now`로 만료된 토큰 전체를 한 번의 명령으로 일괄 제거할 수 있습니다.

Redis TTL을 키 단위로 걸면 강의 전체 Active Queue가 통째로 사라지는 문제가 생기고, String 키로 사용자마다 분리하면 `SCAN` 패턴 조회가 필요해 운영 환경에서 성능 문제가 생깁니다. Sorted Set은 이 두 문제를 모두 피하면서 만료 관리를 할 수 있는 구조입니다.

### 대기열 저장소로 Redis(In-Memory)를 선택한 이유

대기열 시스템은 수강 신청이 시작되는 순간 수만 건의 요청이 짧은 시간에 폭발적으로 몰리는 'Peak Time'에 최적화되어야 합니다. Disk 기반의 DB(MySQL 등) 대신 Redis(In-Memory)를 선택한 이유는 다음과 같습니다.

**1. 성능 (Latency & Throughput)**
* **Disk I/O 병목 제거**: Disk 기반 DB는 물리적인 디스크 I/O로 인해 병목이 발생할 수 있습니다. 반면, 메모리 기반인 Redis는 마이크로초(µs) 단위의 지연 시간으로 데이터를 처리하므로, 수만 명의 사용자가 동시에 진입해도 병목 없이 대기열 순서를 관리할 수 있습니다.

**2. 데이터의 휘발성 (Transient Data)**
* **임시 데이터 관리**: 대기열 데이터는 '토큰 발급'이라는 일시적인 목적을 가집니다. 사용자가 활성 토큰을 발급받거나 수강 신청이 완료되면 해당 데이터는 삭제되어야 합니다. 영구 저장을 위한 Disk I/O는 불필요한 오버헤드입니다. Redis는 TTL(Time To Live)을 통해 이런 휘발성 데이터를 자동으로 관리하는 데 최적화되어 있습니다.

**3. RDBMS의 잠금(Locking) 문제 회피**
* **동시성 처리**: RDBMS로 대기열을 구현하면 수많은 사용자가 동시에 데이터를 조회하고 삭제(Delete)할 때 테이블/행 단위의 Lock이 발생합니다. 이는 수강 신청 같은 고트래픽 상황에서 DB 커넥션 고갈과 응답 지연으로 이어집니다. Redis는 단일 스레드 기반의 원자적 연산을 제공하여, Lock 경합 없이 수천 건의 초당 트랜잭션을 안정적으로 처리합니다.


### 분당 2,100명 한계값 산정 근거

대기열 없이 시스템을 먼저 부하 테스트하여 안정적인 한계 TPS를 측정했습니다. 로컬 환경 기준으로 200 TPS가 측정되었습니다.

수강 신청 시나리오에서 사용자 한 명은 최소 4개의 API를 호출합니다.

```
강의 목록 조회 → 강의 상세 조회 → 수강 신청 → 결제 확정
```

```
200 TPS ÷ 4 API/user = 50명/초
50명/초 × 60초 = 3,000명/분
3,000명 × 70% 안전 마진 = 2,100명/분
```

스케줄러 실행 시점에 대기 중인 활성 강의 수를 세어 2,100명을 균등 분배합니다.

| 활성 강의 수 | 강의당 분당 처리량 |
|-------------|-----------------|
| 3개 | 700명/분 |
| 5개 | 420명/분 |
| 10개 | 210명/분 |

강의 수가 늘어날수록 강의당 처리량이 줄어드는 구조이며, 서버 스케일 업/아웃 시 이 기준값을 조정해야 합니다.

### 4. 강의 상세 캐시: 도입 이유 및 Cache Penetration / Hot Key 방어

#### 캐시 도입 이유

강의 상세 조회는 수강 신청 전 반드시 거치는 경로로, 인기 강의에 수강 신청이 몰리는 시점에는 동일한 강의 ID로 수천 건의 조회가 동시에 발생합니다. 강의 상세 데이터는 상태 변경 전까지 변하지 않으므로 캐시 효과가 높고, 캐시 없이 모든 요청이 DB로 전달되면 수강 신청 처리보다 조회 쿼리가 DB를 먼저 포화시킬 수 있습니다.

#### Cache Penetration / Hot Key 방어

| 문제 | 원인 | 해결 방법 |
|------|------|-----------|
| Cache Penetration | 존재하지 않는 ID로 대량 요청 시 캐시 미스가 계속 발생해 모든 요청이 DB로 전달됨 | DB 조회 결과가 없을 때 `__NULL__` 센티널 값을 60초 TTL로 캐싱 |
| Hot Key | 인기 강의 키가 만료되는 순간 수천 개의 요청이 동시에 캐시 미스되어 DB로 몰림 | Redis `SETNX` 기반 분산 락 + Double Checked Locking으로 DB 조회 스레드를 1개로 제한 |

캐시 키 구조:
- `course:detail:{id}` — 강의 상세 데이터 (TTL: 1800초)
- `course:null:{id}` — 존재하지 않는 강의 센티널 (TTL: 60초)
- `course:lock:{id}` — 분산 락 (TTL: 3초)

강의 상태가 변경될 때 해당 키와 null 캐시를 함께 무효화합니다.

### 5. 인증/인가: 인터셉터 + 커스텀 어노테이션

Spring Security 없이 `AuthenticationInterceptor`와 두 개의 어노테이션으로 접근 정책을 선언적으로 관리합니다.

| 어노테이션 | 의미 |
|-----------|------|
| `@PublicApi` | 인증 불필요 |
| (없음) | `X-User-Id` 헤더 필요 |
| `@RequireCreator` | `X-User-Id` + 크리에이터 프로필 존재 필요 |

---

## 미구현 / 제약사항

- **JWT 인증**: 실제 토큰 검증 없이 헤더 값을 신뢰하는 구조입니다.
- **결제 연동**: 외부 PG 연동 없이 상태 변경으로 대체합니다.
- **Redis 장애 대응**: Redis 장애 시 캐시·카운트·대기열 모두 영향을 받으며 센티널, 서킷 브레이커가 구현되어 있지 않습니다.

---

## AI 활용 범위

> AI로 개발이 편리해진건 맞지만 기획과 설계 그리고 의사 결정에 대한 건 개발자의 몫이라고 생각합니다!

| 활용 영역  | 내용                                 |
|--------|------------------------------------|
| SKILLS | 테스트 코드 양식, 코드 스타일, 커밋 단위나누기, 코드 리뷰 |
| 코드 구현  | 기획한 대로 코드 구현, 작성된 코드 보며 확인         |
| 문서화    | README, 설계 문서 작성                   |

---

## API 목록 및 예시

모든 인증 필요 API는 `X-User-Id: {userId}` 헤더가 필요합니다.

### 사용자 (User)

#### 회원가입
```
POST /api/users/sign-up
```
```json
// Request
{ "email": "user@example.com", "password": "pass1234", "name": "홍길동" }

// Response 200
{ "id": 1, "email": "user@example.com", "name": "홍길동" }
```

#### 로그인
```
POST /api/users/login
```
```json
// Request
{ "email": "user@example.com", "password": "pass1234" }

// Response 200
{ "id": 1, "email": "user@example.com", "name": "홍길동" }
```

#### 내 프로필 조회
```
GET /api/users/me
X-User-Id: 1
```

#### 크리에이터 등록
```
POST /api/users/creator
X-User-Id: 1
```
```json
// Request
{ "bio": "10년 경력의 백엔드 개발자입니다." }

// Response 200
{ "id": 1, "userId": 1, "bio": "10년 경력의 백엔드 개발자입니다." }
```

---

### 강의 (Course)

#### 강의 등록 (크리에이터 전용)
```
POST /api/courses
X-User-Id: 1
```
```json
// Request
{
  "title": "Spring Boot 완벽 가이드",
  "description": "초급부터 고급까지",
  "price": 49900,
  "maxCapacity": 30,
  "startDate": "2026-05-01",
  "endDate": "2026-06-30"
}

// Response 200
{
  "id": 1,
  "title": "Spring Boot 완벽 가이드",
  "status": "DRAFT",
  "maxCapacity": 30,
  "currentEnrollment": 0
}
```

#### 강의 목록 조회
```
GET /api/courses?status=OPEN&page=0&size=10
```
```json
// Response 200
{
  "content": [{ "id": 1, "title": "Spring Boot 완벽 가이드", "status": "OPEN", ... }],
  "totalElements": 1,
  "totalPages": 1
}
```

#### 강의 상세 조회
```
GET /api/courses/{courseId}
X-User-Id: 1  (선택, DRAFT 강의는 작성자만 조회 가능)
```
```json
// Response 200
{
  "id": 1,
  "title": "Spring Boot 완벽 가이드",
  "creatorName": "홍길동",
  "status": "OPEN",
  "maxCapacity": 30,
  "currentEnrollment": 5,
  "startDate": "2026-05-01",
  "endDate": "2026-06-30"
}
```

#### 강의 상태 변경
```
PATCH /api/courses/{courseId}/status
X-User-Id: 1
```
```json
// Request
{ "status": "OPEN" }
// 허용 전이: DRAFT → OPEN → CLOSED
```

---

### 수강 신청 (Enrollment)

#### 수강 신청
```
POST /api/enrollments
X-User-Id: 2
```
```json
// Request
{ "courseId": 1 }

// Response 200
{ "id": 1, "courseId": 1, "status": "PENDING" }
```

> 대기열이 활성화된 경우, 먼저 대기열에 진입하여 `ACTIVE` 토큰을 발급받은 후 수강 신청해야 합니다.

#### 결제 확정
```
POST /api/enrollments/{enrollmentId}/confirm
X-User-Id: 2
```
```json
// Response 200
{ "id": 1, "status": "CONFIRMED", "confirmedAt": "2026-04-29T10:00:00" }
```

#### 수강 취소
```
DELETE /api/enrollments/{enrollmentId}
X-User-Id: 2
```
```json
// Response 200
{ "id": 1, "status": "CANCELLED" }
// CONFIRMED 상태는 결제 후 7일 이내에만 취소 가능
```

#### 내 수강 신청 목록 조회
```
GET /api/enrollments/me?page=0&size=10
X-User-Id: 2
```

#### 강의별 수강생 목록 조회 (크리에이터 전용)
```
GET /api/enrollments/courses/{courseId}?page=0&size=10
X-User-Id: 1
// CONFIRMED 상태 수강생만 반환
```

---

### 대기열 (Queue)

#### 대기열 진입
```
POST /api/queue/courses/{courseId}/enter
X-User-Id: 2
```
```json
// Response 200 (즉시 통과)
{ "status": "ACTIVE", "rank": 0, "message": "입장이 허가되었습니다. 5분 이내에 수강 신청을 완료해 주세요." }

// Response 200 (대기 중)
{ "status": "WAITING", "rank": 1, "totalWaiting": 150, "message": "현재 1번째 대기 중입니다. (총 대기 인원: 150명)" }
```

#### 대기열 상태 조회
```
GET /api/queue/courses/{courseId}/status
X-User-Id: 2
```
```json
// Response 200
{ "status": "ACTIVE" | "WAITING" | "NOT_IN_QUEUE", "rank": 0, "totalWaiting": 0 }
```

---

### 에러 응답 형식

```json
{ "status": 400, "message": "이미 신청한 강의입니다." }
```

| 코드 | 상황 |
|------|------|
| 400 | 잘못된 요청 (중복 신청, 정원 초과, 상태 전이 오류 등) |
| 401 | X-User-Id 헤더 누락 |
| 403 | 권한 없음 (크리에이터 아님, 대기열 토큰 없음 등) |
| 404 | 리소스 없음 |

---

## 데이터 모델 설명

### ERD 요약

```
users
  id, email, password_hash, name, created_at, updated_at

creator_profiles
  id, user_id (FK → users), bio, created_at, updated_at

courses
  id, creator_profile_id (FK → creator_profiles),
  title, description, price, max_capacity,
  start_date, end_date, status(DRAFT|OPEN|CLOSED),
  created_at, updated_at

enrollments
  id, user_id (FK → users), course_id (FK → courses),
  status(PENDING|CONFIRMED|CANCELLED), confirmed_at,
  created_at, updated_at
```

### 상태 전이

```
강의(Course):     DRAFT → OPEN → CLOSED
수강 신청:         PENDING → CONFIRMED → CANCELLED
                  PENDING → CANCELLED
```

### Redis 데이터 구조

| 키 패턴 | 타입 | 용도 |
|---------|------|------|
| `enrollment:count:{courseId}` | String | 강의별 실시간 수강 인원 |
| `queue:waiting:course:{courseId}` | Sorted Set | 대기열 (score: 진입 타임스탬프) |
| `queue:active:course:{courseId}` | Sorted Set | 활성 토큰 (score: 만료 타임스탬프) |
| `course:detail:{courseId}` | String (JSON) | 강의 상세 캐시 (TTL: 1800s) |
| `course:null:{courseId}` | String | 존재하지 않는 강의 센티널 (TTL: 60s) |
| `course:lock:{courseId}` | String | 분산 락 (TTL: 3s) |

### 테스트 환경

- **H2** 인메모리 DB (`application-test.yaml`)
- **Testcontainers** Redis: 대기열/카운트 관련 통합 테스트에서 실제 Redis 컨테이너를 사용합니다. Docker가 실행 중이어야 합니다.

### 테스트 구조

| 레이어 | 파일 | 방식 |
|--------|------|------|
| Service 단위 | `*ServiceTest` | Mockito Mock |
| Service 통합 | `*ServiceIntegrationTest` | H2 + 실제 트랜잭션 |
| Controller | `*ControllerTest` | MockMvc + MockitoBean |
| 동시성 | `EnrollmentServiceConcurrencyTest` | CountDownLatch + ExecutorService |
| 스케줄러 | `QueuePromotionSchedulerTest` | Testcontainers Redis |
