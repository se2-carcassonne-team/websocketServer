package at.aau.serg.websocketserver.service;

import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Optional;

public interface PlayerEntityService {

    PlayerEntity createPlayer(PlayerEntity playerEntity) throws EntityExistsException;
    PlayerEntity createPlayer(PlayerEntity playerEntity, String sessionId) throws EntityExistsException;
    PlayerEntity updateUsername(PlayerEntity playerEntity) throws EntityNotFoundException;
    PlayerEntity joinLobby(Long gameLobbyId, PlayerEntity playerEntity) throws RuntimeException;
    List<PlayerEntity> getAllPlayersForLobby(Long gameLobbyId);
    PlayerEntity leaveLobby(PlayerEntity playerEntity) throws EntityNotFoundException;
    PlayerEntity leaveGameSession(PlayerEntity playerEntity) throws EntityNotFoundException;
    void deletePlayer(Long id);
    boolean exists(Long id);

    Optional<PlayerEntity> findPlayerById(Long id);
    Optional<PlayerEntity> findPlayerBySessionId(String sessionId);
    List<PlayerEntity> findAllPlayers(List<Long> ids);
}
