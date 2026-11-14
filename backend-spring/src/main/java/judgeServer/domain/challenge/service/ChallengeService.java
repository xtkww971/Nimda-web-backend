package judgeServer.domain.challenge.service;

import com.nimda.cite.common.exception.AlreadyExistException;
import com.nimda.cite.common.exception.NotAllowFormException;
import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.common.exception.errorcode.ctf.ChallengeErrorCode;
import com.nimda.cite.domain.attachment.store.S3FileStore;
import judgeServer.domain.challenge.dto.AddChallengeRequest;
import judgeServer.domain.challenge.entity.Challenge;
import judgeServer.domain.challenge.enums.FlagType;
import judgeServer.domain.challenge.enums.IsolationType;
import judgeServer.domain.challenge.repository.ChallengeRepository;
import judgeServer.domain.challenge.util.FlagHasher;
import judgeServer.domain.scoreboard.repository.CtfTeamScoreRepository;
import judgeServer.domain.solve.repository.SolveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private static final int DEFAULT_MAX_POINT = 100;
    private static final int DEFAULT_MIN_POINT = 0;
    private static final int DEFAULT_DECREASE_POINT = 10;

    private final ChallengeRepository challengeRepository;
    private final SolveRepository solveRepository;
    private final CtfTeamScoreRepository teamScoreRepository;
    private final S3FileStore fileStore;

    /**
     * 문제를 등록한다.
     *
     * 압축파일은 풀지 않고 challenges/{code}/{code}.zip 으로 그대로 올린다.
     * 컨테이너를 만드는 쪽은 CTF 서버라, 여기서 풀어봐야 파일 수만큼 S3 요청만 늘어난다.
     */
    @Transactional
    public Challenge addChallenge(AddChallengeRequest request) {
        if (challengeRepository.existsByCode(request.getCode())) {
            throw new AlreadyExistException(ChallengeErrorCode.CHALLENGE_CODE_DUPLICATED,
                    ChallengeErrorCode.CHALLENGE_CODE_DUPLICATED.getMessage() + " (" + request.getCode() + ")");
        }

        FlagType flagType = request.getFlagType() != null ? request.getFlagType() : FlagType.STATIC;
        IsolationType isolationType = request.getIsolationType() != null
                ? request.getIsolationType() : IsolationType.NONE;
        // Judge가 받지 않는 조합은 저장하지 않는다. 저장되면 풀 수 없는 문제가 된다.
        ChallengeRules.validate(request.getCategory(), isolationType, flagType);

        int maxPoint = request.getMaxPoint() != null ? request.getMaxPoint() : DEFAULT_MAX_POINT;
        int minPoint = request.getMinPoint() != null ? request.getMinPoint() : DEFAULT_MIN_POINT;
        int decreasePoint = request.getDecreasePoint() != null ? request.getDecreasePoint() : DEFAULT_DECREASE_POINT;
        if (minPoint > maxPoint) {
            throw new NotAllowFormException(ChallengeErrorCode.INVALID_POINT_RANGE);
        }

        // DYNAMIC은 사용자마다 플래그가 달라 미리 저장할 값이 없다.
        String flagHash = null;
        if (flagType == FlagType.STATIC) {
            if (request.getFlag() == null || request.getFlag().isBlank()) {
                throw new NotAllowFormException(ChallengeErrorCode.STATIC_FLAG_REQUIRED);
            }
            flagHash = FlagHasher.hash(request.getFlag());
        }

        Challenge challenge = Challenge.builder()
                .code(request.getCode())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .maxPoint(maxPoint)
                .minPoint(minPoint)
                .decreasePoint(decreasePoint)
                .flagType(flagType)
                .flagHash(flagHash)
                .isolationType(isolationType)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .build();

        boolean hasArchive = request.getZipFile() != null && !request.getZipFile().isEmpty();
        if (hasArchive) {
            challenge.setAttachmentKey(
                    fileStore.uploadChallengeArchive(request.getCode(), request.getZipFile()));
        }

        try {
            return challengeRepository.save(challenge);
        } catch (RuntimeException e) {
            // DB 저장이 실패하면 트랜잭션은 되돌아가지만 S3에 올린 파일은 남는다.
            // 다음 등록 때 같은 코드가 막히지 않도록 직접 지운다.
            if (hasArchive) {
                fileStore.deleteChallengeDirectory(request.getCode());
            }
            throw e;
        }
    }

    /**
     * 문제와 그 정답 기록을 지우고, 이 문제를 푼 팀들의 점수를 다시 계산한다.
     *
     * <p>{@link judgeServer.domain.solve.service.SolveRecorder}와 같은 문제 행을 잠가서, 삭제 도중 같은 문제의
     * 정답이 기록돼 점수판에만 점수가 남는 일을 막는다. 잠금 조회가 트랜잭션의 첫 조회여야 한다.
     */
    @Transactional
    public void deleteChallenge(Long id) {
        Challenge challenge = challengeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException(ChallengeErrorCode.CHALLENGE_NOT_FOUND));

        List<Long> solverTeamIds = solveRepository.findTeamIdsByChallengeId(id);
        solveRepository.deleteByChallengeId(id);
        if (!solverTeamIds.isEmpty()) {
            teamScoreRepository.recalculate(solverTeamIds);
        }
        challengeRepository.delete(challenge);
        fileStore.deleteChallengeDirectory(challenge.getCode());
    }

    @Transactional
    public boolean toggleIsPublic(Long id) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ChallengeErrorCode.CHALLENGE_NOT_FOUND));

        challenge.setIsPublic(!challenge.getIsPublic());
        return challenge.getIsPublic();
    }
}
