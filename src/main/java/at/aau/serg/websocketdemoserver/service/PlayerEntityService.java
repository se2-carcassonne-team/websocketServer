package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;

import java.util.Optional;

public interface PlayerEntityService {

    PlayerEntity createPlayer(PlayerEntity playerEntity);
    PlayerEntity updateUsername(Long id, PlayerEntity playerEntity);
    PlayerEntity joinLobby(GameLobbyEntity gameLobbyEntity, PlayerEntity playerEntity);
    PlayerEntity leaveLobby(GameLobbyEntity gameLobbyEntity, PlayerEntity playerEntity);
    void deletePlayer(Long id);
    boolean exists(Long id);

    Optional<PlayerEntity> findPlayerById(Long id);
}
