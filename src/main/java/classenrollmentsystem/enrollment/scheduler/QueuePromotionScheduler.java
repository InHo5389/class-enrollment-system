package classenrollmentsystem.enrollment.scheduler;

import classenrollmentsystem.enrollment.service.WaitingQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueuePromotionScheduler {

    private static final int TOTAL_PROMOTE_PER_MINUTE = 2100;
    private static final int MIN_PROMOTE_PER_COURSE = 1;
    private static final long ACTIVE_TOKEN_TTL_MS = 5 * 60 * 1000L;

    private final WaitingQueueRepository waitingQueueRepository;

    @Scheduled(fixedDelay = 60_000)
    public void promoteWaitingToActive() {
        long now = System.currentTimeMillis();

        Set<Long> waitingCourseIds = waitingQueueRepository.scanWaitingCourseIds();
        if (waitingCourseIds.isEmpty()) {
            return;
        }

        int activeCourseCount = waitingCourseIds.size();
        int promotePerCourse = Math.max(TOTAL_PROMOTE_PER_MINUTE / activeCourseCount, MIN_PROMOTE_PER_COURSE);

        for (Long courseId : waitingCourseIds) {
            waitingQueueRepository.removeExpiredFromActiveQueue(courseId, now);

            Long waitingSize = waitingQueueRepository.getWaitingQueueSize(courseId);
            if (waitingSize == null || waitingSize == 0) {
                continue;
            }

            long expiredAt = now + ACTIVE_TOKEN_TTL_MS;
            long promoted = waitingQueueRepository.promoteFromWaiting(courseId, promotePerCourse, expiredAt);
            if (promoted > 0) {
                log.info("대기열 → 활성 큐 이동 완료 - 강의 ID: {}, 이동 인원: {}, 남은 대기: {}, 강의 수: {}",
                        courseId, promoted, waitingQueueRepository.getWaitingQueueSize(courseId), activeCourseCount);
            }
        }
    }

}
