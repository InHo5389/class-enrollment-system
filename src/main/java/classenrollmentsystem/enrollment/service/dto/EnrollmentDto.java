package classenrollmentsystem.enrollment.service.dto;

import classenrollmentsystem.enrollment.entity.Enrollment;
import classenrollmentsystem.enrollment.entity.EnrollmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EnrollmentDto {

    private Long id;
    private Long userId;
    private Long courseId;
    private String courseTitle;
    private String creatorName;
    private EnrollmentStatus status;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EnrollmentDto from(Enrollment enrollment) {
        return EnrollmentDto.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUser().getId())
                .courseId(enrollment.getCourse().getId())
                .courseTitle(enrollment.getCourse().getTitle())
                .creatorName(enrollment.getCourse().getCreatorProfile().getUser().getName())
                .status(enrollment.getStatus())
                .confirmedAt(enrollment.getConfirmedAt())
                .createdAt(enrollment.getCreatedAt())
                .updatedAt(enrollment.getUpdatedAt())
                .build();
    }

}
