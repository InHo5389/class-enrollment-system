package classenrollmentsystem.course.service;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.course.entity.Course;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.service.dto.CourseDetailDto;
import classenrollmentsystem.course.service.dto.CourseDto;
import classenrollmentsystem.course.service.dto.CreateCourseDto;
import classenrollmentsystem.enrollment.service.EnrollmentCountRepository;
import classenrollmentsystem.user.entity.CreatorProfile;
import classenrollmentsystem.user.entity.User;
import classenrollmentsystem.user.service.CreatorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CreatorProfileRepository creatorProfileRepository;

    @Mock
    private EnrollmentCountRepository enrollmentCountRepository;

    @InjectMocks
    private CourseService courseService;

    private User user;
    private CreatorProfile creatorProfile;
    private Course course;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("creator@example.com")
                .passwordHash("hashedPassword")
                .name("Creator")
                .build();

        creatorProfile = CreatorProfile.builder()
                .id(1L)
                .user(user)
                .bio("강의 소개")
                .build();

        course = Course.builder()
                .id(1L)
                .creatorProfile(creatorProfile)
                .title("Spring Boot 완벽 가이드")
                .description("초급부터 고급까지")
                .price(new BigDecimal("49900"))
                .maxCapacity(30)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CourseStatus.DRAFT)
                .build();
    }

    // ========================
    // createCourse
    // ========================

    @Test
    @DisplayName("강의 등록 - 크리에이터가 유효한 정보로 강의를 등록하면 DRAFT 상태로 저장된다")
    void createCourse_success() {
        CreateCourseDto dto = CreateCourseDto.builder()
                .userId(1L)
                .title("Spring Boot 완벽 가이드")
                .description("초급부터 고급까지")
                .price(new BigDecimal("49900"))
                .maxCapacity(30)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        when(creatorProfileRepository.findByUserId(1L)).thenReturn(Optional.of(creatorProfile));
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        CourseDto result = courseService.createCourse(dto);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Spring Boot 완벽 가이드");
        assertThat(result.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(result.getCreatorName()).isEqualTo("Creator");
    }

    @Test
    @DisplayName("강의 등록 - 크리에이터 프로필이 없는 사용자가 등록하면 NOT_CREATOR 예외가 발생한다")
    void createCourse_fail_not_creator() {
        CreateCourseDto dto = CreateCourseDto.builder()
                .userId(99L)
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        when(creatorProfileRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.createCourse(dto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_CREATOR);
    }

    @Test
    @DisplayName("강의 등록 - 정원이 0이면 INVALID_COURSE_CAPACITY 예외가 발생한다")
    void createCourse_fail_capacity_zero() {
        CreateCourseDto dto = CreateCourseDto.builder()
                .userId(1L)
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(0)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        assertThatThrownBy(() -> courseService.createCourse(dto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_COURSE_CAPACITY);
    }

    @Test
    @DisplayName("강의 등록 - 정원이 음수이면 INVALID_COURSE_CAPACITY 예외가 발생한다")
    void createCourse_fail_capacity_negative() {
        CreateCourseDto dto = CreateCourseDto.builder()
                .userId(1L)
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(-1)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        assertThatThrownBy(() -> courseService.createCourse(dto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_COURSE_CAPACITY);
    }

    @Test
    @DisplayName("강의 등록 - 종료일이 시작일보다 이전이면 INVALID_COURSE_PERIOD 예외가 발생한다")
    void createCourse_fail_end_before_start() {
        CreateCourseDto dto = CreateCourseDto.builder()
                .userId(1L)
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 6, 30))
                .endDate(LocalDate.of(2026, 5, 1))
                .build();

        assertThatThrownBy(() -> courseService.createCourse(dto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_COURSE_PERIOD);
    }

    @Test
    @DisplayName("강의 등록 - 종료일과 시작일이 같으면 INVALID_COURSE_PERIOD 예외가 발생한다")
    void createCourse_fail_end_equals_start() {
        CreateCourseDto dto = CreateCourseDto.builder()
                .userId(1L)
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 1))
                .build();

        assertThatThrownBy(() -> courseService.createCourse(dto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_COURSE_PERIOD);
    }

    // ========================
    // getCourses
    // ========================

    @Test
    @DisplayName("강의 목록 조회 - status가 null이면 전체 강의를 조회하고 Redis 수강 인원을 반영한다")
    void getCourses_success_no_filter() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Course> coursePage = new PageImpl<>(List.of(course));

        when(courseRepository.findAll(pageable)).thenReturn(coursePage);
        when(enrollmentCountRepository.getEnrollmentCount(1L)).thenReturn(5);

        Page<CourseDto> result = courseService.getCourses(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Spring Boot 완벽 가이드");
        assertThat(result.getContent().get(0).getCurrentEnrollment()).isEqualTo(5);
    }

    @Test
    @DisplayName("강의 목록 조회 - status 필터가 있으면 해당 상태의 강의만 조회한다")
    void getCourses_success_with_status_filter() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Course> coursePage = new PageImpl<>(List.of(course));

        when(courseRepository.findAllByStatus(CourseStatus.DRAFT, pageable)).thenReturn(coursePage);
        when(enrollmentCountRepository.getEnrollmentCount(anyLong())).thenReturn(0);

        Page<CourseDto> result = courseService.getCourses(CourseStatus.DRAFT, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    @DisplayName("강의 목록 조회 - 강의가 없으면 빈 페이지를 반환한다")
    void getCourses_empty() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(courseRepository.findAll(pageable)).thenReturn(Page.empty());

        Page<CourseDto> result = courseService.getCourses(null, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // ========================
    // getCourse
    // ========================

    @Test
    @DisplayName("강의 상세 조회 - 강의 작성자는 본인의 DRAFT 강의를 조회할 수 있다")
    void getCourse_success_owner_sees_draft() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentCountRepository.getEnrollmentCount(1L)).thenReturn(3);

        CourseDetailDto result = courseService.getCourse(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(result.getCreatorName()).isEqualTo("Creator");
        assertThat(result.getCreatorBio()).isEqualTo("강의 소개");
        assertThat(result.getCurrentEnrollment()).isEqualTo(3);
    }

    @Test
    @DisplayName("강의 상세 조회 - 타인이 DRAFT 강의를 조회하면 COURSE_NOT_FOUND 예외가 발생한다")
    void getCourse_fail_draft_not_owner() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.getCourse(1L, 99L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("강의 상세 조회 - userId가 null이면 DRAFT 강의는 COURSE_NOT_FOUND 예외가 발생한다")
    void getCourse_fail_draft_anonymous() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.getCourse(1L, null))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("강의 상세 조회 - OPEN 강의는 누구나 조회할 수 있다")
    void getCourse_success_open_anonymous() {
        Course openCourse = Course.builder()
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

        when(courseRepository.findById(1L)).thenReturn(Optional.of(openCourse));
        when(enrollmentCountRepository.getEnrollmentCount(1L)).thenReturn(0);

        CourseDetailDto result = courseService.getCourse(1L, null);

        assertThat(result.getStatus()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("강의 상세 조회 - 존재하지 않는 강의를 조회하면 COURSE_NOT_FOUND 예외가 발생한다")
    void getCourse_fail_not_found() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourse(999L, 1L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    // ========================
    // changeCourseStatus
    // ========================

    @Test
    @DisplayName("강의 상태 변경 - 작성자가 DRAFT에서 OPEN으로 변경하면 Redis 수강 인원이 반영된 결과를 반환한다")
    void changeCourseStatus_success_draft_to_open() {
        Course openCourse = Course.builder()
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

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenReturn(openCourse);
        when(enrollmentCountRepository.getEnrollmentCount(1L)).thenReturn(0);

        CourseDto result = courseService.changeCourseStatus(1L, 1L, CourseStatus.OPEN);

        assertThat(result.getStatus()).isEqualTo(CourseStatus.OPEN);
        assertThat(result.getCurrentEnrollment()).isEqualTo(0);
    }

    @Test
    @DisplayName("강의 상태 변경 - 작성자가 아닌 사용자가 변경하면 COURSE_NOT_OWNER 예외가 발생한다")
    void changeCourseStatus_fail_not_owner() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.changeCourseStatus(1L, 99L, CourseStatus.OPEN))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.COURSE_NOT_OWNER);
    }

    @Test
    @DisplayName("강의 상태 변경 - 존재하지 않는 강의를 변경하면 COURSE_NOT_FOUND 예외가 발생한다")
    void changeCourseStatus_fail_not_found() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.changeCourseStatus(999L, 1L, CourseStatus.OPEN))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("강의 상태 변경 - CLOSED 강의를 OPEN으로 변경하면 INVALID_COURSE_STATUS_TRANSITION 예외가 발생한다")
    void changeCourseStatus_fail_closed_to_open() {
        Course closedCourse = Course.builder()
                .id(1L)
                .creatorProfile(creatorProfile)
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CourseStatus.CLOSED)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(closedCourse));

        assertThatThrownBy(() -> courseService.changeCourseStatus(1L, 1L, CourseStatus.OPEN))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_COURSE_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("강의 상태 변경 - OPEN 강의를 DRAFT로 변경하면 INVALID_COURSE_STATUS_TRANSITION 예외가 발생한다")
    void changeCourseStatus_fail_open_to_draft() {
        Course openCourse = Course.builder()
                .id(1L)
                .creatorProfile(creatorProfile)
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CourseStatus.OPEN)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(openCourse));

        assertThatThrownBy(() -> courseService.changeCourseStatus(1L, 1L, CourseStatus.DRAFT))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_COURSE_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("강의 상태 변경 - DRAFT 강의를 CLOSED로 변경하면 INVALID_COURSE_STATUS_TRANSITION 예외가 발생한다")
    void changeCourseStatus_fail_draft_to_closed() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.changeCourseStatus(1L, 1L, CourseStatus.CLOSED))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_COURSE_STATUS_TRANSITION);
    }
}
