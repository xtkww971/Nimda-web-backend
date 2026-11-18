package com.nimda.ctfgame.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CTF 게임 맵에 놓는 물건. 상호작용하면 연결 문제(challengeId) 팝업이 뜨고, 팀이 그 문제를 풀면 "가진" 것으로 보고 이야기를 보여 준다.
 * 이벤트용 기능이라 이 패키지(com.nimda.ctfgame)에 모아 두었다. 지우는 법은 package-info.java 참고.
 */
@Entity
@Table(name = "ctf_game_items")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_key", nullable = false, length = 50, unique = true)
    private String itemKey;

    @Column(nullable = false, length = 100)
    private String name;

    /** 이야기. 가진 팀에게만 내려준다 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 상호작용하면 여는 문제. 문제가 지워지면 null (DB ON DELETE SET NULL) */
    @Column(name = "challenge_id")
    private Long challengeId;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** false 면 참가자에게 보이지 않는다 (배치 준비 중) */
    @Builder.Default
    @Column(nullable = false)
    private Boolean visible = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** 문제 연결만 바꾼다 (연결 API). null 이면 연결 해제 */
    public void linkTo(Long challengeId) {
        this.challengeId = challengeId;
    }

    public void update(String name, String description, Long challengeId, int sortOrder, boolean visible) {
        this.name = name;
        this.description = description;
        this.challengeId = challengeId;
        this.sortOrder = sortOrder;
        this.visible = visible;
    }
}
