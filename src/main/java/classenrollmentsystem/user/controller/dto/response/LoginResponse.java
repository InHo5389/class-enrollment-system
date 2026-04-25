package classenrollmentsystem.user.controller.dto.response;

import classenrollmentsystem.user.entity.User;
import classenrollmentsystem.user.service.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private Long id;
    private String email;
    private String name;

    public static LoginResponse from(User user) {
        return LoginResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    public static LoginResponse from(UserDto userDto) {
        return LoginResponse.builder()
                .id(userDto.getId())
                .email(userDto.getEmail())
                .name(userDto.getName())
                .build();
    }

}
