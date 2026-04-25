package classenrollmentsystem.course.controller.dto.response;

import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.service.dto.CourseDto;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CourseResponse {

    private Long id;
    private String creatorName;
    private String title;
    private String description;
    private BigDecimal price;
    private int maxCapacity;
    private int currentEnrollment;
    private int availableSeats;
    private LocalDate startDate;
    private LocalDate endDate;
    private CourseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CourseResponse from(CourseDto dto) {
        return CourseResponse.builder()
                .id(dto.getId())
                .creatorName(dto.getCreatorName())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .maxCapacity(dto.getMaxCapacity())
                .currentEnrollment(dto.getCurrentEnrollment())
                .availableSeats(dto.getMaxCapacity() - dto.getCurrentEnrollment())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(dto.getStatus())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

}
