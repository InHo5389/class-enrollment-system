package classenrollmentsystem.course.service.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class CreateCourseDto {

    private Long userId;
    private String title;
    private String description;
    private BigDecimal price;
    private int maxCapacity;
    private LocalDate startDate;
    private LocalDate endDate;

}
