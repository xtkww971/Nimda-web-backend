package judgeServer.domain.challenge.entity;

import com.nimda.cite.common.util.TableNames;
import jakarta.persistence.*;
import judgeServer.domain.challenge.enums.ChallengeCategory;
import judgeServer.domain.challenge.enums.FlagType;
import judgeServer.domain.challenge.enums.IsolationType;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = TableNames.challengesTable)
@Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 문제 고유 식별자. 문제 파일/이미지 이름과 맞춰서 쓴다 (예: rev-01) */
    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengeCategory category;

    /** 첫 번째 해결자가 받는 점수 */
    @Builder.Default
    @Column(name = "max_point", nullable = false)
    private Integer maxPoint = 100;

    /** 해결자가 늘어도 이 아래로는 내려가지 않는다 */
    @Builder.Default
    @Column(name = "min_point", nullable = false)
    private Integer minPoint = 0;

    /** 해결자 한 명당 깎이는 점수 */
    @Builder.Default
    @Column(name = "decrease_point", nullable = false)
    private Integer decreasePoint = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "flag_type", nullable = false, length = 20)
    private FlagType flagType = FlagType.STATIC;

    
    /**
     정답 유출을 막기 위해 플래그를 저장하고 비교할 때는 해싱 함수를 적용해야 함
     */
    @Column(name = "flag_hash", length = 64)
    private String flagHash;

    /**
     * S3 객체 위치로 CTF 서버에서 download stream을 받아서 사용
     */
    @Column(name = "attachment_key")
    private String attachmentKey;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "isolation_type", nullable = false, length = 20)
    private IsolationType isolationType = IsolationType.NONE;

    /** 초안 작성 중에는 false로 두고 공개할 때 true로 바꾼다 */
    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 이미 {@code solveCount}명이 푼 상태에서 다음 해결자가 받을 점수. */
    public int currentPoints(long solveCount) {
        long decreased = maxPoint - decreasePoint * solveCount;
        return (int) Math.max(minPoint, decreased);
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
