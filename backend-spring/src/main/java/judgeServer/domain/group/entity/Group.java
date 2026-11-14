package judgeServer.domain.group.entity;

import com.nimda.cite.common.entity.BaseTimeEntity;
import com.nimda.cite.common.util.TableNames;
import jakarta.persistence.*;
import judgeServer.domain.group.enums.GroupType;
import judgeServer.domain.groupMember.entity.GroupMember;
import lombok.*;


import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = TableNames.groupsTable)
/*
, uniqueConstraints = {
        @UniqueConstraint(name = "uk_group_activity_name", columnNames = {"activity_id", "name"})
})
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*

    대회나 스터디

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

     */

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(length = 200)
    private String description;

    /** 스터디 그룹인지 CTF 팀인지. 두 API가 서로의 행을 건드리지 않게 가른다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false, length = 20)
    private GroupType type;

    /** 이 팀이 참가하는 대회(ctf_contests.id). 팀 이름 중복·한 사람 한 팀은 대회 안에서만 따진다. */
    @Column(name = "contest_id")
    private Long contestId;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> members = new ArrayList<>();

    // 임시
    @Builder
    private Group(String name, Integer capacity, String description, Long contestId, GroupType type) {
        this.name = name;
        this.capacity = capacity;
        this.description = description;
        this.contestId = contestId;
        // 기존 행의 기본값(V39)과 맞춘다. 스터디 그룹은 GroupService가 STUDY를 넘긴다.
        this.type = type != null ? type : GroupType.CTF_TEAM;
    }

    public boolean isStudy() {
        return type == GroupType.STUDY;
    }

    public boolean isCtfTeam() {
        return type == GroupType.CTF_TEAM;
    }

    /*
    @Builder
    private Group(Activity activity, String name, Integer capacity, String description) {
        this.activity = activity;
        this.name = name;
        this.capacity = capacity;
        this.description = description;
    }
    */

    public void update(String name, Integer capacity, String description) {
        this.name = name;
        this.capacity = capacity;
        this.description = description;
    }

    /** 멤버 목록에 추가한다. cascade로 함께 저장되고, 같은 트랜잭션의 응답에도 바로 반영된다. */
    public void addMember(GroupMember member) {
        this.members.add(member);
    }

    /** 멤버 목록에서 뺀다. orphanRemoval로 group_members 행도 지워진다. */
    public boolean removeMemberByUserId(Long userId) {
        return this.members.removeIf(m -> m.getUser().getId().equals(userId));
    }

}
