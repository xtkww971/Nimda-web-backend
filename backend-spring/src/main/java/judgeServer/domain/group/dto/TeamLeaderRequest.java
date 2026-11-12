package judgeServer.domain.group.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeamLeaderRequest {

    @NotNull(message = "팀장으로 지정할 유저를 선택해주세요.")
    private Long userId;
}
