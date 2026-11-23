package com.nimda.ctfgame.repository;

import com.nimda.ctfgame.entity.GameSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSettingsRepository extends JpaRepository<GameSettings, Byte> {
}
