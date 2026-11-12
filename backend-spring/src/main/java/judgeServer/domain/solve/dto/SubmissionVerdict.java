package judgeServer.domain.solve.dto;

/**
 * 플래그 채점 결과.
 *
 * @param status        채점 결과
 * @param awardedPoints 이번 제출로 얻은 점수. 초안 문제는 기록하지 않으므로 0이다.
 */
public record SubmissionVerdict(Status status, int awardedPoints) {

    public enum Status {
        CORRECT, WRONG, ALREADY_SOLVED, NOT_FOUND, UNSUPPORTED, ERROR,
        /** 팀에 소속되지 않은 참가자의 제출. 팀전이라 받지 않는다. */
        NO_TEAM
    }

    public static SubmissionVerdict of(Status status) {
        return new SubmissionVerdict(status, 0);
    }
}
