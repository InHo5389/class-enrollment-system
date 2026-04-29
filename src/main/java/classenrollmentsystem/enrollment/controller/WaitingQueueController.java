package classenrollmentsystem.enrollment.controller;

import classenrollmentsystem.enrollment.controller.dto.response.QueueStatusResponse;
import classenrollmentsystem.enrollment.service.WaitingQueueService;
import classenrollmentsystem.enrollment.service.dto.QueueStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class WaitingQueueController {

    private final WaitingQueueService waitingQueueService;

    @PostMapping("/courses/{courseId}/enter")
    public ResponseEntity<QueueStatusResponse> enterQueue(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId
    ) {
        log.debug("대기열 진입 요청 - 강의 ID: {}, 사용자 ID: {}", courseId, userId);
        QueueStatusDto dto = waitingQueueService.enterQueue(courseId, userId);
        return ResponseEntity.ok(QueueStatusResponse.from(dto));
    }

    @GetMapping("/courses/{courseId}/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId
    ) {
        log.debug("대기열 상태 폴링 - 강의 ID: {}, 사용자 ID: {}", courseId, userId);
        QueueStatusDto dto = waitingQueueService.getQueueStatus(courseId, userId);
        return ResponseEntity.ok(QueueStatusResponse.from(dto));
    }

}
