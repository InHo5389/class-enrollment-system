package classenrollmentsystem.user.controller.dto.request;

import classenrollmentsystem.user.service.dto.LoginDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    private String email;
    private String password;

    public static LoginRequest from(String email, String password) {
        return LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
    }

    public LoginDto toDto() {
        return LoginDto.builder()
                .email(this.email)
                .password(this.password)
                .build();
    }

}
