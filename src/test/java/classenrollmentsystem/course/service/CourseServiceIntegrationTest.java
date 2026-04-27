package classenrollmentsystem.course.service;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.config.RedisTestContainerConfig;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.repository.CourseJpaRepository;
import classenrollmentsystem.course.service.dto.CourseDetailDto;
import classenrollmentsystem.course.service.dto.CourseDto;
import classenrollmentsystem.course.service.dto.CreateCourseDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(RedisTestContainerConfig.class)
class CourseServiceIntegrationTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private CreatorProfileJpaRepository creatorProfileJpaRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private User savedUser;
    private CreatorProfile savedCreatorProfile;

    @BeforeEach
    void setUp() {
        redisTemplate.execute((org.springframework.data.redis.connection.RedisConnection connection) -> {
            connection.serverCommands().flushAll();
            return null;
        });

        savedUser = userJpaRepository.save(User.create("creator@example.com", "hashedPassword", "Creator"));
        savedCreatorProfile = creatorProfileJpaRepository.save(CreatorProfile.create(savedUser, "강의 소개"));
    }

    private CreateCourseDto defaultCourseDto() {
        return CreateCourseDto.builder()
                .userId(savedUser.getId())
                .title("Spring Boot 완벽 가이드")
                .description("초급부터 고급까지")
                .price(new BigDecimal("49900"))
                .maxCapacity(30)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();
    }

    // ========================
    // createCourse
    // ========================

    @Test
    @DisplayName("강의 등록 - 크리에이터가 강의를 등록하면 DRAFT 상태로 데이터베이스에 저장된다")
    void createCourse_success() {
        CourseDto result = courseService.createCourse(defaultCourseDto());

        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Spring Boot 완벽 가이드");
        assertThat(result.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(courseJpaRepository.findById(result.getId())).isPresent();
    }

    @Test
    @DisplayName("강의 등록 - 크리에이터가 아닌 사용자는 강의를 등록할 수 없다")
    void createCourse_fail_not_creator() {
        User nonCreator = userJpaRepository.save(User.create("user@example.com", "hashedPassword", "User"));
        CreateCourseDto dto = CreateCourseDto.builder()
                .userId(nonCreator.getId())
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        assertThatThrownBy(() -> courseService.createCourse(dto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_CREATOR);
    }

    // ========================
    // getCourses
    // ========================

    @Test
    @DisplayName("강의 목록 조회 - OPEN 상태 필터로 조회하면 OPEN 강의만 반환된다")
    void getCourses_filter_open() {
        CourseDto created = courseService.createCourse(defaultCourseDto());
        courseService.changeCourseStatus(created.getId(), savedUser.getId(), CourseStatus.OPEN);

        CourseDto draftCourse = courseService.createCourse(CreateCourseDto.builder()
                .userId(savedUser.getId())
                .title("DRAFT 강의")
                .price(new BigDecimal("20000"))
                .maxCapacity(20)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build());

        Page<CourseDto> result = courseService.getCourses(CourseStatus.OPEN, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("강의 목록 조회 - status 필터 없이 조회하면 모든 상태의 강의가 반환된다")
    void getCourses_no_filter_returns_all() {
        courseService.createCourse(defaultCourseDto());
        CourseDto second = courseService.createCourse(CreateCourseDto.builder()
                .userId(savedUser.getId())
                .title("두 번째 강의")
                .price(new BigDecimal("20000"))
                .maxCapacity(20)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build());
        courseService.changeCourseStatus(second.getId(), savedUser.getId(), CourseStatus.OPEN);

        Page<CourseDto> result = courseService.getCourses(null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // ========================
    // getCourse
    // ========================

    @Test
    @DisplayName("강의 상세 조회 - 강의 작성자는 본인의 DRAFT 강의를 조회할 수 있다")
    void getCourse_success_owner_sees_draft() {
        CourseDto created = courseService.createCourse(defaultCourseDto());

        CourseDetailDto result = courseService.getCourse(created.getId(), savedUser.getId());

        assertThat(result.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(result.getCreatorName()).isEqualTo("Creator");
        assertThat(result.getCreatorBio()).isEqualTo("강의 소개");
    }

    @Test
    @DisplayName("강의 상세 조회 - 타인이 DRAFT 강의를 조회하면 COURSE_NOT_FOUND 예외가 발생한다")
    void getCourse_fail_draft_not_owner() {
        CourseDto created = courseService.createCourse(defaultCourseDto());
        User other = userJpaRepository.save(User.create("other@example.com", "hashedPassword", "Other"));

        assertThatThrownBy(() -> courseService.getCourse(created.getId(), other.getId()))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("강의 상세 조회 - 비로그인 사용자(userId=null)가 DRAFT 강의를 조회하면 COURSE_NOT_FOUND 예외가 발생한다")
    void getCourse_fail_draft_anonymous() {
        CourseDto created = courseService.createCourse(defaultCourseDto());

        assertThatThrownBy(() -> courseService.getCourse(created.getId(), null))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("강의 상세 조회 - OPEN 강의는 비로그인 사용자도 조회할 수 있다")
    void getCourse_success_open_anonymous() {
        CourseDto created = courseService.createCourse(defaultCourseDto());
        courseService.changeCourseStatus(created.getId(), savedUser.getId(), CourseStatus.OPEN);

        CourseDetailDto result = courseService.getCourse(created.getId(), null);

        assertThat(result.getStatus()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("강의 상세 조회 - 존재하지 않는 강의를 조회하면 COURSE_NOT_FOUND 예외가 발생한다")
    void getCourse_fail_not_found() {
        assertThatThrownBy(() -> courseService.getCourse(999L, 1L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    // ========================
    // changeCourseStatus
    // ========================

    @Test
    @DisplayName("강의 상태 변경 - DRAFT에서 OPEN으로 변경할 수 있다")
    void changeCourseStatus_draft_to_open() {
        CourseDto created = courseService.createCourse(defaultCourseDto());

        CourseDto result = courseService.changeCourseStatus(created.getId(), savedUser.getId(), CourseStatus.OPEN);

        assertThat(result.getStatus()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("강의 상태 변경 - OPEN에서 CLOSED로 변경할 수 있다")
    void changeCourseStatus_open_to_closed() {
        CourseDto created = courseService.createCourse(defaultCourseDto());
        courseService.changeCourseStatus(created.getId(), savedUser.getId(), CourseStatus.OPEN);

        CourseDto result = courseService.changeCourseStatus(created.getId(), savedUser.getId(), CourseStatus.CLOSED);

        assertThat(result.getStatus()).isEqualTo(CourseStatus.CLOSED);
    }

    @Test
    @DisplayName("강의 상태 변경 - CLOSED 강의를 변경하려 하면 INVALID_COURSE_STATUS_TRANSITION 예외가 발생한다")
    void changeCourseStatus_fail_closed() {
        CourseDto created = courseService.createCourse(defaultCourseDto());
        courseService.changeCourseStatus(created.getId(), savedUser.getId(), CourseStatus.OPEN);
        courseService.changeCourseStatus(created.getId(), savedUser.getId(), CourseStatus.CLOSED);

        assertThatThrownBy(() -> courseService.changeCourseStatus(created.getId(), savedUser.getId(), CourseStatus.OPEN))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_COURSE_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("강의 상태 변경 - 작성자가 아닌 사용자가 변경하면 COURSE_NOT_OWNER 예외가 발생한다")
    void changeCourseStatus_fail_not_owner() {
        CourseDto created = courseService.createCourse(defaultCourseDto());
        User other = userJpaRepository.save(User.create("other@example.com", "hashedPassword", "Other"));

        assertThatThrownBy(() -> courseService.changeCourseStatus(created.getId(), other.getId(), CourseStatus.OPEN))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.COURSE_NOT_OWNER);
    }
}
