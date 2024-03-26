package at.aau.serg.websocketdemoserver.service;

public interface GameLobbyEntityService {

    void createLobby(String name);
    void getListOfLobbies();
    void deleteLobby(Long id);
}
