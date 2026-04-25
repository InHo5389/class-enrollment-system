package classenrollmentsystem.course.service;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.course.entity.Course;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.service.dto.CourseDetailDto;
import classenrollmentsystem.course.service.dto.CourseDto;
import classenrollmentsystem.course.service.dto.CreateCourseDto;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CreatorProfileRepository creatorProfileRepository;

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

    @Test
    @DisplayName("강의 등록 - 크리에이터가 강의를 등록할 수 있다")
    void createCourse_success() {
        // given
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

        // when
        CourseDto result = courseService.createCourse(dto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Spring Boot 완벽 가이드");
        assertThat(result.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(result.getCreatorName()).isEqualTo("Creator");
    }

    @Test
    @DisplayName("강의 등록 - 크리에이터가 아닌 사용자는 강의를 등록할 수 없다")
    void createCourse_fail_not_creator() {
        // given
        CreateCourseDto dto = CreateCourseDto.builder()
                .userId(99L)
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        when(creatorProfileRepository.findByUserId(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> courseService.createCourse(dto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_CREATOR);
    }

    @Test
    @DisplayName("강의 등록 - 정원이 0이하이면 예외를 발생시킨다")
    void createCourse_fail_invalid_capacity() {
        // given
        CreateCourseDto dto = CreateCourseDto.builder()
                .userId(1L)
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(0)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        // when & then
        assertThatThrownBy(() -> courseService.createCourse(dto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_COURSE_CAPACITY);
    }

    @Test
    @DisplayName("강의 등록 - 종료일이 시작일보다 이전이면 예외를 발생시킨다")
    void createCourse_fail_invalid_period() {
        // given
        CreateCourseDto dto = CreateCourseDto.builder()
                .userId(1L)
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 6, 30))
                .endDate(LocalDate.of(2026, 5, 1))
                .build();

        // when & then
        assertThatThrownBy(() -> courseService.createCourse(dto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_COURSE_PERIOD);
    }

    @Test
    @DisplayName("강의 목록 조회 - 상태 필터 없이 전체 조회할 수 있다")
    void getCourses_success_no_filter() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Course> coursePage = new PageImpl<>(List.of(course));

        when(courseRepository.findAll(pageable)).thenReturn(coursePage);

        // when
        Page<CourseDto> result = courseService.getCourses(null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Spring Boot 완벽 가이드");
    }

    @Test
    @DisplayName("강의 목록 조회 - 상태 필터로 조회할 수 있다")
    void getCourses_success_with_filter() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Course> coursePage = new PageImpl<>(List.of(course));

        when(courseRepository.findAllByStatus(CourseStatus.DRAFT, pageable)).thenReturn(coursePage);

        // when
        Page<CourseDto> result = courseService.getCourses(CourseStatus.DRAFT, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    @DisplayName("강의 상세 조회 - 강의 작성자는 DRAFT 강의를 조회할 수 있다")
    void getCourse_success_owner_sees_draft() {
        // given
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        // when
        CourseDetailDto result = courseService.getCourse(1L, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(result.getCreatorName()).isEqualTo("Creator");
        assertThat(result.getCreatorBio()).isEqualTo("강의 소개");
    }

    @Test
    @DisplayName("강의 상세 조회 - 타인은 DRAFT 강의를 조회할 수 없다")
    void getCourse_fail_draft_not_owner() {
        // given
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        // when & then
        assertThatThrownBy(() -> courseService.getCourse(1L, 99L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("강의 상태 변경 - 작성자가 DRAFT에서 OPEN으로 변경할 수 있다")
    void changeCourseStatus_success_draft_to_open() {
        // given
        Course savedCourse = Course.builder()
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
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);

        // when
        CourseDto result = courseService.changeCourseStatus(1L, 1L, CourseStatus.OPEN);

        // then
        assertThat(result.getStatus()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("강의 상태 변경 - 작성자가 아닌 사용자는 상태를 변경할 수 없다")
    void changeCourseStatus_fail_not_owner() {
        // given
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        // when & then
        assertThatThrownBy(() -> courseService.changeCourseStatus(1L, 99L, CourseStatus.OPEN))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.COURSE_NOT_OWNER);
    }

    @Test
    @DisplayName("강의 상태 변경 - CLOSED에서 다른 상태로 변경할 수 없다")
    void changeCourseStatus_fail_closed_to_open() {
        // given
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

        // when & then
        assertThatThrownBy(() -> courseService.changeCourseStatus(1L, 1L, CourseStatus.OPEN))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_COURSE_STATUS_TRANSITION);
    }

}
