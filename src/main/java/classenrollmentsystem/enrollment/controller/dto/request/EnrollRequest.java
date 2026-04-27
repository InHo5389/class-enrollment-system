package classenrollmentsystem.enrollment.controller.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EnrollRequest {

    private Long courseId;

    public static EnrollRequest from(Long courseId) {
        EnrollRequest request = new EnrollRequest();
        request.courseId = courseId;
        return request;
    }

}
