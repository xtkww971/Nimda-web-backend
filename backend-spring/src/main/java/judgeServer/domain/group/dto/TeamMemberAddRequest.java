package judgeServer.domain.group.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** 미배정 참가자 여러 명을 한 번에 팀에 배정한다. 모두 MEMBER로 들어가고, 팀장은 PATCH .../leader로 정한다. */
@Getter
@NoArgsConstructor
public class TeamMemberAddRequest {

    @NotEmpty(message = "배정할 참가자를 선택해주세요.")
    @Size(max = 15)
    private List<@NotNull Long> userIds;
}
