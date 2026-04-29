package classenrollmentsystem.enrollment.controller.dto.response;

import classenrollmentsystem.enrollment.service.dto.QueueStatusDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QueueStatusResponse {

    private Long courseId;
    private Long userId;
    private String status;
    private Long rank;
    private Long totalWaiting;
    private String message;

    public static QueueStatusResponse from(QueueStatusDto dto) {
        String message = switch (dto.getStatus()) {
            case WAITING -> String.format("현재 %d번째 대기 중입니다. (총 대기 인원: %d명)", dto.getRank(), dto.getTotalWaiting());
            case ACTIVE -> "입장이 허가되었습니다. 5분 이내에 수강 신청을 완료해 주세요.";
            case NOT_IN_QUEUE -> "대기열에 등록되지 않은 상태입니다.";
        };

        return QueueStatusResponse.builder()
                .courseId(dto.getCourseId())
                .userId(dto.getUserId())
                .status(dto.getStatus().name())
                .rank(dto.getRank())
                .totalWaiting(dto.getTotalWaiting())
                .message(message)
                .build();
    }

}
