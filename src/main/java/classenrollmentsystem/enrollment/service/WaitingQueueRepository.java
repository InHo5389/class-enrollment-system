package classenrollmentsystem.enrollment.service;

import java.util.Set;

public interface WaitingQueueRepository {

    Set<Long> scanWaitingCourseIds();

    void addToWaitingQueue(Long courseId, Long userId, long timestamp);

    Long getWaitingRank(Long courseId, Long userId);

    Long getWaitingQueueSize(Long courseId);

    void addToActiveQueue(Long courseId, Long userId, long expiredAt);

    boolean isInActiveQueue(Long courseId, Long userId);

    Long getActiveQueueSize(Long courseId);

    void removeFromActiveQueue(Long courseId, Long userId);

    void removeExpiredFromActiveQueue(Long courseId, long currentTime);

    long promoteFromWaiting(Long courseId, int count, long expiredAt);

    void removeFromWaitingQueue(Long courseId, Long userId);

}
