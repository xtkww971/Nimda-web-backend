package com.nimda.cite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing
@EnableAsync // 알림 비동기 방식으로 처리
@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
@ComponentScan(basePackages = {"com.nimda.cite", "judgeServer", "redis"})
@EnableJpaRepositories(basePackages = {"com.nimda.cite", "judgeServer"})
@EntityScan(basePackages = {"com.nimda.cite", "judgeServer"})
public class NimdaCiteApplication {
    public static void main(String[] args) {
        // JVM은 이름 조회 결과를 기본 30초 캐시한다. CTF 인스턴스는 컨테이너 이름으로 프록시하는데,
        // 재생성되면 IP가 바뀌고 옛 IP는 다른 사용자의 인스턴스가 받을 수 있다. 캐시가 남아 있으면
        // 그동안 요청이 남의 인스턴스로 간다. 컨테이너 이름은 캐싱하지 않는다.
        java.security.Security.setProperty("networkaddress.cache.ttl", "0");
        // 컨테이너 재생성 중에 한 번 조회가 실패하면 다시 떠도 10초간 502가 난다. 이름 없는 컨테이너는 캐싱하지 않는다.
        java.security.Security.setProperty("networkaddress.cache.negative.ttl", "0");
        SpringApplication.run(NimdaCiteApplication.class, args);
    }
}
