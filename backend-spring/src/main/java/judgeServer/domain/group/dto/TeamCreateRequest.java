package judgeServer.domain.group.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 운영자가 CTF 팀을 만든다. 운영자 본인은 멤버가 되지 않는다.
 *
 * <p>leaderUserId를 주면 그 유저가 LEADER가 된다. memberUserIds에 없으면 자동으로 포함한다.
 */
@Getter
@NoArgsConstructor
public class TeamCreateRequest {

    @NotBlank(message = "팀 이름을 입력해주세요.")
    @Size(max = 50)
    private String name;

    @NotNull(message = "최대 인원을 입력해주세요.")
    @Min(1) @Max(15)
    private Integer capacity;

    @Size(max = 200, message = "설명은 200자 이내로 입력해주세요.")
    private String description;

    /** 팀장 users.id. 비워두면 팀장 없이 만든다. */
    private Long leaderUserId;

    /** 처음부터 넣을 멤버 users.id 목록. 비워둘 수 있다. */
    @Size(max = 15)
    private List<@NotNull Long> memberUserIds = new ArrayList<>();
}
