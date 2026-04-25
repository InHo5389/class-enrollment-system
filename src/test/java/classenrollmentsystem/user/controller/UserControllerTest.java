package classenrollmentsystem.user.controller;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.user.controller.dto.request.LoginRequest;
import classenrollmentsystem.user.controller.dto.request.SignUpRequest;
import classenrollmentsystem.user.service.UserService;
import classenrollmentsystem.user.service.dto.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("회원가입 - 올바른 요청으로 회원가입할 수 있다")
    void signUp_success() throws Exception {
        SignUpRequest request = SignUpRequest.from("test@example.com", "password123", "Test User");
        UserDto userDto = UserDto.builder().id(1L).email("test@example.com").name("Test User").build();

        Mockito.when(userService.signUp(any())).thenReturn(userDto);

        mockMvc.perform(post("/api/users/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    @DisplayName("회원가입 - 중복된 이메일로 가입하면 400 에러가 반환된다")
    void signUp_fail_duplicate_email() throws Exception {
        SignUpRequest request = SignUpRequest.from("duplicate@example.com", "password123", "User");
        Mockito.when(userService.signUp(any())).thenThrow(new CustomGlobalException(ErrorType.DUPLICATE_EMAIL));

        mockMvc.perform(post("/api/users/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorType.DUPLICATE_EMAIL.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.DUPLICATE_EMAIL.getMessage()));
    }

    @Test
    @DisplayName("로그인 - 올바른 이메일과 비밀번호로 로그인할 수 있다")
    void login_success() throws Exception {
        LoginRequest request = LoginRequest.from("login@example.com", "password123");
        UserDto userDto = UserDto.builder().id(1L).email("login@example.com").name("Login User").build();

        Mockito.when(userService.login(any())).thenReturn(userDto);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("login@example.com"));
    }

    @Test
    @DisplayName("로그인 - 잘못된 정보로 로그인 시 400 에러가 반환된다")
    void login_fail_invalid_credentials() throws Exception {
        LoginRequest request = LoginRequest.from("user@example.com", "wrongPassword");
        Mockito.when(userService.login(any())).thenThrow(new CustomGlobalException(ErrorType.INVALID_PASSWORD));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorType.INVALID_PASSWORD.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.INVALID_PASSWORD.getMessage()));
    }

    @Test
    @DisplayName("사용자 조회 - 올바른 사용자 ID로 조회할 수 있다")
    void getUserById_success() throws Exception {
        UserDto userDto = UserDto.builder().id(1L).email("user@example.com").name("Test User").build();
        Mockito.when(userService.getUserById(1L)).thenReturn(userDto);

        mockMvc.perform(get("/api/users/me")
                        .header("X-USER-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("사용자 조회 - 헤더가 없이 사용자 ID로 조회하면 401예외가 발생한다.")
    void getUserById_noHeader_fail() throws Exception {
        UserDto userDto = UserDto.builder().id(1L).email("user@example.com").name("Test User").build();
        Mockito.when(userService.getUserById(1L)).thenReturn(userDto);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorType.MISSING_USER_ID_HEADER.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorType.MISSING_USER_ID_HEADER.getMessage()));
    }
}