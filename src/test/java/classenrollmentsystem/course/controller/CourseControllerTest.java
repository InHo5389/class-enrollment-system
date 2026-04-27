package classenrollmentsystem.course.controller;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.config.RedisTestContainerConfig;
import classenrollmentsystem.course.controller.dto.request.ChangeCourseStatusRequest;
import classenrollmentsystem.course.controller.dto.request.CreateCourseRequest;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.service.CourseService;
import classenrollmentsystem.course.service.dto.CourseDetailDto;
import classenrollmentsystem.course.service.dto.CourseDto;
import classenrollmentsystem.user.service.CreatorProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RedisTestContainerConfig.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private CreatorProfileRepository creatorProfileRepository;

    private CourseDto buildCourseDto(CourseStatus status) {
        return CourseDto.builder()
                .id(1L)
                .creatorName("Creator")
                .title("Spring Boot 완벽 가이드")
                .description("초급부터 고급까지")
                .price(new BigDecimal("49900"))
                .maxCapacity(30)
                .currentEnrollment(5)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private CourseDetailDto buildCourseDetailDto(CourseStatus status) {
        return CourseDetailDto.builder()
                .id(1L)
                .creatorName("Creator")
                .creatorBio("강의 소개")
                .title("Spring Boot 완벽 가이드")
                .description("초급부터 고급까지")
                .price(new BigDecimal("49900"))
                .maxCapacity(30)
                .currentEnrollment(5)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("강의 등록 API - 크리에이터가 강의를 등록하면 DRAFT 상태의 강의가 반환된다")
    void createCourse_success() throws Exception {
        when(creatorProfileRepository.existsByUserId(1L)).thenReturn(true);
        when(courseService.createCourse(any())).thenReturn(buildCourseDto(CourseStatus.DRAFT));

        mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCourseRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Spring Boot 완벽 가이드"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("강의 등록 API - X-User-Id 헤더가 없으면 401을 반환한다")
    void createCourse_fail_no_auth_header() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCourseRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorType.MISSING_USER_ID_HEADER.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.MISSING_USER_ID_HEADER.getMessage()));
    }

    @Test
    @DisplayName("강의 등록 API - 크리에이터 프로필이 없는 사용자는 403을 반환한다")
    void createCourse_fail_not_creator() throws Exception {
        when(creatorProfileRepository.existsByUserId(99L)).thenReturn(false);

        mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCourseRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("강의 목록 조회 API - 인증 없이 목록을 조회할 수 있으며 기본 필터는 OPEN이다")
    void getCourses_success_default_open_filter() throws Exception {
        Page<CourseDto> page = new PageImpl<>(List.of(buildCourseDto(CourseStatus.OPEN)));
        when(courseService.getCourses(eq(CourseStatus.OPEN), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Spring Boot 완벽 가이드"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("강의 목록 조회 API - status 파라미터로 DRAFT 강의를 필터링할 수 있다")
    void getCourses_success_with_draft_filter() throws Exception {
        Page<CourseDto> page = new PageImpl<>(List.of(buildCourseDto(CourseStatus.DRAFT)));
        when(courseService.getCourses(eq(CourseStatus.DRAFT), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/courses").param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    @Test
    @DisplayName("강의 목록 조회 API - 강의 목록에 수강 인원(currentEnrollment)이 포함된다")
    void getCourses_includes_current_enrollment() throws Exception {
        Page<CourseDto> page = new PageImpl<>(List.of(buildCourseDto(CourseStatus.OPEN)));
        when(courseService.getCourses(eq(CourseStatus.OPEN), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currentEnrollment").value(5));
    }

    @Test
    @DisplayName("강의 상세 조회 API - 인증 없이 OPEN 강의를 상세 조회할 수 있다")
    void getCourse_success_anonymous() throws Exception {
        when(courseService.getCourse(eq(1L), isNull())).thenReturn(buildCourseDetailDto(CourseStatus.OPEN));

        mockMvc.perform(get("/api/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.creatorName").value("Creator"))
                .andExpect(jsonPath("$.creatorBio").value("강의 소개"))
                .andExpect(jsonPath("$.currentEnrollment").value(5))
                .andExpect(jsonPath("$.availableSeats").value(25));
    }

    @Test
    @DisplayName("강의 상세 조회 API - X-User-Id 헤더가 있으면 userId를 함께 전달한다")
    void getCourse_success_authenticated() throws Exception {
        when(courseService.getCourse(eq(1L), eq(1L))).thenReturn(buildCourseDetailDto(CourseStatus.DRAFT));

        mockMvc.perform(get("/api/courses/1").header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("강의 상세 조회 API - 존재하지 않는 강의를 조회하면 404를 반환한다")
    void getCourse_fail_not_found() throws Exception {
        when(courseService.getCourse(eq(999L), any())).thenThrow(new CustomGlobalException(ErrorType.COURSE_NOT_FOUND));

        mockMvc.perform(get("/api/courses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorType.COURSE_NOT_FOUND.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.COURSE_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("강의 상태 변경 API - DRAFT에서 OPEN으로 변경할 수 있다")
    void changeCourseStatus_success() throws Exception {
        when(courseService.changeCourseStatus(anyLong(), anyLong(), eq(CourseStatus.OPEN)))
                .thenReturn(buildCourseDto(CourseStatus.OPEN));

        mockMvc.perform(patch("/api/courses/1/status")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("강의 상태 변경 API - X-User-Id 헤더가 없으면 401을 반환한다")
    void changeCourseStatus_fail_no_auth_header() throws Exception {
        mockMvc.perform(patch("/api/courses/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorType.MISSING_USER_ID_HEADER.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.MISSING_USER_ID_HEADER.getMessage()));
    }

    @Test
    @DisplayName("강의 상태 변경 API - 작성자가 아닌 사용자가 변경하면 403을 반환한다")
    void changeCourseStatus_fail_not_owner() throws Exception {
        when(courseService.changeCourseStatus(anyLong(), anyLong(), any()))
                .thenThrow(new CustomGlobalException(ErrorType.COURSE_NOT_OWNER));

        mockMvc.perform(patch("/api/courses/1/status")
                        .header("X-User-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorType.COURSE_NOT_OWNER.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.COURSE_NOT_OWNER.getMessage()));
    }

    @Test
    @DisplayName("강의 상태 변경 API - 허용되지 않는 상태 변경이면 400을 반환한다")
    void changeCourseStatus_fail_invalid_transition() throws Exception {
        when(courseService.changeCourseStatus(anyLong(), anyLong(), any()))
                .thenThrow(new CustomGlobalException(ErrorType.INVALID_COURSE_STATUS_TRANSITION));

        mockMvc.perform(patch("/api/courses/1/status")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorType.INVALID_COURSE_STATUS_TRANSITION.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.INVALID_COURSE_STATUS_TRANSITION.getMessage()));
    }
}
