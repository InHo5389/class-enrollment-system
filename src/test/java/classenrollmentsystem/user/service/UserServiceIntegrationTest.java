package classenrollmentsystem.user.service;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.user.controller.dto.request.LoginRequest;
import classenrollmentsystem.user.controller.dto.request.SignUpRequest;
import classenrollmentsystem.user.entity.User;
import classenrollmentsystem.user.repository.CreatorProfileJpaRepository;
import classenrollmentsystem.user.repository.UserJpaRepository;
import classenrollmentsystem.user.service.dto.CreatorProfileDto;
import classenrollmentsystem.user.service.dto.LoginDto;
import classenrollmentsystem.user.service.dto.RegisterCreatorDto;
import classenrollmentsystem.user.service.dto.SignUpDto;
import classenrollmentsystem.user.service.dto.UserDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private CreatorProfileJpaRepository creatorProfileJpaRepository;

    @AfterEach
    void tearDown() {
        creatorProfileJpaRepository.deleteAllInBatch();
        userJpaRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("회원가입 - 새로운 사용자를 등록할 수 있다")
    void signUp_success() {
        // given
        SignUpDto signUpDto = SignUpRequest.from("test@example.com", "password123", "Test User").toDto();

        // when
        UserDto response = userService.signUp(signUpDto);

        // then
        assertThat(response)
                .isNotNull()
                .extracting("email", "name")
                .containsExactly("test@example.com", "Test User");
        assertThat(response.getId()).isNotNull();

        User savedUser = userJpaRepository.findById(response.getId()).orElseThrow();
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("회원가입 - 중복된 이메일로 가입하면 CustomGlobalException을 발생시킨다")
    void signUp_fail_duplicate_email() {
        // given
        SignUpDto firstDto = SignUpRequest.from("duplicate@example.com", "password123", "User").toDto();
        userService.signUp(firstDto);

        SignUpDto secondDto = SignUpRequest.from("duplicate@example.com", "password456", "Another User").toDto();

        // when & then
        assertThatThrownBy(() -> userService.signUp(secondDto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("로그인 - 정확한 이메일과 비밀번호로 로그인할 수 있다")
    void login_success() {
        // given
        SignUpDto signUpDto = SignUpRequest.from("login@example.com", "password123", "Login User").toDto();
        userService.signUp(signUpDto);

        LoginDto loginDto = LoginRequest.from("login@example.com", "password123").toDto();

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
        SignUpDto signUpDto = SignUpRequest.from("user@example.com", "correctPassword", "User").toDto();
        userService.signUp(signUpDto);

        LoginDto loginDto = LoginRequest.from("user@example.com", "wrongPassword").toDto();

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
        SignUpDto signUpDto = SignUpRequest.from("user@example.com", "password123", "Test User").toDto();
        UserDto signUpResponse = userService.signUp(signUpDto);

        // when
        UserDto response = userService.getUserById(signUpResponse.getId());

        // then
        assertThat(response)
                .isNotNull()
                .extracting("email", "name")
                .containsExactly("user@example.com", "Test User");
        assertThat(response.getId()).isEqualTo(signUpResponse.getId());
    }

    @Test
    @DisplayName("사용자 조회 - 존재하지 않는 ID로 조회하면 CustomGlobalException을 발생시킨다")
    void getUserById_fail_user_not_found() {
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
        UserDto userDto = userService.signUp(SignUpRequest.from("creator@example.com", "password123", "Creator").toDto());
        RegisterCreatorDto dto = RegisterCreatorDto.of(userDto.getId(), "안녕하세요, 강사입니다.");

        // when
        CreatorProfileDto response = userService.registerCreator(dto);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userDto.getId());
        assertThat(response.getBio()).isEqualTo("안녕하세요, 강사입니다.");
        assertThat(response.getId()).isNotNull();

        assertThat(creatorProfileJpaRepository.existsByUserId(userDto.getId())).isTrue();
    }

    @Test
    @DisplayName("크리에이터 등록 - 이미 크리에이터로 등록된 사용자는 CustomGlobalException을 발생시킨다")
    void registerCreator_fail_duplicate_creator_profile() {
        // given
        UserDto userDto = userService.signUp(SignUpRequest.from("creator@example.com", "password123", "Creator").toDto());
        RegisterCreatorDto dto = RegisterCreatorDto.of(userDto.getId(), "첫 번째 등록");
        userService.registerCreator(dto);

        RegisterCreatorDto duplicateDto = RegisterCreatorDto.of(userDto.getId(), "두 번째 등록 시도");

        // when & then
        assertThatThrownBy(() -> userService.registerCreator(duplicateDto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.DUPLICATE_CREATOR_PROFILE);
    }

    @Test
    @DisplayName("크리에이터 등록 - 존재하지 않는 사용자로 등록하면 CustomGlobalException을 발생시킨다")
    void registerCreator_fail_user_not_found() {
        // given
        RegisterCreatorDto dto = RegisterCreatorDto.of(999L, "bio");

        // when & then
        assertThatThrownBy(() -> userService.registerCreator(dto))
                .isInstanceOf(CustomGlobalException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.USER_NOT_FOUND);
    }
}
