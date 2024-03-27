package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;

import java.util.Optional;

public interface GameLobbyEntityService {

    GameLobbyEntity createLobby(GameLobbyEntity gameLobbyEntity);
    void getListOfLobbies();
    void deleteLobby(Long id);

    Optional<GameLobbyEntity> findById(Long id);
}
