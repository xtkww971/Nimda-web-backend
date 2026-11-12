package judgeServer.domain.contest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 종료된 대회의 최종 결과를 자동으로 확정한다. 1분마다 확인하고, 종료 후 유예 시간이 지났으면 스냅샷을 만든다.
 * 트랜잭션이 프록시를 거치도록 서비스와 분리해 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContestFinalizeScheduler {

    private final ContestService contestService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void finalizeEndedContest() {
        try {
            contestService.finalizeIfDue();
        } catch (Exception e) {
            log.error("대회 결과 자동 확정 실패: {}", e.getMessage(), e);
        }
    }
}
