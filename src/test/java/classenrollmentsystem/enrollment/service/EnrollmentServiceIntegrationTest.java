package classenrollmentsystem.enrollment.service;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.course.entity.Course;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.repository.CourseJpaRepository;
import classenrollmentsystem.enrollment.entity.Enrollment;
import classenrollmentsystem.enrollment.entity.EnrollmentStatus;
import classenrollmentsystem.enrollment.repository.EnrollmentJpaRepository;
import classenrollmentsystem.enrollment.service.dto.EnrollmentDto;
import classenrollmentsystem.user.entity.CreatorProfile;
import classenrollmentsystem.user.entity.User;
import classenrollmentsystem.user.repository.CreatorProfileJpaRepository;
import classenrollmentsystem.user.repository.UserJpaRepository;
import classenrollmentsystem.config.RedisTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisTestContainerConfig.class)
class EnrollmentServiceIntegrationTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentJpaRepository enrollmentJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private CreatorProfileJpaRepository creatorProfileJpaRepository;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private User savedUser;
    private User savedCreatorUser;
    private Course savedOpenCourse;
    private Course savedDraftCourse;
    private Course savedClosedCourse;

    @BeforeEach
    void setUp() {
        enrollmentJpaRepository.deleteAll();
        courseJpaRepository.deleteAll();
        creatorProfileJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        redisTemplate.execute((org.springframework.data.redis.connection.RedisConnection connection) -> {
            connection.serverCommands().flushAll();
            return null;
        });

        savedUser = userJpaRepository.save(User.create("user@example.com", "hashedPassword", "User"));

        savedCreatorUser = userJpaRepository.save(User.create("creator@example.com", "hashedPassword", "Creator"));
        CreatorProfile creatorProfile = creatorProfileJpaRepository.save(CreatorProfile.create(savedCreatorUser, "강의 소개"));

        savedOpenCourse = courseJpaRepository.save(Course.builder()
                .creatorProfile(creatorProfile)
                .title("Spring Boot 완벽 가이드")
                .description("초급부터 고급까지")
                .price(new BigDecimal("49900"))
                .maxCapacity(3)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CourseStatus.OPEN)
                .build());

        savedDraftCourse = courseJpaRepository.save(Course.builder()
                .creatorProfile(creatorProfile)
                .title("미공개 강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CourseStatus.DRAFT)
                .build());

        savedClosedCourse = courseJpaRepository.save(Course.builder()
                .creatorProfile(creatorProfile)
                .title("마감된 강의")
                .price(new BigDecimal("20000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CourseStatus.CLOSED)
                .build());
    }

    // ========================
    // enroll
    // ========================

    @Test
    @DisplayName("수강 신청 - OPEN 상태의 강의에 정원이 남아 있으면 수강 신청이 PENDING 상태로 DB에 저장된다")
    void enroll_success() {
        EnrollmentDto result = enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId());

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(enrollmentJpaRepository.findById(result.getId())).isPresent();
    }

    @Test
    @DisplayName("수강 신청 - 본인이 개설한 강의에 신청하면 ENROLLMENT_OWN_COURSE 예외가 발생한다")
    void enroll_fail_own_course() {
        assertThatThrownBy(() -> enrollmentService.enroll(savedCreatorUser.getId(), savedOpenCourse.getId()))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_OWN_COURSE);
    }

    @Test
    @DisplayName("수강 신청 - DRAFT 상태의 강의에 신청하면 ENROLLMENT_COURSE_NOT_OPEN 예외가 발생한다")
    void enroll_fail_draft_course_not_open() {
        assertThatThrownBy(() -> enrollmentService.enroll(savedUser.getId(), savedDraftCourse.getId()))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_COURSE_NOT_OPEN);
    }

    @Test
    @DisplayName("수강 신청 - CLOSED 상태의 강의에 신청하면 ENROLLMENT_COURSE_NOT_OPEN 예외가 발생한다")
    void enroll_fail_closed_course_not_open() {
        assertThatThrownBy(() -> enrollmentService.enroll(savedUser.getId(), savedClosedCourse.getId()))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_COURSE_NOT_OPEN);
    }

    @Test
    @DisplayName("수강 신청 - PENDING 상태로 이미 신청한 강의에 재신청하면 ENROLLMENT_ALREADY_EXISTS 예외가 발생한다")
    void enroll_fail_duplicate() {
        enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId());

        assertThatThrownBy(() -> enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId()))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("수강 신청 - 정원이 가득 찬 강의에 신청하면 ENROLLMENT_CAPACITY_EXCEEDED 예외가 발생한다")
    void enroll_fail_capacity_exceeded() {
        for (int i = 0; i < 3; i++) {
            User other = userJpaRepository.save(
                    User.create("other" + i + "@example.com", "hashedPassword", "Other" + i));
            enrollmentService.enroll(other.getId(), savedOpenCourse.getId());
        }

        assertThatThrownBy(() -> enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId()))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_CAPACITY_EXCEEDED);
    }

    @Test
    @DisplayName("수강 신청 - 취소 후 같은 강의에 다시 신청하면 새로운 PENDING 수강 신청이 생성된다")
    void enroll_success_after_cancel() {
        EnrollmentDto enrolled = enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId());
        enrollmentService.cancel(enrolled.getId(), savedUser.getId());

        EnrollmentDto result = enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId());

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
    }

    // ========================
    // confirm
    // ========================

    @Test
    @DisplayName("결제 확정 - PENDING 상태의 수강 신청은 CONFIRMED 상태로 변경되고 confirmedAt이 기록된다")
    void confirm_success() {
        EnrollmentDto enrolled = enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId());

        EnrollmentDto result = enrollmentService.confirm(enrolled.getId(), savedUser.getId());

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);

        Enrollment saved = enrollmentJpaRepository.findById(result.getId()).orElseThrow();
        assertThat(saved.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("결제 확정 - 본인 소유가 아닌 수강 신청을 확정하려 하면 ENROLLMENT_NOT_OWNER 예외가 발생한다")
    void confirm_fail_not_owner() {
        EnrollmentDto enrolled = enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId());

        assertThatThrownBy(() -> enrollmentService.confirm(enrolled.getId(), savedCreatorUser.getId()))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_NOT_OWNER);
    }

    @Test
    @DisplayName("결제 확정 - 이미 CONFIRMED 상태인 수강 신청을 다시 확정하려 하면 ENROLLMENT_INVALID_STATUS_TRANSITION 예외가 발생한다")
    void confirm_fail_already_confirmed() {
        EnrollmentDto enrolled = enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId());
        enrollmentService.confirm(enrolled.getId(), savedUser.getId());

        assertThatThrownBy(() -> enrollmentService.confirm(enrolled.getId(), savedUser.getId()))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("결제 확정 - CANCELLED 상태인 수강 신청을 확정하려 하면 ENROLLMENT_INVALID_STATUS_TRANSITION 예외가 발생한다")
    void confirm_fail_cancelled() {
        EnrollmentDto enrolled = enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId());
        enrollmentService.cancel(enrolled.getId(), savedUser.getId());

        assertThatThrownBy(() -> enrollmentService.confirm(enrolled.getId(), savedUser.getId()))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_INVALID_STATUS_TRANSITION);
    }

    // ========================
    // cancel
    // ========================

    @Test
    @DisplayName("수강 취소 - PENDING 상태의 수강 신청은 기간 제한 없이 CANCELLED 상태로 변경된다")
    void cancel_success_pending() {
        EnrollmentDto enrolled = enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId());

        EnrollmentDto result = enrollmentService.cancel(enrolled.getId(), savedUser.getId());

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("수강 취소 - 본인 소유가 아닌 수강 신청을 취소하려 하면 ENROLLMENT_NOT_OWNER 예외가 발생한다")
    void cancel_fail_not_owner() {
        EnrollmentDto enrolled = enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId());

        assertThatThrownBy(() -> enrollmentService.cancel(enrolled.getId(), savedCreatorUser.getId()))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_NOT_OWNER);
    }

    @Test
    @DisplayName("수강 취소 - 이미 CANCELLED 상태인 수강 신청을 다시 취소하려 하면 ENROLLMENT_CANCEL_NOT_ALLOWED 예외가 발생한다")
    void cancel_fail_already_cancelled() {
        EnrollmentDto enrolled = enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId());
        enrollmentService.cancel(enrolled.getId(), savedUser.getId());

        assertThatThrownBy(() -> enrollmentService.cancel(enrolled.getId(), savedUser.getId()))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_CANCEL_NOT_ALLOWED);
    }

    // ========================
    // getMyEnrollments
    // ========================

    @Test
    @DisplayName("내 수강 신청 목록 조회 - 신청한 강의가 있으면 해당 목록이 페이지 형태로 반환된다")
    void getMyEnrollments_success() {
        enrollmentService.enroll(savedUser.getId(), savedOpenCourse.getId());

        Page<EnrollmentDto> result = enrollmentService.getMyEnrollments(savedUser.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCourseTitle()).isEqualTo("Spring Boot 완벽 가이드");
    }

    @Test
    @DisplayName("내 수강 신청 목록 조회 - 신청한 강의가 없으면 빈 페이지가 반환된다")
    void getMyEnrollments_empty() {
        Page<EnrollmentDto> result = enrollmentService.getMyEnrollments(savedUser.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }
}
