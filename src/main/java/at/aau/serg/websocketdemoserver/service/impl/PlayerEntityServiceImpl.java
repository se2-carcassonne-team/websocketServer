package at.aau.serg.websocketdemoserver.service.impl;

import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketdemoserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketdemoserver.domain.entity.repository.PlayerEntityRepository;
import at.aau.serg.websocketdemoserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketdemoserver.mapper.PlayerMapper;
import at.aau.serg.websocketdemoserver.service.PlayerEntityService;
import org.springframework.stereotype.Service;

@Service
public class PlayerEntityServiceImpl implements PlayerEntityService {

    PlayerEntityRepository playerEntityRepository;
    GameLobbyEntityRepository gameLobbyEntityRepository;
    GameLobbyMapper gameLobbyMapper;
    PlayerMapper playerMapper;

    public PlayerEntityServiceImpl(
            PlayerEntityRepository playerEntityRepository,
            GameLobbyEntityRepository gameLobbyEntityRepository,
            GameLobbyMapper gameLobbyMapper,
            PlayerMapper playerMapper)
    {
        this.playerEntityRepository = playerEntityRepository;
        this.gameLobbyEntityRepository = gameLobbyEntityRepository;
        this.gameLobbyMapper = gameLobbyMapper;
        this.playerMapper = playerMapper;
    }

    // slightly different approach: expose only entity objects to the service
    @Override
    public PlayerEntity createPlayer(PlayerEntity playerEntity) {
        return playerEntityRepository.save(playerEntity);
    }

    @Override
    public void updatePlayer(String name) {

    }


    /**
     * updates the PlayerEntity in the Database to reference gameLobbyEntity and
     * updates the GameLobbyEntity in the Database by incrementing numPlayers by 1
     * @param gameLobbyEntity  the gameLobbyEntity to join
     * @param playerEntity  the PlayerEntity who wants to join the gameLobbyEntity
     * @return updated PlayerEntity from the database
     */
    @Override
    public PlayerEntity joinLobby(GameLobbyEntity gameLobbyEntity, PlayerEntity playerEntity) {

        // TODO: only allow lobby join if lobby is not full yet

        // update the PlayerEntity's gameLobby property to reference the gameLobbyEntity which should be joined
        playerEntity.setGameLobbyEntity(gameLobbyEntity);
        // update the numPlayers property of the gameLobbyEntity by 1
        gameLobbyEntity.setNumPlayers(gameLobbyEntity.getNumPlayers()+1);
        gameLobbyEntityRepository.save(gameLobbyEntity);

        return playerEntityRepository.save(playerEntity);
    }


    @Override
    public void leaveLobby(Long id) {

    }

    @Override
    public void deletePlayer(Long id) {

    }

    @Override
    public boolean exists(Long id) {
        return playerEntityRepository.existsById(id);
    }
}
