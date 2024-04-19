package at.aau.serg.websocketserver.service.impl;

import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketserver.domain.entity.repository.PlayerEntityRepository;
import at.aau.serg.websocketserver.statuscode.ErrorCode;
import at.aau.serg.websocketserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketserver.mapper.PlayerMapper;
import at.aau.serg.websocketserver.service.PlayerEntityService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
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
            throw new EntityExistsException(ErrorCode.ERROR_2002.getErrorCode());
        }

        // check if player with username already exists --> extra method in repository: findPlayerEntitiesByUsername
        if(playerEntity.getUsername() != null && !playerEntityRepository.findPlayerEntitiesByUsername(playerEntity.getUsername()).isEmpty()) {
            throw new EntityExistsException(ErrorCode.ERROR_2003.getErrorCode());
        }

        return playerEntityRepository.save(playerEntity);
    }

    @Override
    public PlayerEntity updateUsername(PlayerEntity playerEntity) throws EntityNotFoundException {

        // retrieve player entity from database, then update only the username
        return playerEntityRepository.findById(playerEntity.getId()).map( playerEntityToUpdate -> {
            Optional.ofNullable(playerEntity.getUsername()).ifPresent(playerEntityToUpdate::setUsername);
            return playerEntityRepository.save(playerEntityToUpdate);
        }).orElseThrow(()-> new EntityNotFoundException(ErrorCode.ERROR_2001.getErrorCode()));

    }


    /**
     * updates the PlayerEntity in the Database to reference gameLobbyEntity and
     * updates the GameLobbyEntity in the Database by incrementing numPlayers by 1
     * @param gameLobbyId  the gameLobbyId to join
     * @param playerEntity  the PlayerEntity who wants to join the gameLobbyEntity
     * @return updated PlayerEntity from the database
     */
    @Override
    public PlayerEntity joinLobby(Long gameLobbyId, PlayerEntity playerEntity) throws RuntimeException {

        // Check if lobby exists. Only if lobby exists can player join
        Optional<GameLobbyEntity> gameLobbyEntityInDatabase = gameLobbyEntityRepository.findById(gameLobbyId);
        if (gameLobbyEntityInDatabase.isEmpty()) {
            throw new EntityNotFoundException(ErrorCode.ERROR_1003.getErrorCode());
        }

        // only allow lobby join if lobby is not full yet
        if (gameLobbyEntityInDatabase.get().getNumPlayers() > 4) {
            throw new RuntimeException(ErrorCode.ERROR_1004.getErrorCode());
        }

        GameLobbyEntity gameLobbyEntity = gameLobbyEntityInDatabase.get();

        // update the PlayerEntity's gameLobby property to reference the gameLobbyEntity which should be joined
        playerEntity.setGameLobbyEntity(gameLobbyEntity);
        // update the numPlayers property of the gameLobbyEntity by 1
        gameLobbyEntity.setNumPlayers(gameLobbyEntity.getNumPlayers()+1);
        gameLobbyEntityRepository.save(gameLobbyEntity);

        return playerEntityRepository.save(playerEntity);
    }

    @Override
    public List<PlayerEntity> getAllPlayersForLobby(Long gameLobbyId) {
        if(gameLobbyEntityRepository.findById(gameLobbyId).isEmpty()) {
            throw new RuntimeException(ErrorCode.ERROR_1003.getErrorCode());
        }
        return playerEntityRepository.findPlayerEntitiesByGameLobbyEntity_Id(gameLobbyId);
    }


    @Override
    public PlayerEntity leaveLobby(PlayerEntity playerEntity) throws EntityNotFoundException {

        if (playerEntityRepository.findById(playerEntity.getId()).isEmpty()) {
            throw new EntityNotFoundException(ErrorCode.ERROR_2001.getErrorCode());
        }

        GameLobbyEntity gameLobbyEntity = playerEntity.getGameLobbyEntity();

        if (gameLobbyEntity == null) {
            throw new EntityNotFoundException(ErrorCode.ERROR_2005.getErrorCode());
        }

        playerEntity.setGameLobbyEntity(null);
        playerEntityRepository.save(playerEntity);

        // Check if lobby can be deleted
        int numberOfPlayers = gameLobbyEntity.getNumPlayers();
        if(numberOfPlayers > 1) {
            gameLobbyEntity.setNumPlayers(gameLobbyEntity.getNumPlayers()-1);

            if(playerEntity.getId() == gameLobbyEntity.getLobbyCreatorId()) {
                // transfer lobby admin rights to another player
                List<PlayerEntity> remainingPlayersInLobby = playerEntityRepository.findPlayerEntitiesByGameLobbyEntity_Id(gameLobbyEntity.getId());
                PlayerEntity nextPlayerEntity = remainingPlayersInLobby.get(0);
                gameLobbyEntity.setLobbyCreatorId(nextPlayerEntity.getId());
            }

            gameLobbyEntityRepository.save(gameLobbyEntity);
        } else {
            gameLobbyEntityRepository.deleteById(gameLobbyEntity.getId());
        }
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
