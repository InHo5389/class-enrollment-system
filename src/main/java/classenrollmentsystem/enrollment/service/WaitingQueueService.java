package classenrollmentsystem.enrollment.service;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.course.entity.Course;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.service.CourseRepository;
import classenrollmentsystem.enrollment.service.dto.QueueStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingQueueService {

    private static final long ACTIVE_TOKEN_TTL_MS = 5 * 60 * 1000L;
    private static final int IMMEDIATE_PASS_THRESHOLD = 2100;

    private final WaitingQueueRepository waitingQueueRepository;
    private final CourseRepository courseRepository;

    public QueueStatusDto enterQueue(Long courseId, Long userId) {
        log.info("대기열 진입 요청 - 강의 ID: {}, 사용자 ID: {}", courseId, userId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.OPEN) {
            throw new CustomGlobalException(ErrorType.ENROLLMENT_COURSE_NOT_OPEN);
        }

        if (waitingQueueRepository.isInActiveQueue(courseId, userId)) {
            log.debug("이미 활성 토큰 보유 - 강의 ID: {}, 사용자 ID: {}", courseId, userId);
            return QueueStatusDto.active(courseId, userId);
        }

        Long existingRank = waitingQueueRepository.getWaitingRank(courseId, userId);
        if (existingRank != null) {
            Long totalWaiting = waitingQueueRepository.getWaitingQueueSize(courseId);
            return QueueStatusDto.waiting(courseId, userId, existingRank, totalWaiting);
        }

        Long waitingSize = waitingQueueRepository.getWaitingQueueSize(courseId);
        Long activeSize = waitingQueueRepository.getActiveQueueSize(courseId);

        if (waitingSize == 0 && activeSize < IMMEDIATE_PASS_THRESHOLD) {
            long expiredAt = System.currentTimeMillis() + ACTIVE_TOKEN_TTL_MS;
            waitingQueueRepository.addToActiveQueue(courseId, userId, expiredAt);
            log.info("대기열 즉시 통과 (비인기 강의) - 강의 ID: {}, 사용자 ID: {}", courseId, userId);
            return QueueStatusDto.active(courseId, userId);
        }

        long timestamp = System.currentTimeMillis();
        waitingQueueRepository.addToWaitingQueue(courseId, userId, timestamp);

        Long rank = waitingQueueRepository.getWaitingRank(courseId, userId);
        Long totalWaiting = waitingQueueRepository.getWaitingQueueSize(courseId);

        log.info("대기열 진입 완료 - 강의 ID: {}, 사용자 ID: {}, 대기 순위: {}", courseId, userId, rank != null ? rank + 1 : -1);
        return QueueStatusDto.waiting(courseId, userId, rank != null ? rank : 0L, totalWaiting);
    }

    public QueueStatusDto getQueueStatus(Long courseId, Long userId) {
        if (waitingQueueRepository.isInActiveQueue(courseId, userId)) {
            return QueueStatusDto.active(courseId, userId);
        }

        Long rank = waitingQueueRepository.getWaitingRank(courseId, userId);
        if (rank != null) {
            Long totalWaiting = waitingQueueRepository.getWaitingQueueSize(courseId);
            return QueueStatusDto.waiting(courseId, userId, rank, totalWaiting);
        }

        return QueueStatusDto.notInQueue(courseId, userId);
    }

    public void validateActiveToken(Long courseId, Long userId) {
        if (!waitingQueueRepository.isInActiveQueue(courseId, userId)) {
            log.warn("활성 토큰 없이 수강 신청 시도 - 강의 ID: {}, 사용자 ID: {}", courseId, userId);
            throw new CustomGlobalException(ErrorType.QUEUE_TOKEN_NOT_ACTIVE);
        }
    }

    public void releaseActiveToken(Long courseId, Long userId) {
        waitingQueueRepository.removeFromActiveQueue(courseId, userId);
        log.debug("활성 토큰 반환 - 강의 ID: {}, 사용자 ID: {}", courseId, userId);
    }


}
