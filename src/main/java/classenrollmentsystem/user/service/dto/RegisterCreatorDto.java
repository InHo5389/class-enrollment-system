package classenrollmentsystem.user.service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterCreatorDto {

    private Long userId;
    private String bio;

    public static RegisterCreatorDto of(Long userId, String bio) {
        return RegisterCreatorDto.builder()
                .userId(userId)
                .bio(bio)
                .build();
    }

}
