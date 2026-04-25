package classenrollmentsystem.user.controller.dto.response;

import classenrollmentsystem.user.service.dto.CreatorProfileDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatorProfileResponse {

    private Long id;
    private Long userId;
    private String bio;

    public static CreatorProfileResponse from(CreatorProfileDto dto) {
        return CreatorProfileResponse.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .bio(dto.getBio())
                .build();
    }

}
