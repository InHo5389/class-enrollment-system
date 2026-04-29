package classenrollmentsystem.enrollment.controller;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.config.RedisTestContainerConfig;
import classenrollmentsystem.enrollment.controller.dto.request.EnrollRequest;
import classenrollmentsystem.enrollment.service.EnrollmentService;
import classenrollmentsystem.enrollment.service.WaitingQueueService;
import classenrollmentsystem.enrollment.service.dto.QueueStatusDto;
import classenrollmentsystem.user.service.CreatorProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RedisTestContainerConfig.class)
class WaitingQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WaitingQueueService waitingQueueService;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @MockitoBean
    private CreatorProfileRepository creatorProfileRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("대기열 진입 API - waiting 큐가 비어있으면 ACTIVE 상태가 즉시 반환된다")
    void enterQueue_immediate_active() throws Exception {
        when(waitingQueueService.enterQueue(1L, 1L))
                .thenReturn(QueueStatusDto.active(1L, 1L));

        mockMvc.perform(post("/api/queue/courses/1/enter")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("입장이 허가되었습니다. 5분 이내에 수강 신청을 완료해 주세요."));
    }

    @Test
    @DisplayName("대기열 진입 API - 대기가 있으면 WAITING 상태와 순위가 반환된다")
    void enterQueue_waiting() throws Exception {
        when(waitingQueueService.enterQueue(1L, 1L))
                .thenReturn(QueueStatusDto.waiting(1L, 1L, 99L, 500L));

        mockMvc.perform(post("/api/queue/courses/1/enter")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.rank").value(100))
                .andExpect(jsonPath("$.totalWaiting").value(500));
    }

    @Test
    @DisplayName("대기열 진입 API - X-User-Id 헤더가 없으면 401을 반환한다")
    void enterQueue_fail_no_auth_header() throws Exception {
        mockMvc.perform(post("/api/queue/courses/1/enter"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorType.MISSING_USER_ID_HEADER.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.MISSING_USER_ID_HEADER.getMessage()));
    }

    @Test
    @DisplayName("대기열 진입 API - 존재하지 않는 강의 ID로 요청하면 404를 반환한다")
    void enterQueue_fail_course_not_found() throws Exception {
        when(waitingQueueService.enterQueue(anyLong(), anyLong()))
                .thenThrow(new CustomGlobalException(ErrorType.COURSE_NOT_FOUND));

        mockMvc.perform(post("/api/queue/courses/999/enter")
                        .header("X-User-Id", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorType.COURSE_NOT_FOUND.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.COURSE_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("대기열 진입 API - OPEN 상태가 아닌 강의에 진입하면 400을 반환한다")
    void enterQueue_fail_course_not_open() throws Exception {
        when(waitingQueueService.enterQueue(anyLong(), anyLong()))
                .thenThrow(new CustomGlobalException(ErrorType.ENROLLMENT_COURSE_NOT_OPEN));

        mockMvc.perform(post("/api/queue/courses/2/enter")
                        .header("X-User-Id", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorType.ENROLLMENT_COURSE_NOT_OPEN.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.ENROLLMENT_COURSE_NOT_OPEN.getMessage()));
    }

    @Test
    @DisplayName("대기열 상태 조회 API - active 큐에 있으면 ACTIVE 상태를 반환한다")
    void getQueueStatus_active() throws Exception {
        when(waitingQueueService.getQueueStatus(1L, 1L))
                .thenReturn(QueueStatusDto.active(1L, 1L));

        mockMvc.perform(get("/api/queue/courses/1/status")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.rank").value(0));
    }

    @Test
    @DisplayName("대기열 상태 조회 API - waiting 큐에 있으면 WAITING 상태와 순위를 반환한다")
    void getQueueStatus_waiting() throws Exception {
        when(waitingQueueService.getQueueStatus(1L, 1L))
                .thenReturn(QueueStatusDto.waiting(1L, 1L, 299L, 1000L));

        mockMvc.perform(get("/api/queue/courses/1/status")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.rank").value(300))
                .andExpect(jsonPath("$.totalWaiting").value(1000));
    }

    @Test
    @DisplayName("대기열 상태 조회 API - 어느 큐에도 없으면 NOT_IN_QUEUE 상태를 반환한다")
    void getQueueStatus_not_in_queue() throws Exception {
        when(waitingQueueService.getQueueStatus(1L, 1L))
                .thenReturn(QueueStatusDto.notInQueue(1L, 1L));

        mockMvc.perform(get("/api/queue/courses/1/status")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_IN_QUEUE"));
    }

    @Test
    @DisplayName("대기열 상태 조회 API - X-User-Id 헤더가 없으면 401을 반환한다")
    void getQueueStatus_fail_no_auth_header() throws Exception {
        mockMvc.perform(get("/api/queue/courses/1/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorType.MISSING_USER_ID_HEADER.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.MISSING_USER_ID_HEADER.getMessage()));
    }

    @Test
    @DisplayName("수강 신청 API - 대기열 활성 토큰이 없거나 만료되면 QUEUE_TOKEN_NOT_ACTIVE 403을 반환한다")
    void enroll_fail_no_active_token() throws Exception {
        when(enrollmentService.enroll(anyLong(), anyLong()))
                .thenThrow(new CustomGlobalException(ErrorType.QUEUE_TOKEN_NOT_ACTIVE));

        EnrollRequest request = EnrollRequest.from(1L);

        mockMvc.perform(post("/api/enrollments")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorType.QUEUE_TOKEN_NOT_ACTIVE.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.QUEUE_TOKEN_NOT_ACTIVE.getMessage()));
    }

    @Test
    @DisplayName("대기열 진입 API - 대기열에 이미 있으면 WAITING 상태와 현재 순위를 반환한다")
    void enterQueue_already_in_waiting_queue() throws Exception {
        when(waitingQueueService.enterQueue(1L, 1L))
                .thenReturn(QueueStatusDto.waiting(1L, 1L, 49L, 200L));

        mockMvc.perform(post("/api/queue/courses/1/enter")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.rank").value(50))
                .andExpect(jsonPath("$.totalWaiting").value(200));
    }

    @Test
    @DisplayName("대기열 진입 API - 이미 active 큐에 있으면 ACTIVE 상태가 반환된다")
    void enterQueue_already_in_active_queue() throws Exception {
        when(waitingQueueService.enterQueue(1L, 1L))
                .thenReturn(QueueStatusDto.active(1L, 1L));

        mockMvc.perform(post("/api/queue/courses/1/enter")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
