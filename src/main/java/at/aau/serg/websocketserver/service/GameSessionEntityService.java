package at.aau.serg.websocketserver.service;

import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import jakarta.persistence.EntityExistsException;

public interface GameSessionEntityService {
    GameSessionEntity createGameSession(Long gameLobbyId) throws EntityExistsException;
}
