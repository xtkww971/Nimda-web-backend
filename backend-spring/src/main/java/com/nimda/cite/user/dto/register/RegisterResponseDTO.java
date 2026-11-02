package com.nimda.cite.user.dto.register;

import com.nimda.cite.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponseDTO {
    private Long id;
    private String userId;
    private String name;
    private String nickname;
    private String studentNum;
    private String email;
    private String major;
    private String birth;
    private String bojId;

    // builder().build()의 결과를 버리면 필드가 채워지지 않으므로, 생성자에서는 직접 대입한다.
    public RegisterResponseDTO(User user){
        this.id = user.getId();
        this.userId = user.getUserId();
        this.name = user.getName();
        this.nickname = user.getNickname();
        this.studentNum = user.getStudentNum();
        this.email = user.getEmail();
        this.major = user.getMajor();
        this.birth = user.getBirth();
        this.bojId = user.getBojId();
    }
}
