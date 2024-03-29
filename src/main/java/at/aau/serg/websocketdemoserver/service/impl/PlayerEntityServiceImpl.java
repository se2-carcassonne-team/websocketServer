package at.aau.serg.websocketdemoserver.service.impl;

import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketdemoserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketdemoserver.domain.entity.repository.PlayerEntityRepository;
import at.aau.serg.websocketdemoserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketdemoserver.mapper.PlayerMapper;
import at.aau.serg.websocketdemoserver.service.PlayerEntityService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
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
    public PlayerEntity createPlayer(PlayerEntity playerEntity) throws EntityExistsException {

        // check if player with id already exists
        if(playerEntity.getId() != null && playerEntityRepository.findById(playerEntity.getId()).isPresent()) {
            throw new EntityExistsException("A player with the id:" + playerEntity.getId() + " already exists");
        }

        // check if player with username already exists --> extra method in repository: findPlayerEntitiesByUsername
        if(playerEntity.getUsername() != null && !playerEntityRepository.findPlayerEntitiesByUsername(playerEntity.getUsername()).isEmpty()) {
            throw new EntityExistsException("A player with the username:" + playerEntity.getUsername() + " already exists");
        }

        return playerEntityRepository.save(playerEntity);
    }

    @Override
    public PlayerEntity updateUsername(PlayerEntity playerEntity) throws EntityNotFoundException {

        // retrieve player entity from database, then update only the username
        return playerEntityRepository.findById(playerEntity.getId()).map( playerEntityToUpdate -> {
            Optional.ofNullable(playerEntity.getUsername()).ifPresent(playerEntityToUpdate::setUsername);
            return playerEntityRepository.save(playerEntityToUpdate);
        }).orElseThrow(()-> new EntityNotFoundException("Player does not exist"));

    }


    /**
     * updates the PlayerEntity in the Database to reference gameLobbyEntity and
     * updates the GameLobbyEntity in the Database by incrementing numPlayers by 1
     * @param gameLobbyEntity  the gameLobbyEntity to join
     * @param playerEntity  the PlayerEntity who wants to join the gameLobbyEntity
     * @return updated PlayerEntity from the database
     */
    @Override
    public PlayerEntity joinLobby(GameLobbyEntity gameLobbyEntity, PlayerEntity playerEntity) throws EntityNotFoundException, RuntimeException {

        // Check if lobby exists. Only if lobby exists can player join
        Optional<GameLobbyEntity> gameLobbyEntityInDatabase = gameLobbyEntityRepository.findById(gameLobbyEntity.getId());
        if (gameLobbyEntityInDatabase.isEmpty()) {
            throw new EntityNotFoundException("GameLobbyEntity with the id:" + gameLobbyEntity.getId() + " doesn't exist");
        }

        // only allow lobby join if lobby is not full yet
        if (gameLobbyEntityInDatabase.get().getNumPlayers() > 5) {
            throw new RuntimeException("The game lobby is already full");
        }


        // update the PlayerEntity's gameLobby property to reference the gameLobbyEntity which should be joined
        playerEntity.setGameLobbyEntity(gameLobbyEntity);
        // update the numPlayers property of the gameLobbyEntity by 1
        gameLobbyEntity.setNumPlayers(gameLobbyEntity.getNumPlayers()+1);
        gameLobbyEntityRepository.save(gameLobbyEntity);

        return playerEntityRepository.save(playerEntity);
    }


    @Override
    public PlayerEntity leaveLobby(GameLobbyEntity gameLobbyEntity, PlayerEntity playerEntity) {

        // TODO: exceptions?

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
