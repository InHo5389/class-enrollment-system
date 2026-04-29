package classenrollmentsystem.enrollment.controller;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.config.RedisTestContainerConfig;
import classenrollmentsystem.enrollment.entity.EnrollmentStatus;
import classenrollmentsystem.enrollment.service.EnrollmentService;
import classenrollmentsystem.enrollment.service.dto.EnrollmentDto;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RedisTestContainerConfig.class)
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @MockitoBean
    private CreatorProfileRepository creatorProfileRepository;

    private EnrollmentDto buildEnrollmentDto(EnrollmentStatus status) {
        return EnrollmentDto.builder()
                .id(1L)
                .userId(1L)
                .courseId(1L)
                .courseTitle("Spring Boot 완벽 가이드")
                .creatorName("Creator")
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("수강 신청 API - X-User-Id 헤더와 유효한 강의 ID가 있으면 PENDING 상태의 수강 신청이 생성된다")
    void enroll_success() throws Exception {
        when(enrollmentService.enroll(eq(1L), eq(1L))).thenReturn(buildEnrollmentDto(EnrollmentStatus.PENDING));

        mockMvc.perform(post("/api/enrollments")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.courseTitle").value("Spring Boot 완벽 가이드"));
    }

    @Test
    @DisplayName("수강 신청 API - X-User-Id 헤더가 없으면 401을 반환한다")
    void enroll_fail_no_auth_header() throws Exception {
        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseId", 1L))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorType.MISSING_USER_ID_HEADER.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.MISSING_USER_ID_HEADER.getMessage()));
    }

    @Test
    @DisplayName("수강 신청 API - 본인이 개설한 강의에 신청하면 400을 반환한다")
    void enroll_fail_own_course() throws Exception {
        when(enrollmentService.enroll(anyLong(), anyLong()))
                .thenThrow(new CustomGlobalException(ErrorType.ENROLLMENT_OWN_COURSE));

        mockMvc.perform(post("/api/enrollments")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseId", 1L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorType.ENROLLMENT_OWN_COURSE.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.ENROLLMENT_OWN_COURSE.getMessage()));
    }

    @Test
    @DisplayName("수강 신청 API - OPEN 상태가 아닌 강의에 신청하면 400을 반환한다")
    void enroll_fail_course_not_open() throws Exception {
        when(enrollmentService.enroll(anyLong(), anyLong()))
                .thenThrow(new CustomGlobalException(ErrorType.ENROLLMENT_COURSE_NOT_OPEN));

        mockMvc.perform(post("/api/enrollments")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseId", 2L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorType.ENROLLMENT_COURSE_NOT_OPEN.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.ENROLLMENT_COURSE_NOT_OPEN.getMessage()));
    }

    @Test
    @DisplayName("수강 신청 API - 이미 신청한 강의에 재신청하면 400을 반환한다")
    void enroll_fail_already_exists() throws Exception {
        when(enrollmentService.enroll(anyLong(), anyLong()))
                .thenThrow(new CustomGlobalException(ErrorType.ENROLLMENT_ALREADY_EXISTS));

        mockMvc.perform(post("/api/enrollments")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseId", 1L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorType.ENROLLMENT_ALREADY_EXISTS.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.ENROLLMENT_ALREADY_EXISTS.getMessage()));
    }

    @Test
    @DisplayName("수강 신청 API - 정원이 가득 찬 강의에 신청하면 400을 반환한다")
    void enroll_fail_capacity_exceeded() throws Exception {
        when(enrollmentService.enroll(anyLong(), anyLong()))
                .thenThrow(new CustomGlobalException(ErrorType.ENROLLMENT_CAPACITY_EXCEEDED));

        mockMvc.perform(post("/api/enrollments")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseId", 1L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorType.ENROLLMENT_CAPACITY_EXCEEDED.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.ENROLLMENT_CAPACITY_EXCEEDED.getMessage()));
    }

    @Test
    @DisplayName("수강 신청 API - 대기열 활성 토큰 없이 수강 신청하면 403을 반환한다")
    void enroll_fail_no_active_token() throws Exception {
        when(enrollmentService.enroll(anyLong(), anyLong()))
                .thenThrow(new CustomGlobalException(ErrorType.QUEUE_TOKEN_NOT_ACTIVE));

        mockMvc.perform(post("/api/enrollments")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseId", 1L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorType.QUEUE_TOKEN_NOT_ACTIVE.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.QUEUE_TOKEN_NOT_ACTIVE.getMessage()));
    }

    @Test
    @DisplayName("결제 확정 API - X-User-Id 헤더와 유효한 수강 신청 ID가 있으면 CONFIRMED 상태로 변경된다")
    void confirm_success() throws Exception {
        when(enrollmentService.confirm(eq(1L), eq(1L))).thenReturn(buildEnrollmentDto(EnrollmentStatus.CONFIRMED));

        mockMvc.perform(post("/api/enrollments/1/confirm")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("결제 확정 API - X-User-Id 헤더가 없으면 401을 반환한다")
    void confirm_fail_no_auth_header() throws Exception {
        mockMvc.perform(post("/api/enrollments/1/confirm"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorType.MISSING_USER_ID_HEADER.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.MISSING_USER_ID_HEADER.getMessage()));
    }

    @Test
    @DisplayName("결제 확정 API - 본인 소유가 아닌 수강 신청을 확정하려 하면 403을 반환한다")
    void confirm_fail_not_owner() throws Exception {
        when(enrollmentService.confirm(anyLong(), anyLong()))
                .thenThrow(new CustomGlobalException(ErrorType.ENROLLMENT_NOT_OWNER));

        mockMvc.perform(post("/api/enrollments/1/confirm")
                        .header("X-User-Id", 99L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorType.ENROLLMENT_NOT_OWNER.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.ENROLLMENT_NOT_OWNER.getMessage()));
    }

    @Test
    @DisplayName("결제 확정 API - 이미 CONFIRMED 상태인 수강 신청을 다시 확정하려 하면 400을 반환한다")
    void confirm_fail_already_confirmed() throws Exception {
        when(enrollmentService.confirm(anyLong(), anyLong()))
                .thenThrow(new CustomGlobalException(ErrorType.ENROLLMENT_INVALID_STATUS_TRANSITION));

        mockMvc.perform(post("/api/enrollments/1/confirm")
                        .header("X-User-Id", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorType.ENROLLMENT_INVALID_STATUS_TRANSITION.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.ENROLLMENT_INVALID_STATUS_TRANSITION.getMessage()));
    }

    @Test
    @DisplayName("수강 취소 API - X-User-Id 헤더와 유효한 수강 신청 ID가 있으면 CANCELLED 상태로 변경된다")
    void cancel_success() throws Exception {
        when(enrollmentService.cancel(eq(1L), eq(1L))).thenReturn(buildEnrollmentDto(EnrollmentStatus.CANCELLED));

        mockMvc.perform(delete("/api/enrollments/1")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("수강 취소 API - X-User-Id 헤더가 없으면 401을 반환한다")
    void cancel_fail_no_auth_header() throws Exception {
        mockMvc.perform(delete("/api/enrollments/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorType.MISSING_USER_ID_HEADER.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.MISSING_USER_ID_HEADER.getMessage()));
    }

    @Test
    @DisplayName("수강 취소 API - 본인 소유가 아닌 수강 신청을 취소하려 하면 403을 반환한다")
    void cancel_fail_not_owner() throws Exception {
        when(enrollmentService.cancel(anyLong(), anyLong()))
                .thenThrow(new CustomGlobalException(ErrorType.ENROLLMENT_NOT_OWNER));

        mockMvc.perform(delete("/api/enrollments/1")
                        .header("X-User-Id", 99L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorType.ENROLLMENT_NOT_OWNER.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.ENROLLMENT_NOT_OWNER.getMessage()));
    }

    @Test
    @DisplayName("수강 취소 API - 이미 CANCELLED 상태인 수강 신청을 다시 취소하려 하면 400을 반환한다")
    void cancel_fail_already_cancelled() throws Exception {
        when(enrollmentService.cancel(anyLong(), anyLong()))
                .thenThrow(new CustomGlobalException(ErrorType.ENROLLMENT_CANCEL_NOT_ALLOWED));

        mockMvc.perform(delete("/api/enrollments/1")
                        .header("X-User-Id", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorType.ENROLLMENT_CANCEL_NOT_ALLOWED.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.ENROLLMENT_CANCEL_NOT_ALLOWED.getMessage()));
    }

    @Test
    @DisplayName("수강 취소 API - 결제 확정 후 취소 기한이 지난 수강 신청을 취소하려 하면 400을 반환한다")
    void cancel_fail_deadline_exceeded() throws Exception {
        when(enrollmentService.cancel(anyLong(), anyLong()))
                .thenThrow(new CustomGlobalException(ErrorType.ENROLLMENT_CANCEL_DEADLINE_EXCEEDED));

        mockMvc.perform(delete("/api/enrollments/1")
                        .header("X-User-Id", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorType.ENROLLMENT_CANCEL_DEADLINE_EXCEEDED.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.ENROLLMENT_CANCEL_DEADLINE_EXCEEDED.getMessage()));
    }

    @Test
    @DisplayName("내 수강 신청 목록 API - X-User-Id 헤더가 있으면 수강 신청 목록이 페이지 형태로 반환된다")
    void getMyEnrollments_success() throws Exception {
        Page<EnrollmentDto> page = new PageImpl<>(List.of(buildEnrollmentDto(EnrollmentStatus.PENDING)));
        when(enrollmentService.getMyEnrollments(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/enrollments/me")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].courseTitle").value("Spring Boot 완벽 가이드"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("내 수강 신청 목록 API - X-User-Id 헤더가 없으면 401을 반환한다")
    void getMyEnrollments_fail_no_auth_header() throws Exception {
        mockMvc.perform(get("/api/enrollments/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorType.MISSING_USER_ID_HEADER.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.MISSING_USER_ID_HEADER.getMessage()));
    }
}
