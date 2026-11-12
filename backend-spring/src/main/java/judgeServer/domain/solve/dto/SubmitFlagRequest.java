package judgeServer.domain.solve.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitFlagRequest(
        @NotBlank(message = "플래그를 입력해 주세요.")
        @Size(max = 512, message = "플래그가 너무 깁니다.")
        String flag
) {
}
