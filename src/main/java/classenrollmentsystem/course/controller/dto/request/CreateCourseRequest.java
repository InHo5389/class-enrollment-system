package classenrollmentsystem.course.controller.dto.request;

import classenrollmentsystem.course.service.dto.CreateCourseDto;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CreateCourseRequest {

    private String title;
    private String description;
    private BigDecimal price;
    private int maxCapacity;
    private LocalDate startDate;
    private LocalDate endDate;

    public CreateCourseDto toDto(Long userId) {
        return CreateCourseDto.builder()
                .userId(userId)
                .title(this.title)
                .description(this.description)
                .price(this.price)
                .maxCapacity(this.maxCapacity)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .build();
    }

}
