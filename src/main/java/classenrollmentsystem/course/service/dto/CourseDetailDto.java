package classenrollmentsystem.course.service.dto;

import classenrollmentsystem.course.entity.Course;
import classenrollmentsystem.course.entity.CourseStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CourseDetailDto {

    private Long id;
    private String creatorName;
    private String creatorBio;
    private String title;
    private String description;
    private BigDecimal price;
    private int maxCapacity;
    private int currentEnrollment;
    private LocalDate startDate;
    private LocalDate endDate;
    private CourseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CourseDetailDto of(Course course, int currentEnrollment) {
        return CourseDetailDto.builder()
                .id(course.getId())
                .creatorName(course.getCreatorProfile().getUser().getName())
                .creatorBio(course.getCreatorProfile().getBio())
                .title(course.getTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .maxCapacity(course.getMaxCapacity())
                .currentEnrollment(currentEnrollment)
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .status(course.getStatus())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

}
