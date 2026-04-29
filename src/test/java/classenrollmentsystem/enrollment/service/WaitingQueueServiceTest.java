package classenrollmentsystem.enrollment.service;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.course.entity.Course;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.service.CourseRepository;
import classenrollmentsystem.enrollment.service.dto.QueueStatusDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitingQueueServiceTest {

    @Mock
    private WaitingQueueRepository waitingQueueRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private WaitingQueueService waitingQueueService;

    private Course openCourse;
    private Course draftCourse;
    private Course closedCourse;

    private static final Long COURSE_ID = 1L;
    private static final Long USER_ID = 10L;

    @BeforeEach
    void setUp() {
        openCourse = Course.builder()
                .id(COURSE_ID)
                .title("Spring Boot 완벽 가이드")
                .price(new BigDecimal("49900"))
                .maxCapacity(30)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CourseStatus.OPEN)
                .build();

        draftCourse = Course.builder()
                .id(2L)
                .title("미공개 강의")
                .price(new BigDecimal("10000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CourseStatus.DRAFT)
                .build();

        closedCourse = Course.builder()
                .id(3L)
                .title("마감된 강의")
                .price(new BigDecimal("20000"))
                .maxCapacity(10)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CourseStatus.CLOSED)
                .build();
    }

    @Test
    @DisplayName("대기열 진입 - waiting 큐가 비어있고 active 큐에 여유가 있으면 즉시 ACTIVE 상태로 반환된다")
    void enterQueue_immediate_active_when_no_waiting() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(openCourse));
        when(waitingQueueRepository.isInActiveQueue(COURSE_ID, USER_ID)).thenReturn(false);
        when(waitingQueueRepository.getWaitingRank(COURSE_ID, USER_ID)).thenReturn(null);
        when(waitingQueueRepository.getWaitingQueueSize(COURSE_ID)).thenReturn(0L);
        when(waitingQueueRepository.getActiveQueueSize(COURSE_ID)).thenReturn(0L);

        QueueStatusDto result = waitingQueueService.enterQueue(COURSE_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(QueueStatusDto.QueueStatus.ACTIVE);
        verify(waitingQueueRepository).addToActiveQueue(eq(COURSE_ID), eq(USER_ID), anyLong());
        verify(waitingQueueRepository, never()).addToWaitingQueue(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("대기열 진입 - waiting 큐에 사람이 있으면 waiting 큐에 추가되고 WAITING 상태로 반환된다")
    void enterQueue_waiting_when_queue_has_people() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(openCourse));
        when(waitingQueueRepository.isInActiveQueue(COURSE_ID, USER_ID)).thenReturn(false);
        when(waitingQueueRepository.getWaitingRank(COURSE_ID, USER_ID)).thenReturn(null, 500L);
        when(waitingQueueRepository.getWaitingQueueSize(COURSE_ID)).thenReturn(500L);
        when(waitingQueueRepository.getActiveQueueSize(COURSE_ID)).thenReturn(100L);

        QueueStatusDto result = waitingQueueService.enterQueue(COURSE_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(QueueStatusDto.QueueStatus.WAITING);
        assertThat(result.getRank()).isEqualTo(501L);
        verify(waitingQueueRepository).addToWaitingQueue(eq(COURSE_ID), eq(USER_ID), anyLong());
        verify(waitingQueueRepository, never()).addToActiveQueue(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("대기열 진입 - active 큐가 2,100명으로 가득 찼으면 waiting 큐에 추가된다")
    void enterQueue_waiting_when_active_queue_full() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(openCourse));
        when(waitingQueueRepository.isInActiveQueue(COURSE_ID, USER_ID)).thenReturn(false);
        when(waitingQueueRepository.getWaitingRank(COURSE_ID, USER_ID)).thenReturn(null, 0L);
        when(waitingQueueRepository.getWaitingQueueSize(COURSE_ID)).thenReturn(0L);
        when(waitingQueueRepository.getActiveQueueSize(COURSE_ID)).thenReturn(2100L);

        QueueStatusDto result = waitingQueueService.enterQueue(COURSE_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(QueueStatusDto.QueueStatus.WAITING);
        verify(waitingQueueRepository).addToWaitingQueue(eq(COURSE_ID), eq(USER_ID), anyLong());
        verify(waitingQueueRepository, never()).addToActiveQueue(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("대기열 진입 - 이미 active 큐에 있으면 다시 추가하지 않고 ACTIVE 상태를 반환한다")
    void enterQueue_returns_active_if_already_in_active_queue() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(openCourse));
        when(waitingQueueRepository.isInActiveQueue(COURSE_ID, USER_ID)).thenReturn(true);

        QueueStatusDto result = waitingQueueService.enterQueue(COURSE_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(QueueStatusDto.QueueStatus.ACTIVE);
        verify(waitingQueueRepository, never()).addToActiveQueue(anyLong(), anyLong(), anyLong());
        verify(waitingQueueRepository, never()).addToWaitingQueue(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("대기열 진입 - 이미 waiting 큐에 있으면 중복 추가 없이 현재 순위로 WAITING 상태를 반환한다")
    void enterQueue_returns_current_rank_if_already_in_waiting_queue() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(openCourse));
        when(waitingQueueRepository.isInActiveQueue(COURSE_ID, USER_ID)).thenReturn(false);
        when(waitingQueueRepository.getWaitingRank(COURSE_ID, USER_ID)).thenReturn(99L);
        when(waitingQueueRepository.getWaitingQueueSize(COURSE_ID)).thenReturn(200L);

        QueueStatusDto result = waitingQueueService.enterQueue(COURSE_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(QueueStatusDto.QueueStatus.WAITING);
        assertThat(result.getRank()).isEqualTo(100L);
        verify(waitingQueueRepository, never()).addToWaitingQueue(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("대기열 진입 - 존재하지 않는 강의 ID로 요청하면 COURSE_NOT_FOUND 예외가 발생한다")
    void enterQueue_fail_course_not_found() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waitingQueueService.enterQueue(999L, USER_ID))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("대기열 진입 - DRAFT 상태의 강의에 진입하면 ENROLLMENT_COURSE_NOT_OPEN 예외가 발생한다")
    void enterQueue_fail_draft_course() {
        when(courseRepository.findById(2L)).thenReturn(Optional.of(draftCourse));

        assertThatThrownBy(() -> waitingQueueService.enterQueue(2L, USER_ID))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_COURSE_NOT_OPEN);
    }

    @Test
    @DisplayName("대기열 진입 - CLOSED 상태의 강의에 진입하면 ENROLLMENT_COURSE_NOT_OPEN 예외가 발생한다")
    void enterQueue_fail_closed_course() {
        when(courseRepository.findById(3L)).thenReturn(Optional.of(closedCourse));

        assertThatThrownBy(() -> waitingQueueService.enterQueue(3L, USER_ID))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ENROLLMENT_COURSE_NOT_OPEN);
    }

    @Test
    @DisplayName("대기열 상태 조회 - active 큐에 있으면 ACTIVE 상태를 반환한다")
    void getQueueStatus_active() {
        when(waitingQueueRepository.isInActiveQueue(COURSE_ID, USER_ID)).thenReturn(true);

        QueueStatusDto result = waitingQueueService.getQueueStatus(COURSE_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(QueueStatusDto.QueueStatus.ACTIVE);
    }

    @Test
    @DisplayName("대기열 상태 조회 - waiting 큐에 있으면 현재 순위와 함께 WAITING 상태를 반환한다")
    void getQueueStatus_waiting() {
        when(waitingQueueRepository.isInActiveQueue(COURSE_ID, USER_ID)).thenReturn(false);
        when(waitingQueueRepository.getWaitingRank(COURSE_ID, USER_ID)).thenReturn(49L);
        when(waitingQueueRepository.getWaitingQueueSize(COURSE_ID)).thenReturn(100L);

        QueueStatusDto result = waitingQueueService.getQueueStatus(COURSE_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(QueueStatusDto.QueueStatus.WAITING);
        assertThat(result.getRank()).isEqualTo(50L);
        assertThat(result.getTotalWaiting()).isEqualTo(100L);
    }

    @Test
    @DisplayName("대기열 상태 조회 - 어느 큐에도 없으면 NOT_IN_QUEUE 상태를 반환한다")
    void getQueueStatus_not_in_queue() {
        when(waitingQueueRepository.isInActiveQueue(COURSE_ID, USER_ID)).thenReturn(false);
        when(waitingQueueRepository.getWaitingRank(COURSE_ID, USER_ID)).thenReturn(null);

        QueueStatusDto result = waitingQueueService.getQueueStatus(COURSE_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(QueueStatusDto.QueueStatus.NOT_IN_QUEUE);
    }

    @Test
    @DisplayName("활성 토큰 검증 - active 큐에 없으면 QUEUE_TOKEN_NOT_ACTIVE 예외가 발생한다")
    void validateActiveToken_fail_not_in_active_queue() {
        when(waitingQueueRepository.isInActiveQueue(COURSE_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> waitingQueueService.validateActiveToken(COURSE_ID, USER_ID))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.QUEUE_TOKEN_NOT_ACTIVE);
    }

    @Test
    @DisplayName("활성 토큰 검증 - active 큐에 있으면 예외 없이 통과된다")
    void validateActiveToken_success() {
        when(waitingQueueRepository.isInActiveQueue(COURSE_ID, USER_ID)).thenReturn(true);

        waitingQueueService.validateActiveToken(COURSE_ID, USER_ID);
    }

    @Test
    @DisplayName("활성 토큰 반환 - 수강 신청 완료 후 active 큐에서 제거된다")
    void releaseActiveToken_removes_from_active_queue() {
        waitingQueueService.releaseActiveToken(COURSE_ID, USER_ID);

        verify(waitingQueueRepository).removeFromActiveQueue(COURSE_ID, USER_ID);
    }
}
