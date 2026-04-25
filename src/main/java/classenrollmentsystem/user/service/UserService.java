package classenrollmentsystem.user.service;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.user.entity.CreatorProfile;
import classenrollmentsystem.user.service.dto.CreatorProfileDto;
import classenrollmentsystem.user.service.dto.LoginDto;
import classenrollmentsystem.user.service.dto.RegisterCreatorDto;
import classenrollmentsystem.user.service.dto.SignUpDto;
import classenrollmentsystem.user.service.dto.UserDto;
import classenrollmentsystem.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDto signUp(SignUpDto signUpDto) {
        log.info("회원가입 시작 - 이메일: {}", signUpDto.getEmail());

        if (userRepository.existsByEmail(signUpDto.getEmail())) {
            log.info("중복된 이메일로 회원가입 시도 - 이메일: {}", signUpDto.getEmail());
            throw new CustomGlobalException(ErrorType.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(signUpDto.getPassword());

        User user = User.create(signUpDto.getEmail(), encodedPassword, signUpDto.getName());

        User savedUser = userRepository.save(user);
        log.info("회원가입 완료 - 사용자 ID: {}, 이메일: {}", savedUser.getId(), savedUser.getEmail());
        return UserDto.from(savedUser);
    }

    public UserDto login(LoginDto loginDto) {
        log.info("로그인 시작 - 이메일: {}", loginDto.getEmail());

        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 이메일로 로그인 시도 - 이메일: {}", loginDto.getEmail());
                    return new CustomGlobalException(ErrorType.INVALID_EMAIL);
                });

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPasswordHash())) {
            log.warn("잘못된 비밀번호로 로그인 시도 - 이메일: {}", loginDto.getEmail());
            throw new CustomGlobalException(ErrorType.INVALID_PASSWORD);
        }

        log.info("로그인 완료 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
        return UserDto.from(user);
    }

    public UserDto getUserById(Long userId) {
        log.debug("사용자 조회 - 사용자 ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자 조회 시도 - 사용자 ID: {}", userId);
                    return new CustomGlobalException(ErrorType.USER_NOT_FOUND);
                });

        log.info("사용자 조회 성공 - 사용자 ID: {}, 이메일: {}", user.getId(), user.getEmail());
        return UserDto.from(user);
    }

    public CreatorProfileDto registerCreator(RegisterCreatorDto dto) {
        log.info("크리에이터 등록 시작 - 사용자 ID: {}", dto.getUserId());

        if (creatorProfileRepository.existsByUserId(dto.getUserId())) {
            log.warn("이미 크리에이터로 등록된 사용자 - 사용자 ID: {}", dto.getUserId());
            throw new CustomGlobalException(ErrorType.DUPLICATE_CREATOR_PROFILE);
        }

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자의 크리에이터 등록 시도 - 사용자 ID: {}", dto.getUserId());
                    return new CustomGlobalException(ErrorType.USER_NOT_FOUND);
                });

        CreatorProfile creatorProfile = CreatorProfile.create(user, dto.getBio());
        CreatorProfile saved = creatorProfileRepository.save(creatorProfile);

        log.info("크리에이터 등록 완료 - 사용자 ID: {}, 크리에이터 프로필 ID: {}", dto.getUserId(), saved.getId());
        return CreatorProfileDto.from(saved);
    }

}
