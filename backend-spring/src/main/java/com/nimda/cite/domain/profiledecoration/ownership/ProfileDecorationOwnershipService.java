package com.nimda.cite.domain.profiledecoration.ownership;

import com.nimda.cite.common.exception.NotAllowFormException;
import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.common.exception.errorcode.domain.ProfileDecorationErrorCode;
import com.nimda.cite.common.exception.errorcode.user.UserErrorCode;
import com.nimda.cite.domain.profiledecoration.entity.ProfileDecoration;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileDecorationOwnershipService {

    private final UserProfileDecorationRepository repository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean owns(Long userId, Long profileDecorationId) {
        return repository.existsByUserIdAndProfileDecorationId(userId, profileDecorationId);
    }

    @Transactional
    public void grant(Long userId, ProfileDecoration profileDecoration) {
        if (profileDecoration == null || !profileDecoration.isActive()) {
            throw new NotAllowFormException(ProfileDecorationErrorCode.BADGE_NOT_USABLE);
        }
        if (owns(userId, profileDecoration.getId())) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND));
        repository.save(new UserProfileDecoration(user, profileDecoration));
    }

    @Transactional
    public void grantByStudentNum(String studentNum, ProfileDecoration profileDecoration) {
        User user = findUserByStudentNum(studentNum);
        grant(user.getId(), profileDecoration);
    }

    @Transactional
    public void revokeByStudentNum(String studentNum, ProfileDecoration profileDecoration) {
        User user = findUserByStudentNum(studentNum);
        if (profileDecoration == null) {
            throw new NotFoundException(ProfileDecorationErrorCode.BADGE_NOT_FOUND);
        }
        repository.deleteByUserIdAndProfileDecorationId(user.getId(), profileDecoration.getId());
        if (profileDecoration.getKey().equals(user.getProfileDecoration())) {
            user.changeProfileDecoration(null);
        }
    }

    @Transactional(readOnly = true)
    public List<ProfileDecoration> getOwnedDecorations(Long userId) {
        return repository.findDecorationsByUserId(userId);
    }

    private User findUserByStudentNum(String studentNum) {
        if (studentNum == null || studentNum.isBlank()) {
            throw new NotAllowFormException(ProfileDecorationErrorCode.STUDENT_NUM_REQUIRED);
        }
        return userRepository.findByStudentNum(studentNum.trim())
                .orElseThrow(() -> new NotFoundException(ProfileDecorationErrorCode.STUDENT_NUM_NOT_FOUND));
    }
}
