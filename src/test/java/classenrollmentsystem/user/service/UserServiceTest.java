package classenrollmentsystem.user.service;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.user.controller.dto.request.LoginRequest;
import classenrollmentsystem.user.controller.dto.request.RegisterCreatorRequest;
import classenrollmentsystem.user.controller.dto.request.SignUpRequest;
import classenrollmentsystem.user.entity.CreatorProfile;
import classenrollmentsystem.user.entity.User;
import classenrollmentsystem.user.service.dto.CreatorProfileDto;
import classenrollmentsystem.user.service.dto.LoginDto;
import classenrollmentsystem.user.service.dto.RegisterCreatorDto;
import classenrollmentsystem.user.service.dto.SignUpDto;
import classenrollmentsystem.user.service.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CreatorProfileRepository creatorProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 - 새로운 사용자를 등록할 수 있다")
    void signUp_success() {
        // given
        SignUpDto signUpDto = SignUpRequest.from("test@example.com", "password123", "Test User").toDto();
        User savedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .name("Test User")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // when
        UserDto response = userService.signUp(signUpDto);

        // then
        assertThat(response)
                .isNotNull()
                .extracting("email", "name")
                .containsExactly("test@example.com", "Test User");
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("회원가입 - 중복된 이메일로 가입하면 CustomGlobalException을 발생시킨다")
    void signUp_fail_duplicate_email() {
        // given
        SignUpDto signUpDto = SignUpRequest.from("duplicate@example.com", "password123", "User").toDto();

        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signUp(signUpDto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("로그인 - 정확한 이메일과 비밀번호로 로그인할 수 있다")
    void login_success() {
        // given
        LoginDto loginDto = LoginRequest.from("login@example.com", "password123").toDto();
        String encodedPassword = passwordEncoder.encode("password123");
        User savedUser = User.builder()
                .id(1L)
                .email("login@example.com")
                .passwordHash(encodedPassword)
                .name("Login User")
                .build();

        when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password123", encodedPassword)).thenReturn(true);

        // when
        UserDto response = userService.login(loginDto);

        // then
        assertThat(response)
                .isNotNull()
                .extracting("email", "name")
                .containsExactly("login@example.com", "Login User");
    }

    @Test
    @DisplayName("로그인 - 존재하지 않는 이메일로 로그인하면 CustomGlobalException을 발생시킨다")
    void login_fail_user_not_found() {
        // given
        LoginDto loginDto = LoginRequest.from("nonexistent@example.com", "password123").toDto();

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.login(loginDto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_EMAIL);
    }

    @Test
    @DisplayName("로그인 - 잘못된 비밀번호로 로그인하면 CustomGlobalException을 발생시킨다")
    void login_fail_wrong_password() {
        // given
        LoginDto loginDto = LoginRequest.from("user@example.com", "wrongPassword").toDto();
        String correctEncodedPassword = passwordEncoder.encode("correctPassword");
        User savedUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash(correctEncodedPassword)
                .name("User")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(savedUser));

        // when & then
        assertThatThrownBy(() -> userService.login(loginDto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("사용자 조회 - ID로 사용자를 조회할 수 있다")
    void getUserById_success() {
        // given
        User savedUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("hashedPassword")
                .name("Test User")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(savedUser));

        // when
        UserDto response = userService.getUserById(1L);

        // then
        assertThat(response)
                .isNotNull()
                .extracting("email", "name")
                .containsExactly("user@example.com", "Test User");
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("사용자 조회 - 존재하지 않는 ID로 조회하면 CustomGlobalException을 발생시킨다")
    void getUserById_fail_user_not_found() {
        // given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("크리에이터 등록 - 사용자를 크리에이터로 등록할 수 있다")
    void registerCreator_success() {
        // given
        String bio = "안녕하세요, 강사입니다.";
        RegisterCreatorDto dto = RegisterCreatorDto.of(1L, bio);
        User user = User.builder()
                .id(1L)
                .email("creator@example.com")
                .passwordHash("hashedPassword")
                .name("Creator User")
                .build();
        CreatorProfile creatorProfile = CreatorProfile.create(user, bio);

        when(creatorProfileRepository.existsByUserId(1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(creatorProfileRepository.save(any(CreatorProfile.class))).thenReturn(creatorProfile);

        // when
        CreatorProfileDto response = userService.registerCreator(dto);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getBio()).isEqualTo(bio);
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("크리에이터 등록 - 이미 크리에이터로 등록된 사용자는 CustomGlobalException을 발생시킨다")
    void registerCreator_fail_duplicate_creator_profile() {
        // given
        RegisterCreatorDto dto = RegisterCreatorDto.of(1L, "bio");

        when(creatorProfileRepository.existsByUserId(1L)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.registerCreator(dto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.DUPLICATE_CREATOR_PROFILE);
    }

    @Test
    @DisplayName("크리에이터 등록 - 존재하지 않는 사용자로 등록하면 CustomGlobalException을 발생시킨다")
    void registerCreator_fail_user_not_found() {
        // given
        RegisterCreatorDto dto = RegisterCreatorDto.of(999L, "bio");

        when(creatorProfileRepository.existsByUserId(999L)).thenReturn(false);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.registerCreator(dto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.USER_NOT_FOUND);
    }
}
