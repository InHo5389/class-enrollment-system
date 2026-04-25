package classenrollmentsystem.user.service.dto;

import classenrollmentsystem.user.entity.CreatorProfile;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatorProfileDto {

    private Long id;
    private Long userId;
    private String bio;

    public static CreatorProfileDto from(CreatorProfile creatorProfile) {
        return CreatorProfileDto.builder()
                .id(creatorProfile.getId())
                .userId(creatorProfile.getUser().getId())
                .bio(creatorProfile.getBio())
                .build();
    }

}
