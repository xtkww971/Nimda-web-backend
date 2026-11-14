package judgeServer.domain.instance.controller;

import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import judgeServer.domain.instance.service.CtfServerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * GET /api/ctf/server-status → { "available": true|false }
 *
 * <p>문제 페이지가 [인스턴스 시작] 버튼을 보여줄지 정할 때 쓴다. /api/ctf/instance 아래에 두지 않은 이유는
 * GET /api/ctf/instance/{code}와 경로가 겹쳐서, "server-status"라는 코드의 문제를 가릴 수 있기 때문이다.
 * 로그인 여부는 SecurityConfig의 기본 규칙(authenticated)을 따른다.
 */
@RestController
@RequestMapping(UrlNames.ctfServerStatusUrl)
@RequiredArgsConstructor
public class CtfServerStatusController {

    private final CtfServerStatus serverStatus;

    @GetMapping(UrlNames.getCtfServerStatusUrl)
    public ResponseEntity<?> status() {
        return ApiResponse.ok(Map.of("available", serverStatus.isAvailable())).toResponse();
    }
}
