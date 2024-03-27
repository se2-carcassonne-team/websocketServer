package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;

public interface PlayerEntityService {

    PlayerEntity createPlayer(PlayerEntity playerEntity);
    PlayerEntity updateUsername(Long id, PlayerEntity playerEntity);
    PlayerEntity joinLobby(GameLobbyEntity gameLobbyEntity, PlayerEntity playerEntity);
    void leaveLobby(Long id);
    void deletePlayer(Long id);
    boolean exists(Long id);
}
