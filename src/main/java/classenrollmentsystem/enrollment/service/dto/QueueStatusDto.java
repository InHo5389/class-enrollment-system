package classenrollmentsystem.enrollment.service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QueueStatusDto {

    private Long courseId;
    private Long userId;
    private QueueStatus status;
    private Long rank;
    private Long totalWaiting;

    public enum QueueStatus {
        WAITING,
        ACTIVE,
        NOT_IN_QUEUE
    }

    public static QueueStatusDto waiting(Long courseId, Long userId, Long rank, Long totalWaiting) {
        return QueueStatusDto.builder()
                .courseId(courseId)
                .userId(userId)
                .status(QueueStatus.WAITING)
                .rank(rank + 1)
                .totalWaiting(totalWaiting)
                .build();
    }

    public static QueueStatusDto active(Long courseId, Long userId) {
        return QueueStatusDto.builder()
                .courseId(courseId)
                .userId(userId)
                .status(QueueStatus.ACTIVE)
                .rank(0L)
                .totalWaiting(0L)
                .build();
    }

    public static QueueStatusDto notInQueue(Long courseId, Long userId) {
        return QueueStatusDto.builder()
                .courseId(courseId)
                .userId(userId)
                .status(QueueStatus.NOT_IN_QUEUE)
                .rank(null)
                .totalWaiting(null)
                .build();
    }

}
