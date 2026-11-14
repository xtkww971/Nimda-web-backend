package judgeServer.domain.instance.proxy;

import judgeServer.config.InstanceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// ctf 서버로 보내는 요청을 전달하는 Proxy
@Slf4j
@Component
@RequiredArgsConstructor
public class InstanceProxy {

    /**
     * 게이트웨이에 "어느 인스턴스로 넘길지"를 알리는 헤더. 값은 결과의 host:port(alias:EXPOSE 포트).
     * 참가자가 보낸 같은 이름의 헤더는 항상 지운다. 남기면 남의 인스턴스나 아무 내부 주소로 보낼 수 있다.
     */
    public static final String TARGET_HEADER = "X-Ctf-Target";

    private final InstanceProperties props;

    // 참가자에게 보이는 502 문구. 예외 메시지에는 내부 호스트명·IP가 담길 수 있어 본문에 넣지 않는다.
    private static final byte[] BAD_GATEWAY_BODY = "인스턴스에 연결할 수 없습니다.".getBytes(StandardCharsets.UTF_8);

    // 인스턴스 서버에 전달하지 않는 값
    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade",
            "host", "content-length", "expect");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER) // 리다이렉트는 브라우저가 처리하게 그대로 전달
            .build();

    
    public ResponseEntity<byte[]> forward(String host, int port, String path, String query,
                                          String method, HttpHeaders inHeaders, byte[] body) {
        
        // 쿼리가 있는지 없는지 확인
        String q = (query != null && !query.isEmpty()) ? "?" + query : "";
        // 게이트웨이가 있으면 게이트웨이로, 없으면 결과의 host:port로 직접 보낸다.
        String gateway = props.getGateway() == null ? "" : props.getGateway().trim();
        String target = host + ":" + port;
        String authority = gateway.isEmpty() ? target : gateway;
        URI uri;
        try {
            uri = URI.create("http://" + authority + "/" + path + q);
        } catch (IllegalArgumentException e) {
            // host에 URI에 못 쓰는 문자가 있거나 경로가 깨진 경우. IOException이 아니라 그냥 두면 500이 된다.
            log.warn("인스턴스 주소를 만들 수 없습니다: authority={}, reason={}", authority, e.getMessage());
            return ResponseEntity.status(502).body(BAD_GATEWAY_BODY);
        }
        // 해당 도메인으로 요청을 보내고 대기한다. 타임아웃 15초
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15));

        if (inHeaders != null) {
            inHeaders.forEach((name, values) -> {
                String lower = name.toLowerCase(Locale.ROOT);
                // 참가자가 보낸 대상 헤더는 버린다(위조 방지). 게이트웨이 모드에서는 아래에서 backend가 직접 넣는다.
                if (!HOP_BY_HOP.contains(lower) && !TARGET_HEADER.equalsIgnoreCase(lower)) {
                    values.forEach(v -> builder.header(name, v));
                }
            });
        }
        if (!gateway.isEmpty()) {
            builder.header(TARGET_HEADER, target);
        }

        // 요청에 전달할 값이 있는지 확인
        // 있다면 바이트 배열로 변환 후 전달
        if (body != null && body.length > 0) {
            builder.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
    
        // 메소드 전달
        try {
            HttpResponse<byte[]> resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());

            HttpHeaders outHeaders = new HttpHeaders();
            for (Map.Entry<String, List<String>> e : resp.headers().map().entrySet()) {
                if (HOP_BY_HOP.contains(e.getKey().toLowerCase(Locale.ROOT))) {
                    continue; // 컨테이너가 다시 계산/관리하는 헤더는 넘기지 않는다
                }
                for (String v : e.getValue()) {
                    outHeaders.add(e.getKey(), v); // Set-Cookie/Location 등 요청을 보낸 클라이언트에게 전달
                }
            }
            return new ResponseEntity<>(resp.body(), outHeaders, resp.statusCode());
        } catch (IOException e) {
            log.warn("인스턴스 연결 실패: target={}, via={}, reason={}", target, authority, e.toString());
            return ResponseEntity.status(502).body(BAD_GATEWAY_BODY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(502).body("프록시 중단됨".getBytes(StandardCharsets.UTF_8));
        }
    }
}
