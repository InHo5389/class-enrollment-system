package classenrollmentsystem.course.service;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.repository.CourseJpaRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseServiceIntegrationTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private CreatorProfileJpaRepository creatorProfileJpaRepository;

    private User savedUser;
    private CreatorProfile savedCreatorProfile;

    @BeforeEach
    void setUp() {
        savedUser = userJpaRepository.save(User.create("creator@example.com", "hashedPassword", "Creator"));
        savedCreatorProfile = creatorProfileJpaRepository.save(CreatorProfile.create(savedUser, "강의 소개"));
    }

    @Test
    @DisplayName("강의 등록 - 크리에이터가 강의를 등록하면 데이터베이스에 저장된다")
    void createCourse_success() {
        // given
        CreateCourseDto dto = CreateCourseDto.builder()
                .userId(savedUser.getId())
                .title("Spring Boot 완벽 가이드")
                .description("초급부터 고급까지")
                .price(new BigDecimal("49900"))
                .maxCapacity(30)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        // when
        CourseDto result = courseService.createCourse(dto);

        // then
        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Spring Boot 완벽 가이드");
        assertThat(result.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(courseJpaRepository.findById(result.getId())).isPresent();
    }

    @Test
    @DisplayName("강의 등록 - 크리에이터가 아닌 사용자는 강의를 등록할 수 없다")
    void createCourse_fail_not_creator() {
        // given
        User nonCreator = userJpaRepository.save(User.create("user@example.com", "hashedPassword", "User"));
        CreateCourseDto dto = CreateCourseDto.builder()
                .userId(nonCreator.getId())
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        // when & then
        assertThatThrownBy(() -> courseService.createCourse(dto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_CREATOR);
    }

    @Test
    @DisplayName("강의 목록 조회 - 상태 필터로 OPEN 강의만 조회할 수 있다")
    void getCourses_success_filter_open() {
        // given
        CreateCourseDto draftDto = CreateCourseDto.builder()
                .userId(savedUser.getId())
                .title("DRAFT 강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();
        CourseDto created = courseService.createCourse(draftDto);
        courseService.changeCourseStatus(created.getId(), savedUser.getId(), CourseStatus.OPEN);

        CreateCourseDto draftDto2 = CreateCourseDto.builder()
                .userId(savedUser.getId())
                .title("DRAFT 강의2")
                .price(new BigDecimal("20000"))
                .maxCapacity(20)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();
        courseService.createCourse(draftDto2);

        // when
        Page<CourseDto> result = courseService.getCourses(CourseStatus.OPEN, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("강의 상태 변경 - DRAFT에서 OPEN으로 변경할 수 있다")
    void changeCourseStatus_success_draft_to_open() {
        // given
        CourseDto created = courseService.createCourse(CreateCourseDto.builder()
                .userId(savedUser.getId())
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build());

        // when
        CourseDto result = courseService.changeCourseStatus(created.getId(), savedUser.getId(), CourseStatus.OPEN);

        // then
        assertThat(result.getStatus()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("강의 상태 변경 - OPEN에서 CLOSED로 변경할 수 있다")
    void changeCourseStatus_success_open_to_closed() {
        // given
        CourseDto created = courseService.createCourse(CreateCourseDto.builder()
                .userId(savedUser.getId())
                .title("강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build());
        courseService.changeCourseStatus(created.getId(), savedUser.getId(), CourseStatus.OPEN);

        // when
        CourseDto result = courseService.changeCourseStatus(created.getId(), savedUser.getId(), CourseStatus.CLOSED);

        // then
        assertThat(result.getStatus()).isEqualTo(CourseStatus.CLOSED);
    }

}
