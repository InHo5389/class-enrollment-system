package classenrollmentsystem.course.controller;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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
                .currentEnrollment(0)
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
                .currentEnrollment(0)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("강의 등록 API - 크리에이터가 강의를 등록할 수 있다")
    void createCourse_success() throws Exception {
        // given
        CreateCourseRequest request = new CreateCourseRequest();
        CourseDto courseDto = buildCourseDto(CourseStatus.DRAFT);

        when(creatorProfileRepository.existsByUserId(1L)).thenReturn(true);
        when(courseService.createCourse(any())).thenReturn(courseDto);

        // when & then
        mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Spring Boot 완벽 가이드"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("강의 등록 API - 크리에이터가 아닌 사용자는 403을 반환한다")
    void createCourse_fail_not_creator() throws Exception {
        // given
        CreateCourseRequest request = new CreateCourseRequest();

        when(creatorProfileRepository.existsByUserId(99L)).thenReturn(false);

        // when & then
        mockMvc.perform(post("/api/courses")
                        .header("X-User-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("강의 목록 조회 API - 인증 없이 목록을 조회할 수 있다")
    void getCourses_success() throws Exception {
        // given
        Page<CourseDto> page = new PageImpl<>(List.of(buildCourseDto(CourseStatus.OPEN)));

        when(courseService.getCourses(eq(CourseStatus.OPEN), any(Pageable.class))).thenReturn(page);

        // when & then
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Spring Boot 완벽 가이드"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("강의 목록 조회 API - 상태 필터로 조회할 수 있다")
    void getCourses_success_with_status_filter() throws Exception {
        // given
        Page<CourseDto> page = new PageImpl<>(List.of(buildCourseDto(CourseStatus.OPEN)));

        when(courseService.getCourses(eq(CourseStatus.OPEN), any(Pageable.class))).thenReturn(page);

        // when & then
        mockMvc.perform(get("/api/courses")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("강의 상세 조회 API - 인증 없이 강의를 상세 조회할 수 있다")
    void getCourse_success() throws Exception {
        // given
        CourseDetailDto courseDetailDto = buildCourseDetailDto(CourseStatus.OPEN);

        when(courseService.getCourse(anyLong(), isNull())).thenReturn(courseDetailDto);

        // when & then
        mockMvc.perform(get("/api/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.creatorName").value("Creator"))
                .andExpect(jsonPath("$.creatorBio").value("강의 소개"))
                .andExpect(jsonPath("$.availableSeats").value(30));
    }

    @Test
    @DisplayName("강의 상태 변경 API - DRAFT에서 OPEN으로 변경할 수 있다")
    void changeCourseStatus_success() throws Exception {
        // given
        CourseDto courseDto = buildCourseDto(CourseStatus.OPEN);

        when(courseService.changeCourseStatus(anyLong(), anyLong(), eq(CourseStatus.OPEN))).thenReturn(courseDto);

        ChangeCourseStatusRequest request = new ChangeCourseStatusRequest();

        // when & then
        mockMvc.perform(patch("/api/courses/1/status")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("강의 상태 변경 API - X-User-Id 헤더 없이 요청하면 401을 반환한다")
    void changeCourseStatus_fail_no_auth() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/courses/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isUnauthorized());
    }

}
