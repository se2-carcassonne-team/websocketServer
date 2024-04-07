package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Optional;

public interface PlayerEntityService {

    PlayerEntity createPlayer(PlayerEntity playerEntity) throws EntityExistsException;
    PlayerEntity updateUsername(PlayerEntity playerEntity) throws EntityNotFoundException;
    PlayerEntity joinLobby(GameLobbyEntity gameLobbyEntity, PlayerEntity playerEntity) throws EntityNotFoundException, RuntimeException;
    List<PlayerEntity> getAllPlayersForLobby(GameLobbyEntity gameLobbyEntity);
    PlayerEntity leaveLobby(PlayerEntity playerEntity) throws EntityNotFoundException;
    void deletePlayer(Long id);
    boolean exists(Long id);

    Optional<PlayerEntity> findPlayerById(Long id);
}
