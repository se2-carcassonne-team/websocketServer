package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;

import java.util.List;
import java.util.Optional;

public interface GameLobbyEntityService {

    GameLobbyEntity createLobby(GameLobbyEntity gameLobbyEntity);
    GameLobbyEntity updateLobbyName(GameLobbyEntity gameLobbyEntity);
    List<GameLobbyEntity> getListOfLobbies();
    void deleteLobby(Long id);
    Optional<GameLobbyEntity> findById(Long id);
}
