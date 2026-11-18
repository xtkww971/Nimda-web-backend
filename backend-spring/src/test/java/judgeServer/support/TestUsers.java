package judgeServer.support;

import com.nimda.cite.user.entity.Authority;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.security.CustomUserDetails;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/** 역할을 가진 로그인 사용자 (Java 24 에서 Mockito 가 엔티티를 mock 하지 못해 직접 만든다). */
public final class TestUsers {

    private TestUsers() {
    }

    public static CustomUserDetails user(long id, String... roles) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", id);
        for (String role : roles) {
            user.addAuthority(Authority.builder().authorityName(role).build());
        }
        return new CustomUserDetails(user);
    }
}
