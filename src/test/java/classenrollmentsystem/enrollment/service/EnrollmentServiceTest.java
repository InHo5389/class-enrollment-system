package classenrollmentsystem.enrollment.service;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.course.entity.Course;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.service.CourseRepository;
import classenrollmentsystem.enrollment.entity.Enrollment;
import classenrollmentsystem.enrollment.entity.EnrollmentStatus;
import classenrollmentsystem.enrollment.service.dto.EnrollmentDto;
import classenrollmentsystem.user.entity.CreatorProfile;
import classenrollmentsystem.user.entity.User;
import classenrollmentsystem.user.service.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentCountRepository enrollmentCountRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private User user;
    private User creatorUser;
    private CreatorProfile creatorProfile;
    private Course openCourse;
    private Course draftCourse;
    private Course closedCourse;
    private Enrollment pendingEnrollment;
    private Enrollment confirmedEnrollment;
    private Enrollment cancelledEnrollment;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 4, 28, 12, 0, 0);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(enrollmentService, "cancelDeadlineDays", 7);

        user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("hashedPassword")
                .name("User")
                .build();

        creatorUser = User.builder()
                .id(2L)
                .email("creator@example.com")
                .passwordHash("hashedPassword")
                .name("Creator")
                .build();

        creatorProfile = CreatorProfile.builder()
                .id(1L)
                .user(creatorUser)
                .bio("강의 소개")
                .build();

        openCourse = Course.builder()
                .id(1L)
                .creatorProfile(creatorProfile)
                .title("Spring Boot 완벽 가이드")
                .description("초급부터 고급까지")
                .price(new BigDecimal("49900"))
                .maxCapacity(30)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CourseStatus.OPEN)
                .build();

        draftCourse = Course.builder()
                .id(2L)
                .creatorProfile(creatorProfile)
                .title("미공개 강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CourseStatus.DRAFT)
                .build();

        closedCourse = Course.builder()
                .id(3L)
                .creatorProfile(creatorProfile)
                .title("마감된 강의")
                .price(new BigDecimal("20000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CourseStatus.CLOSED)
                .build();

        pendingEnrollment = Enrollment.builder()
                .id(1L)
                .user(user)
                .course(openCourse)
                .status(EnrollmentStatus.PENDING)
                .build();

        confirmedEnrollment = Enrollment.builder()
                .id(2L)
                .user(user)
                .course(openCourse)
                .status(EnrollmentStatus.CONFIRMED)
                .confirmedAt(FIXED_NOW.minusDays(1))
                .build();

        cancelledEnrollment = Enrollment.builder()
                .id(3L)
                .user(user)
                .course(openCourse)
                .status(EnrollmentStatus.CANCELLED)
                .build();
    }

    @Test
    @DisplayName("수강 신청 - OPEN 상태의 강의에 정원이 남아 있으면 수강 신청이 PENDING 상태로 생성된다")
    void enroll_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(openCourse));
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(1L, 1L, EnrollmentStatus.CANCELLED)).thenReturn(false);
        when(enrollmentCountRepository.incrementEnrollmentCount(1L)).thenReturn(1L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(pendingEnrollment);

        EnrollmentDto result = enrollmentService.enroll(1L, 1L);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(result.getCourseTitle()).isEqualTo("Spring Boot 완벽 가이드");
    }

    @Test
    @DisplayName("수강 신청 - 존재하지 않는 사용자 ID로 요청하면 USER_NOT_FOUND 예외가 발생한다")
    void enroll_fail_user_not_found() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.enroll(999L, 1L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("수강 신청 - 존재하지 않는 강의 ID로 요청하면 COURSE_NOT_FOUND 예외가 발생한다")
    void enroll_fail_course_not_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 999L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("수강 신청 - 본인이 개설한 강의에 신청하면 ENROLLMENT_OWN_COURSE 예외가 발생한다")
    void enroll_fail_own_course() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(creatorUser));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(openCourse));

        assertThatThrownBy(() -> enrollmentService.enroll(2L, 1L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_OWN_COURSE);
    }

    @Test
    @DisplayName("수강 신청 - DRAFT 상태의 강의에 신청하면 ENROLLMENT_COURSE_NOT_OPEN 예외가 발생한다")
    void enroll_fail_draft_course_not_open() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(draftCourse));

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 2L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_COURSE_NOT_OPEN);
    }

    @Test
    @DisplayName("수강 신청 - CLOSED 상태의 강의에 신청하면 ENROLLMENT_COURSE_NOT_OPEN 예외가 발생한다")
    void enroll_fail_closed_course_not_open() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(3L)).thenReturn(Optional.of(closedCourse));

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 3L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_COURSE_NOT_OPEN);
    }

    @Test
    @DisplayName("수강 신청 - PENDING 상태로 이미 신청한 강의에 재신청하면 ENROLLMENT_ALREADY_EXISTS 예외가 발생한다")
    void enroll_fail_already_exists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(openCourse));
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(1L, 1L, EnrollmentStatus.CANCELLED)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 1L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("수강 신청 - 정원이 가득 찬 강의에 신청하면 ENROLLMENT_CAPACITY_EXCEEDED 예외가 발생한다")
    void enroll_fail_capacity_exceeded() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(openCourse));
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(1L, 1L, EnrollmentStatus.CANCELLED)).thenReturn(false);
        when(enrollmentCountRepository.incrementEnrollmentCount(1L)).thenReturn(31L);

        assertThatThrownBy(() -> enrollmentService.enroll(1L, 1L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_CAPACITY_EXCEEDED);
    }

    @Test
    @DisplayName("결제 확정 - PENDING 상태의 수강 신청은 CONFIRMED 상태로 변경된다")
    void confirm_success() {
        Enrollment saved = Enrollment.builder()
                .id(1L)
                .user(user)
                .course(openCourse)
                .status(EnrollmentStatus.CONFIRMED)
                .confirmedAt(LocalDateTime.now())
                .build();

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(pendingEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(saved);

        EnrollmentDto result = enrollmentService.confirm(1L, 1L);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("결제 확정 - 존재하지 않는 수강 신청 ID로 요청하면 ENROLLMENT_NOT_FOUND 예외가 발생한다")
    void confirm_fail_enrollment_not_found() {
        when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.confirm(999L, 1L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("결제 확정 - 본인 소유가 아닌 수강 신청을 확정하려 하면 ENROLLMENT_NOT_OWNER 예외가 발생한다")
    void confirm_fail_not_owner() {
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(pendingEnrollment));

        assertThatThrownBy(() -> enrollmentService.confirm(1L, 99L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_NOT_OWNER);
    }

    @Test
    @DisplayName("결제 확정 - 이미 CONFIRMED 상태인 수강 신청을 다시 확정하려 하면 ENROLLMENT_INVALID_STATUS_TRANSITION 예외가 발생한다")
    void confirm_fail_already_confirmed() {
        when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(confirmedEnrollment));

        assertThatThrownBy(() -> enrollmentService.confirm(2L, 1L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("결제 확정 - CANCELLED 상태인 수강 신청을 확정하려 하면 ENROLLMENT_INVALID_STATUS_TRANSITION 예외가 발생한다")
    void confirm_fail_already_cancelled() {
        when(enrollmentRepository.findById(3L)).thenReturn(Optional.of(cancelledEnrollment));

        assertThatThrownBy(() -> enrollmentService.confirm(3L, 1L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("수강 취소 - PENDING 상태의 수강 신청은 기간 제한 없이 CANCELLED 상태로 변경된다")
    void cancel_success_pending() {
        Enrollment saved = Enrollment.builder()
                .id(1L)
                .user(user)
                .course(openCourse)
                .status(EnrollmentStatus.CANCELLED)
                .build();

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(pendingEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(saved);

        EnrollmentDto result = enrollmentService.cancel(1L, 1L, FIXED_NOW);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("수강 취소 - CONFIRMED 상태이고 결제 확정 후 취소 기한 내라면 CANCELLED 상태로 변경된다")
    void cancel_success_confirmed_within_deadline() {
        Enrollment recentlyConfirmed = Enrollment.builder()
                .id(2L)
                .user(user)
                .course(openCourse)
                .status(EnrollmentStatus.CONFIRMED)
                .confirmedAt(FIXED_NOW.minusDays(1))
                .build();

        Enrollment saved = Enrollment.builder()
                .id(2L)
                .user(user)
                .course(openCourse)
                .status(EnrollmentStatus.CANCELLED)
                .build();

        when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(recentlyConfirmed));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(saved);

        EnrollmentDto result = enrollmentService.cancel(2L, 1L, FIXED_NOW);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("수강 취소 - 존재하지 않는 수강 신청 ID로 요청하면 ENROLLMENT_NOT_FOUND 예외가 발생한다")
    void cancel_fail_enrollment_not_found() {
        when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.cancel(999L, 1L, FIXED_NOW))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("수강 취소 - 본인 소유가 아닌 수강 신청을 취소하려 하면 ENROLLMENT_NOT_OWNER 예외가 발생한다")
    void cancel_fail_not_owner() {
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(pendingEnrollment));

        assertThatThrownBy(() -> enrollmentService.cancel(1L, 99L, FIXED_NOW))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_NOT_OWNER);
    }

    @Test
    @DisplayName("수강 취소 - 이미 CANCELLED 상태인 수강 신청을 다시 취소하려 하면 ENROLLMENT_CANCEL_NOT_ALLOWED 예외가 발생한다")
    void cancel_fail_already_cancelled() {
        when(enrollmentRepository.findById(3L)).thenReturn(Optional.of(cancelledEnrollment));

        assertThatThrownBy(() -> enrollmentService.cancel(3L, 1L, FIXED_NOW))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_CANCEL_NOT_ALLOWED);
    }

    @Test
    @DisplayName("수강 취소 - CONFIRMED 상태이고 결제 확정 후 취소 기한이 지났다면 ENROLLMENT_CANCEL_DEADLINE_EXCEEDED 예외가 발생한다")
    void cancel_fail_confirmed_deadline_exceeded() {
        Enrollment expiredConfirmed = Enrollment.builder()
                .id(2L)
                .user(user)
                .course(openCourse)
                .status(EnrollmentStatus.CONFIRMED)
                .confirmedAt(FIXED_NOW.minusDays(8))
                .build();

        when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(expiredConfirmed));

        assertThatThrownBy(() -> enrollmentService.cancel(2L, 1L, FIXED_NOW))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_CANCEL_DEADLINE_EXCEEDED);
    }

    @Test
    @DisplayName("내 수강 신청 목록 조회 - 수강 신청 내역이 있으면 페이지 형태로 반환된다")
    void getMyEnrollments_success() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Enrollment> enrollmentPage = new PageImpl<>(List.of(pendingEnrollment));

        when(enrollmentRepository.findAllByUserId(1L, pageable)).thenReturn(enrollmentPage);

        Page<EnrollmentDto> result = enrollmentService.getMyEnrollments(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(EnrollmentStatus.PENDING);
    }

    @Test
    @DisplayName("내 수강 신청 목록 조회 - 수강 신청 내역이 없으면 빈 페이지가 반환된다")
    void getMyEnrollments_empty() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Enrollment> emptyPage = new PageImpl<>(List.of());

        when(enrollmentRepository.findAllByUserId(1L, pageable)).thenReturn(emptyPage);

        Page<EnrollmentDto> result = enrollmentService.getMyEnrollments(1L, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
