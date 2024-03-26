package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketdemoserver.domain.dto.PlayerDto;

public interface PlayerEntityService {

    void createPlayer(String name);
    void updatePlayer(String name);
    GameLobbyDto joinLobby(GameLobbyDto lobby, PlayerDto player);
    void leaveLobby(Long id);
    void deletePlayer(Long id);
}
