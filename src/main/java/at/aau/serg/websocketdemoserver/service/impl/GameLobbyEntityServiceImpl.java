package at.aau.serg.websocketdemoserver.service.impl;

import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketdemoserver.service.GameLobbyEntityService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GameLobbyEntityServiceImpl implements GameLobbyEntityService {

    private GameLobbyEntityRepository gameLobbyEntityRepository;

    public GameLobbyEntityServiceImpl(GameLobbyEntityRepository gameLobbyEntityRepository) {
        this.gameLobbyEntityRepository = gameLobbyEntityRepository;
    }

    @Override
    public GameLobbyEntity createLobby(GameLobbyEntity gameLobbyEntity) {
        GameLobbyEntity createdGameLobbyEntity = gameLobbyEntityRepository.save(gameLobbyEntity);
        // set number of players to 0 on creation for now
        // TODO: the gameLobby is automatically joined by the player who created it
        createdGameLobbyEntity.setNumPlayers(0);
        return createdGameLobbyEntity;
    }

    @Override
    public void getListOfLobbies() {

    }

    @Override
    public void deleteLobby(Long id) {

    }

    @Override
    public Optional<GameLobbyEntity> findById(Long id) {
        return gameLobbyEntityRepository.findById(id);
    }
}
