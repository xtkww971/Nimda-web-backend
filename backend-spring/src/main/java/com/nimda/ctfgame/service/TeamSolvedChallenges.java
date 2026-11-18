package com.nimda.ctfgame.service;

import com.nimda.ctfgame.repository.GameItemRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import judgeServer.domain.contest.service.ContestService;
import judgeServer.domain.group.service.CtfTeamResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 팀 공유 규칙: 현재 대회에서 팀원 중 누구라도 푼 문제는 팀 전체가 푼 것으로 본다 (기획안: 팀 공유, 소비하지 않음).
 * 팀이 없으면(관리자·DEV 시험 등) 본인이 푼 것만 본다. 기존 CTF 코드(judgeServer)는 읽기만 한다.
 */
@Component
@RequiredArgsConstructor
public class TeamSolvedChallenges implements SolvedChallenges {

    private final GameItemRepository repository;
    private final CtfTeamResolver teamResolver;
    private final ContestService contestService;

    @Override
    public Set<Long> of(Long userId) {
        Long contestId = contestService.currentContestId().orElse(null);
        Optional<Long> teamId = teamResolver.findTeamId(userId);
        return new HashSet<>(teamId.isPresent()
                ? repository.findSolvedChallengeIdsByTeam(teamId.get(), contestId)
                : repository.findSolvedChallengeIdsByUser(userId, contestId));
    }
}
