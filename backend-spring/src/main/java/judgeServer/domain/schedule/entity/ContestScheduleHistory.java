package judgeServer.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 대회 일정 변경 이력 (운영 설정 화면의 CHANGE LOG). field 값은 V37 마이그레이션 주석 참고. */
@Entity
@Table(name = "ctf_schedule_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ContestScheduleHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String field;

    @Column(name = "old_value", length = 100)
    private String oldValue;

    @Column(name = "new_value", length = 100)
    private String newValue;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
