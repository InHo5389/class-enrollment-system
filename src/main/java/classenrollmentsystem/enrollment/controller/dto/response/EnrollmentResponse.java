package classenrollmentsystem.enrollment.controller.dto.response;

import classenrollmentsystem.enrollment.entity.EnrollmentStatus;
import classenrollmentsystem.enrollment.service.dto.EnrollmentDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EnrollmentResponse {

    private Long id;
    private Long userId;
    private Long courseId;
    private String courseTitle;
    private String creatorName;
    private EnrollmentStatus status;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EnrollmentResponse from(EnrollmentDto dto) {
        return EnrollmentResponse.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .courseId(dto.getCourseId())
                .courseTitle(dto.getCourseTitle())
                .creatorName(dto.getCreatorName())
                .status(dto.getStatus())
                .confirmedAt(dto.getConfirmedAt())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

}
