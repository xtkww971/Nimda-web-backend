package judgeServer.domain.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대회 일정·종료 후 동작 저장. 시각은 KST 기준 ISO 로컬 형식이다 (예: 2026-03-21T10:00:00).
 * "지금 종료"는 endsAt을 현재 시각으로 보내면 된다. 설정 값을 비워두면 기존 값(처음이면 기본값)을 쓴다.
 */
@Getter
@NoArgsConstructor
public class ScheduleUpdateRequest {

    @NotNull(message = "시작 시각을 입력해주세요.")
    private LocalDateTime startsAt;

    @NotNull(message = "종료 시각을 입력해주세요.")
    private LocalDateTime endsAt;

    private Boolean cleanupInstancesOnEnd;

    private Boolean challengesVisibleAfterEnd;

    private Boolean freezeScoreboard;

    @Min(value = 1, message = "고정 시점은 1분 이상이어야 합니다.")
    @Max(value = 1440, message = "고정 시점은 24시간 이내여야 합니다.")
    private Integer freezeMinutesBeforeEnd;
}
