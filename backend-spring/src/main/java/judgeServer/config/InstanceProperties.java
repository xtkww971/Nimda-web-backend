package judgeServer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 인스턴스 서브도메인 접근 설정 (application.yml의 ctf.instance.*).
 *
 * 인스턴스는 inst-{token}.{baseDomain} 형태의 호스트로 접근한다. baseDomain이 비어 있으면
 * 서브도메인 프록시 기능이 꺼진 것으로 보고 아무 요청도 가로채지 않는다(기본값).
 *
 * 예)
 *   운영:  base-domain: nimda.kr           -> inst-abc123.nimda.kr
 *   개발:  base-domain: 127.0.0.1.nip.io   -> inst-abc123.127.0.0.1.nip.io (실제 DNS 불필요)
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ctf.instance")
public class InstanceProperties {

    /** inst-{token} 뒤에 붙는 도메인. 비어 있으면 서브도메인 프록시 비활성. */
    private String baseDomain = "";

    /** 서브도메인 접두사. */
    private String subdomainPrefix = "inst-";

    /**
     * CTF 서버(EC2)의 인스턴스 게이트웨이 주소 "host:port" (예: 172.31.10.20:8088). VPC peering으로 닿는 사설 IP다.
     *
     * <p>web 인스턴스는 CTF 서버의 ctf-instances 망 안에서만 이름(alias)으로 풀린다. 웹 backend는 다른 호스트라
     * 그 이름에 직접 못 붙으므로, 게이트웨이로 보내고 대상(alias:포트)은 {@code X-Ctf-Target} 헤더로 넘긴다.
     * 비어 있으면 예전처럼 결과의 host:port로 직접 붙는다(같은 호스트·로컬 구성).
     * 근거: docs/issue/instance-split-host-access-issue.md 설계 A.
     */
    private String gateway = "";

    // 매핑 보관 시간(mappingTtl)은 CtfTtlProperties(ctf.ttl.mapping)로 옮겼다.
}
