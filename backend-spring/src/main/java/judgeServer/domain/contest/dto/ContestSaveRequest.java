package judgeServer.domain.contest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대회 생성·일정 저장. 시각은 KST 기준 ISO 로컬 형식이다 (예: 2026-03-21T10:00:00).
 * "지금 종료"는 endsAt을 현재 시각으로 보내면 된다. 설정 값을 비워두면 기존 값(처음이면 기본값)을 쓴다.
 * name을 비워두면 기존 이름(처음이면 "NIMDACON {시작 연도}")을 쓴다.
 */
@Getter
@NoArgsConstructor
public class ContestSaveRequest {

    @Size(max = 100, message = "대회 이름은 100자 이내로 입력해주세요.")
    private String name;

    @NotNull(message = "시작 시각을 입력해주세요.")
    private LocalDateTime startsAt;

    @NotNull(message = "종료 시각을 입력해주세요.")
    private LocalDateTime endsAt;

    private Boolean cleanupInstancesOnEnd;

    private Boolean challengesVisibleAfterEnd;

    /** 종료 전 순위 비공개 켜기 (화면 이름 "순위 비공개", 필드 이름은 예전 "스코어보드 고정" 그대로) */
    private Boolean freezeScoreboard;

    /** 순위 비공개 시작: 종료 몇 분 전 (1~600, 기본 120) */
    @Min(value = 1, message = "순위 비공개는 종료 1분 전부터 정할 수 있습니다.")
    @Max(value = 600, message = "순위 비공개는 종료 600분 전까지 정할 수 있습니다.")
    private Integer freezeMinutesBeforeEnd;
}
