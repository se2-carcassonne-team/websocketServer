package at.aau.serg.websocketdemoserver.service.impl;

import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketdemoserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketdemoserver.domain.entity.repository.PlayerEntityRepository;
import at.aau.serg.websocketdemoserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketdemoserver.mapper.PlayerMapper;
import at.aau.serg.websocketdemoserver.service.PlayerEntityService;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    // @Dominik: slightly different approach: expose only entity objects to the service
    @Override
    public PlayerEntity createPlayer(PlayerEntity playerEntity) {
        return playerEntityRepository.save(playerEntity);
    }

    @Override
    public PlayerEntity updateUsername(Long id, PlayerEntity playerEntity) {
        playerEntity.setId(id);

        // retrieve player entity from database, then update only the username
        return playerEntityRepository.findById(id).map( playerEntityToUpdate -> {
            Optional.ofNullable(playerEntity.getUsername()).ifPresent(playerEntityToUpdate::setUsername);
            return playerEntityRepository.save(playerEntityToUpdate);
        }).orElseThrow(()-> new RuntimeException("Player does not exist"));

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
    public PlayerEntity leaveLobby(GameLobbyEntity gameLobbyEntity, PlayerEntity playerEntity) {
        playerEntity.setGameLobbyEntity(null);
        gameLobbyEntity.setNumPlayers(gameLobbyEntity.getNumPlayers()-1);
        gameLobbyEntityRepository.save(gameLobbyEntity);
        playerEntityRepository.save(playerEntity);
        return playerEntity;
    }

    @Override
    public void deletePlayer(Long id) {
        playerEntityRepository.deleteById(id);
    }
    // alternative suggestion:
//    public void deletePlayer(PlayerEntity playerEntity) {
//        playerEntityRepository.delete(playerEntity);
//    }



    @Override
    public boolean exists(Long id) {
        return playerEntityRepository.existsById(id);
    }

    @Override
    public Optional<PlayerEntity> findPlayerById(Long id) {
        return playerEntityRepository.findById(id);
    }
}
