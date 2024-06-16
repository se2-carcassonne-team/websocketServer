package at.aau.serg.websocketserver.service;

import at.aau.serg.websocketserver.domain.dto.FinishedTurnDto;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;

import java.util.List;

public interface CheatService {
    void assignCheatFunctionality(List<PlayerEntity> playerEntityList);
    void updatePlayerPoints(Long playerId, FinishedTurnDto finishedTurnDto, int cheatPoints);
    int generateCheatPoints();
}
