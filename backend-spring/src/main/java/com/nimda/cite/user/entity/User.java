package com.nimda.cite.user.entity;

import com.nimda.cite.common.exception.errorcode.ErrorCode;
import com.nimda.cite.common.util.TableNames;
import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.config.CheckConfig;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimda.cite.common.entity.BaseTimeEntity;
import com.nimda.cite.user.enums.ApprovalStatus;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = TableNames.usersTable)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 20)
    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;
    // 로그인 아이디 (변경 불가능, 고유)

    @NotBlank
    @Size(min = 2, max = 10)
    @Column(name = "name", nullable = false, length = 10)
    private String name;
    // 실명

    @NotBlank
    @Size(min = 2, max = 20)
    @Column(name = "nickname", unique = true, nullable = false, length = 20)
    private String nickname;
    // 닉네임 (변경 가능, 표시용)

    @NotBlank
    @Size(min = 4, max = 255)
    @Column(nullable = false, length = 255)
    @JsonIgnore // 보안: 비밀번호는 JSON 응답에서 제외
    private String password;

    @NotBlank
    @Size(min = 9, max = 9)
    @Column(name = "student_num", nullable = false, length = 20)
    private String studentNum;
    // 학번 (9글자 고정)

    @NotBlank
    @Email
    @Column(unique = true, nullable = false, length = 40)
    private String email;

    @NotBlank
    @Size(max = 20)
    @Column(name = "major", nullable = false, length = 20)
    private String major;
    // 학과 (department → major로 변경)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;
    // 사용자 승인 상태 (PENDING: 승인 대기, APPROVED: 승인 완료, REJECTED: 승인 거부)

    @Column(name = "birth", length = 20)
    private String birth;
    // 생년월일

    @Column(name = "boj_id", length = 50)
    private String bojId;
    // 백준 ID (선택, Null 허용)

    @Column(name = "profile_image", length = 255)
    private String profileImage;
    // 프로필 이미지 URL

    @Column(name = "profile_decoration", length = 100)
    private String profileDecoration;
    // 장착 중인 프로필 장식 키

    // 마이페이지 이메일 숨기기
    @Column(name = "email_hide", nullable = false)
    private boolean emailHide = false; // 기본값 설정

    // ERD 기반 전체 필드 생성자
    @Builder
    public User(Long Id, String userId, String name, String nickname, String password,
            String studentNum, String email, String major, String birth, String bojId) {
        this.userId = userId;
        this.name = name;
        this.nickname = nickname;
        this.password = password;
        this.studentNum = studentNum;
        this.email = email;
        this.major = major;
        this.birth = birth;
        this.bojId = bojId;
    }

    // 사용자 권한 관리 테이블
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = TableNames.userAuthorityTable,
               joinColumns = @JoinColumn(name = "user_id"), // FK → users.id
               inverseJoinColumns = @JoinColumn(name = "authority_id") // FK → authority.id
    )
    @JsonIgnore
    private Set<Authority> authorities = new HashSet<>();

    public void changeStatus(ApprovalStatus targetStatus){
        this.status = targetStatus;
    }

    public boolean toggleEmailHide(){
        this.emailHide = !this.emailHide;
        return this.emailHide;
    }

    public void changeProfileImage(String profileImageKey){
        this.profileImage = profileImageKey;
    }

    public void changeProfileDecoration(String profileDecorationKey){
        this.profileDecoration = profileDecorationKey;
    }

    public void changePassword(String encodedPassword){
        CheckConfig.npeAndBlankCheck(encodedPassword, "encodedPassword");
        this.password = encodedPassword;
    }

    /**
     * 프로필 정보 부분 수정 (null/blank인 필드는 수정하지 않음, bojId는 blank면 삭제)
     * - 닉네임 중복 체크는 서비스에서 수행한다.
     */
    public void updateProfile(String nickname, String bojId, String birth, String major, String studentNum){
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (bojId != null) {
            this.bojId = bojId.isBlank() ? null : bojId;
        }
        if (birth != null && !birth.isBlank()) {
            this.birth = birth;
        }
        if (major != null && !major.isBlank()) {
            this.major = major;
        }
        if (studentNum != null && !studentNum.isBlank()) {
            this.studentNum = studentNum;
        }
    }

    public void addAuthority(Authority authority){
        CheckConfig.npeCheck(authority, "authority");

        if(authorities.stream()
                .anyMatch(a -> a.getAuthorityName().equals(authority.getAuthorityName()))){
            return;
        }

        this.authorities.add(authority);
    }

    public void removeAuthority(Authority authority){
        CheckConfig.npeCheck(authority, "authority");

        boolean removed =  this.authorities
                .removeIf(a -> a.getAuthorityName().equals(authority.getAuthorityName()));
        if(!removed){
            throw new NotFoundException(ErrorCode.AUTHORITY_NOT_EXIST);
        }
    }
}
