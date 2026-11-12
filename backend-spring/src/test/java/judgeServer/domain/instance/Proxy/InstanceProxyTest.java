package judgeServer.domain.instance.Proxy;

import com.sun.net.httpserver.HttpServer;
import judgeServer.config.InstanceProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게이트웨이 경유(호스트 분리) / 직접 연결(같은 호스트) 두 모드를 실제 HTTP로 확인한다.
 * 가짜 게이트웨이·인스턴스는 JDK HttpServer로 띄운다.
 */
class InstanceProxyTest {

    private HttpServer server;
    private int serverPort;
    private final AtomicReference<String> seenTarget = new AtomicReference<>();
    private final AtomicReference<List<String>> seenTargetAll = new AtomicReference<>();
    private final AtomicReference<String> seenUri = new AtomicReference<>();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            seenTarget.set(exchange.getRequestHeaders().getFirst(InstanceProxy.TARGET_HEADER));
            seenTargetAll.set(exchange.getRequestHeaders().get(InstanceProxy.TARGET_HEADER));
            seenUri.set(exchange.getRequestURI().toString());
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        serverPort = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private InstanceProxy proxy(String gateway) {
        InstanceProperties props = new InstanceProperties();
        props.setGateway(gateway);
        return new InstanceProxy(props);
    }

    @Test
    void 게이트웨이가_있으면_게이트웨이로_보내고_대상은_헤더로_넘긴다() {
        HttpHeaders in = new HttpHeaders();
        // 참가자가 대상 헤더를 위조해 보내도 backend가 정한 값만 남아야 한다.
        in.add(InstanceProxy.TARGET_HEADER, "nimda-redis:6379");
        in.add("x-ctf-target", "nimda-mysql:3306");

        ResponseEntity<byte[]> resp = proxy("127.0.0.1:" + serverPort)
                .forward("s-web-basic-01", 5000, "login", "q=1'%20union%20select%201--", "GET", in, null);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(seenTargetAll.get()).containsExactly("s-web-basic-01:5000");
        assertThat(seenUri.get()).isEqualTo("/login?q=1'%20union%20select%201--");
    }

    @Test
    void 게이트웨이가_비어_있으면_결과의_host_port로_직접_붙고_대상_헤더는_보내지_않는다() {
        HttpHeaders in = new HttpHeaders();
        in.add(InstanceProxy.TARGET_HEADER, "nimda-redis:6379");

        ResponseEntity<byte[]> resp = proxy(" ")
                .forward("127.0.0.1", serverPort, "", null, "GET", in, null);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(seenTarget.get()).isNull();
        assertThat(seenUri.get()).isEqualTo("/");
    }

    @Test
    void 게이트웨이에_못_붙으면_502() {
        int closedPort = serverPort;
        server.stop(0);

        ResponseEntity<byte[]> resp = proxy("127.0.0.1:" + closedPort)
                .forward("s-web-basic-01", 5000, "", null, "GET", new HttpHeaders(), null);

        assertThat(resp.getStatusCode().value()).isEqualTo(502);
        // 본문에 내부 주소가 새지 않는다.
        assertThat(new String(resp.getBody(), StandardCharsets.UTF_8)).doesNotContain("s-web-basic-01", "127.0.0.1");
    }
}
