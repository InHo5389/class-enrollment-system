package classenrollmentsystem.enrollment.service;

import classenrollmentsystem.config.RedisTestContainerConfig;
import classenrollmentsystem.course.entity.Course;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.repository.CourseJpaRepository;
import classenrollmentsystem.enrollment.repository.EnrollmentJpaRepository;
import classenrollmentsystem.user.entity.CreatorProfile;
import classenrollmentsystem.user.entity.User;
import classenrollmentsystem.user.repository.CreatorProfileJpaRepository;
import classenrollmentsystem.user.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Import(RedisTestContainerConfig.class)
class EnrollmentServiceConcurrencyTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private CreatorProfileJpaRepository creatorProfileJpaRepository;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private EnrollmentJpaRepository enrollmentJpaRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private WaitingQueueRepository waitingQueueRepository;

    private static final int NUMBER_OF_THREADS = 100;

    private Course course;
    private List<User> users;

    @BeforeEach
    void setUp() {
        enrollmentJpaRepository.deleteAll();
        courseJpaRepository.deleteAll();
        creatorProfileJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        redisTemplate.execute((RedisConnection connection) -> {
            connection.serverCommands().flushAll();
            return null;
        });

        User creatorUser = userJpaRepository.save(User.create("creator@example.com", "hashedPassword", "Creator"));
        CreatorProfile creatorProfile = creatorProfileJpaRepository.save(CreatorProfile.create(creatorUser, "강의 소개"));

        course = courseJpaRepository.save(Course.builder()
                .creatorProfile(creatorProfile)
                .title("동시성 테스트 강의")
                .description("정원 1명")
                .price(new BigDecimal("10000"))
                .maxCapacity(1)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CourseStatus.OPEN)
                .build());

        users = new ArrayList<>();
        long expiredAt = System.currentTimeMillis() + (5 * 60 * 1000L);
        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            User user = userJpaRepository.save(User.create("user" + i + "@example.com", "hashedPassword", "User" + i));
            users.add(user);
            waitingQueueRepository.addToActiveQueue(course.getId(), user.getId(), expiredAt);
        }
    }

    @Test
    @DisplayName("100명이 동시에 정원 1명 강의에 신청하면 1명만 성공해야 한다 (Redis incr)")
    void enroll_concurrency_only_one_success() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            Long userId = users.get(i).getId();
            executorService.submit(() -> {
                try {
                    enrollmentService.enroll(userId, course.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        System.out.println("테스트 실행 시간: " + (endTime - startTime) + "ms");
        System.out.println("성공: " + successCount.get() + ", 실패: " + failCount.get());

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(NUMBER_OF_THREADS - 1);
        assertThat(enrollmentJpaRepository.count()).isEqualTo(1);
    }

}
