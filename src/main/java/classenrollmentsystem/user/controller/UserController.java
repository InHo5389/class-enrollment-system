package classenrollmentsystem.user.controller;

import classenrollmentsystem.common.interceptor.annotation.PublicApi;
import classenrollmentsystem.user.controller.dto.request.LoginRequest;
import classenrollmentsystem.user.controller.dto.request.RegisterCreatorRequest;
import classenrollmentsystem.user.controller.dto.request.SignUpRequest;
import classenrollmentsystem.user.controller.dto.response.CreatorProfileResponse;
import classenrollmentsystem.user.controller.dto.response.SignUpResponse;
import classenrollmentsystem.user.controller.dto.response.LoginResponse;
import classenrollmentsystem.user.controller.dto.response.UserResponse;
import classenrollmentsystem.user.service.UserService;
import classenrollmentsystem.user.service.dto.CreatorProfileDto;
import classenrollmentsystem.user.service.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PublicApi
    @PostMapping("/sign-up")
    public ResponseEntity<SignUpResponse> signUp(@RequestBody SignUpRequest request) {
        log.debug("회원가입 요청 - 이메일: {}", request.getEmail());
        UserDto userDto = userService.signUp(request.toDto());

        return ResponseEntity.ok(SignUpResponse.from(userDto));
    }

    @PublicApi
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        log.debug("로그인 요청 - 이메일: {}", request.getEmail());
        UserDto userDto = userService.login(request.toDto());

        return ResponseEntity.ok(LoginResponse.from(userDto));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@RequestHeader("X-User-Id") Long userId) {
        log.debug("사용자 프로필 조회 요청 - 사용자 ID: {}", userId);
        UserDto userDto = userService.getUserById(userId);

        return ResponseEntity.ok(UserResponse.from(userDto));
    }

    @PostMapping("/creator")
    public ResponseEntity<CreatorProfileResponse> registerCreator(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody RegisterCreatorRequest request
    ) {
        log.debug("크리에이터 등록 요청 - 사용자 ID: {}", userId);
        CreatorProfileDto creatorProfileDto = userService.registerCreator(request.toDto(userId));

        return ResponseEntity.ok(CreatorProfileResponse.from(creatorProfileDto));
    }

}
