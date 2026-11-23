package com.nimda.ctfgame.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CTF 게임 설정 (한 줄, id = 1). 지금은 Stage2 시간 개방 하나.
 * stage2OpenMinutes: 대회 시작 후 이 분이 지나면 모든 팀에게 게임의 Stage2 문을 연다. null 이면 시간 개방 없음.
 * 이벤트용이라 com.nimda.ctfgame 에 둔다 (지우는 법은 package-info.java).
 */
@Entity
@Table(name = "ctf_game_settings")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameSettings {

    /** 설정은 한 줄뿐이다 */
    public static final byte ID = 1;

    @Id
    private Byte id;

    @Column(name = "stage2_open_minutes")
    private Integer stage2OpenMinutes;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    /** 아직 한 번도 저장하지 않았을 때의 기본값 (시간 개방 없음) */
    public static GameSettings defaults(LocalDateTime now) {
        return GameSettings.builder().id(ID).stage2OpenMinutes(null).updatedAt(now).build();
    }

    public void changeStage2OpenMinutes(Integer minutes, LocalDateTime at, Long by) {
        this.stage2OpenMinutes = minutes;
        this.updatedAt = at;
        this.updatedBy = by;
    }
}
