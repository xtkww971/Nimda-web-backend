package judgeServer.domain.schedule.enums;

/** 대회 진행 상태. */
public enum ContestPhase {
    /** 일정이 등록되지 않았다. 상시 워게임처럼 제출을 막지 않는다. */
    NOT_SCHEDULED,
    /** 시작 전 */
    UPCOMING,
    /** 진행 중 (시작 시각 이상, 종료 시각 미만) */
    RUNNING,
    /** 종료. 플래그 제출이 마감된다. */
    ENDED;

    /** 이 상태에서 낸 정답을 점수로 인정하는지. */
    public boolean acceptsSubmissions() {
        return this == NOT_SCHEDULED || this == RUNNING;
    }
}
