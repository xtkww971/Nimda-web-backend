package com.nimda.cite.user.service;

import com.nimda.cite.common.exception.AlreadyExistException;
import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.common.exception.errorcode.user.UserErrorCode;
import com.nimda.cite.config.CheckConfig;
import com.nimda.cite.domain.point.entity.UserBalance;
import com.nimda.cite.domain.point.repositroy.UserBalanceRepository;
import com.nimda.cite.user.dto.register.RegisterRequestDTO;
import com.nimda.cite.user.dto.register.RegisterResponseDTO;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 일반 사용자용 서비스
 * 
 * [역할]
 * - 회원가입
 * - 사용자 정보 조회/수정
 * - 중복 확인
 * 
 * [책임 분리]
 * - 일반 사용자 기능: UserService
 * - 관리자 기능: AdminUserService
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final PasswordEncoder passwordEncoder; // Spring Security 비밀번호 암호화 도구

    /*
     * CreateUser : 회원 가입
     */
    @Transactional
    public RegisterResponseDTO createUser(RegisterRequestDTO registerRequestDTO) {
        // input npe check
        CheckConfig.npeCheck(registerRequestDTO, "registerRequestDTO");

        //Validation Check -> userId, nickname, email 중복 시 예외 던지기
        if(userRepository.existsByUserId(registerRequestDTO.getUserId())
        || userRepository.existsByEmail(registerRequestDTO.getEmail())
        || userRepository.existsByNickname(registerRequestDTO.getNickname())){
            throw new AlreadyExistException(UserErrorCode.USER_ALREADY_EXIST);
        }

        String encodedPassword = passwordEncoder.encode(registerRequestDTO.getPassword());

        // ERD 기반 필수 필드로 사용자 생성
        User user = User.builder()
                .userId(registerRequestDTO.getUserId())
                .name(registerRequestDTO.getName())
                .nickname(registerRequestDTO.getNickname())
                .password(encodedPassword)
                .studentNum(registerRequestDTO.getStudentNum())
                .email(registerRequestDTO.getEmail())
                .major(registerRequestDTO.getMajor())
                .birth(registerRequestDTO.getBirth())
                .bojId(registerRequestDTO.getBojId())
                .build();

        // 승인 전까지 권한 없이 생성 (기본값: status = PENDING)
        // 승인 시 AdminUserController에서 ROLE_USER 권한 부여

        // 유저 계좌 생성
        UserBalance userBalance = UserBalance.builder()
                .user(user)
                .totalAmount(0L)
                .updatedAt(LocalDateTime.now())
                .build();
        userBalanceRepository.save(userBalance);

        return new RegisterResponseDTO(userRepository.save(user));
    }

    // 사용자 정보 업데이트
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    // 닉네임으로 사용자 조회 (공개 프로필)
    public User getUserByNickname(String nickname) {
        return userRepository.findByNickname(nickname)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));
    }
}
