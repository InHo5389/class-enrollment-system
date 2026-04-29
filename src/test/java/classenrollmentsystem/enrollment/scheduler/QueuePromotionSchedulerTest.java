package classenrollmentsystem.enrollment.scheduler;

import classenrollmentsystem.enrollment.service.WaitingQueueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueuePromotionSchedulerTest {

    @Mock
    private WaitingQueueRepository waitingQueueRepository;

    @InjectMocks
    private QueuePromotionScheduler scheduler;

    @Test
    @DisplayName("스케줄러 실행 - waiting 강의가 없으면 아무 작업도 하지 않는다")
    void promoteWaitingToActive_does_nothing_when_no_waiting_courses() {
        when(waitingQueueRepository.scanWaitingCourseIds()).thenReturn(Set.of());

        scheduler.promoteWaitingToActive();

        verify(waitingQueueRepository, never()).promoteFromWaiting(anyLong(), anyInt(), anyLong());
        verify(waitingQueueRepository, never()).removeExpiredFromActiveQueue(anyLong(), anyLong());
    }

    @Test
    @DisplayName("스케줄러 실행 - waiting 큐에 대기자가 있으면 active 큐로 이동시킨다")
    void promoteWaitingToActive_promotes_when_waiting_exists() {
        when(waitingQueueRepository.scanWaitingCourseIds()).thenReturn(Set.of(1L));
        when(waitingQueueRepository.getWaitingQueueSize(1L)).thenReturn(100L);
        when(waitingQueueRepository.promoteFromWaiting(eq(1L), eq(2100), anyLong())).thenReturn(100L);

        scheduler.promoteWaitingToActive();

        verify(waitingQueueRepository).removeExpiredFromActiveQueue(eq(1L), anyLong());
        verify(waitingQueueRepository).promoteFromWaiting(eq(1L), eq(2100), anyLong());
    }

    @Test
    @DisplayName("스케줄러 실행 - waiting 큐가 비어있으면 promote를 호출하지 않는다")
    void promoteWaitingToActive_skips_promote_when_waiting_empty() {
        when(waitingQueueRepository.scanWaitingCourseIds()).thenReturn(Set.of(1L));
        when(waitingQueueRepository.getWaitingQueueSize(1L)).thenReturn(0L);

        scheduler.promoteWaitingToActive();

        verify(waitingQueueRepository).removeExpiredFromActiveQueue(eq(1L), anyLong());
        verify(waitingQueueRepository, never()).promoteFromWaiting(anyLong(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("스케줄러 실행 - 강의가 2개이면 각 강의에 2100/2 = 1050명씩 할당한다")
    void promoteWaitingToActive_splits_quota_evenly_across_courses() {
        when(waitingQueueRepository.scanWaitingCourseIds()).thenReturn(Set.of(1L, 2L));
        when(waitingQueueRepository.getWaitingQueueSize(1L)).thenReturn(500L);
        when(waitingQueueRepository.getWaitingQueueSize(2L)).thenReturn(500L);
        when(waitingQueueRepository.promoteFromWaiting(anyLong(), eq(1050), anyLong())).thenReturn(500L);

        scheduler.promoteWaitingToActive();

        verify(waitingQueueRepository, times(2)).promoteFromWaiting(anyLong(), eq(1050), anyLong());
    }

    @Test
    @DisplayName("스케줄러 실행 - 강의가 3000개이면 강의당 최소 1명이 할당된다")
    void promoteWaitingToActive_guarantees_minimum_one_per_course() {
        Set<Long> courseIds = new HashSet<>();
        for (long i = 1; i <= 3000; i++) {
            courseIds.add(i);
            when(waitingQueueRepository.getWaitingQueueSize(i)).thenReturn(10L);
        }
        when(waitingQueueRepository.scanWaitingCourseIds()).thenReturn(courseIds);

        ArgumentCaptor<Integer> promoteCountCaptor = ArgumentCaptor.forClass(Integer.class);
        when(waitingQueueRepository.promoteFromWaiting(anyLong(), promoteCountCaptor.capture(), anyLong())).thenReturn(1L);

        scheduler.promoteWaitingToActive();

        assertThat(promoteCountCaptor.getAllValues()).allMatch(count -> count >= 1);
    }

    @Test
    @DisplayName("스케줄러 실행 - active 큐로 이동 시 expiredAt은 현재 시간 + 5분이다")
    void promoteWaitingToActive_sets_expiredAt_as_five_minutes_from_now() {
        when(waitingQueueRepository.scanWaitingCourseIds()).thenReturn(Set.of(1L));
        when(waitingQueueRepository.getWaitingQueueSize(1L)).thenReturn(10L);

        ArgumentCaptor<Long> expiredAtCaptor = ArgumentCaptor.forClass(Long.class);
        when(waitingQueueRepository.promoteFromWaiting(eq(1L), anyInt(), expiredAtCaptor.capture())).thenReturn(10L);

        long before = System.currentTimeMillis();
        scheduler.promoteWaitingToActive();
        long after = System.currentTimeMillis();

        long capturedExpiredAt = expiredAtCaptor.getValue();
        long fiveMinutesMs = 5 * 60 * 1000L;

        assertThat(capturedExpiredAt).isBetween(before + fiveMinutesMs, after + fiveMinutesMs);
    }
}
