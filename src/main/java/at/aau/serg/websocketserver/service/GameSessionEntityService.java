package at.aau.serg.websocketserver.service;

import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

import java.util.Optional;

public interface GameSessionEntityService {
    GameSessionEntity createGameSession(Long gameLobbyId) throws EntityExistsException;
    Optional<GameSessionEntity> findById(Long id) throws EntityNotFoundException;
    GameSessionEntity terminateGameSession(Long gameSessionId);
}
