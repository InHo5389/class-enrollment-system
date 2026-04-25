package classenrollmentsystem.user.controller.dto.request;

import classenrollmentsystem.user.service.dto.RegisterCreatorDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCreatorRequest {

    private String bio;

    public static RegisterCreatorRequest from(String bio) {
        return RegisterCreatorRequest.builder()
                .bio(bio)
                .build();
    }

    public RegisterCreatorDto toDto(Long userId) {
        return RegisterCreatorDto.of(userId, bio);
    }

}
