package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketdemoserver.domain.dto.PlayerDto;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;

public interface PlayerEntityService {

    PlayerEntity createPlayer(PlayerEntity playerEntity);
    void updatePlayer(String name);
    GameLobbyDto joinLobby(GameLobbyDto lobby, PlayerDto player);
    void leaveLobby(Long id);
    void deletePlayer(Long id);
}
