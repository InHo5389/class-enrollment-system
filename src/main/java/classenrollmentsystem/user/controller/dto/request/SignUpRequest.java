package classenrollmentsystem.user.controller.dto.request;

import classenrollmentsystem.user.service.dto.SignUpDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {

    private String email;
    private String password;
    private String name;

    public static SignUpRequest from(String email, String password, String name) {
        return SignUpRequest.builder()
                .email(email)
                .password(password)
                .name(name)
                .build();
    }

    public SignUpDto toDto() {
        return SignUpDto.builder()
                .email(this.email)
                .password(this.password)
                .name(this.name)
                .build();
    }

}
