package at.aau.serg.websocketserver.service.impl;

import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketserver.domain.entity.repository.GameSessionEntityRepository;
import at.aau.serg.websocketserver.domain.entity.repository.PlayerEntityRepository;
import at.aau.serg.websocketserver.statuscode.ErrorCode;
import at.aau.serg.websocketserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketserver.mapper.PlayerMapper;
import at.aau.serg.websocketserver.service.PlayerEntityService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class PlayerEntityServiceImpl implements PlayerEntityService {

    PlayerEntityRepository playerEntityRepository;
    GameLobbyEntityRepository gameLobbyEntityRepository;
    GameSessionEntityRepository gameSessionEntityRepository;
    GameLobbyMapper gameLobbyMapper;
    PlayerMapper playerMapper;
    SecureRandom random;

    public PlayerEntityServiceImpl(
            PlayerEntityRepository playerEntityRepository,
            GameLobbyEntityRepository gameLobbyEntityRepository,
            GameSessionEntityRepository gameSessionEntityRepository,
            GameLobbyMapper gameLobbyMapper,
            PlayerMapper playerMapper)
    {
        this.playerEntityRepository = playerEntityRepository;
        this.gameLobbyEntityRepository = gameLobbyEntityRepository;
        this.gameLobbyMapper = gameLobbyMapper;
        this.playerMapper = playerMapper;
        this.gameSessionEntityRepository = gameSessionEntityRepository;
        this.random = new SecureRandom();
    }

    private void setPlayerColour(PlayerEntity playerEntity, GameLobbyEntity gameLobbyEntity) {
        List<String> colours = gameLobbyEntity.getAvailableColours();

        int randomIndex = random.nextInt(colours.size());
        playerEntity.setPlayerColour(colours.get(randomIndex));
        colours.remove(randomIndex);
        gameLobbyEntity.setAvailableColours(colours);
    }

    // Only used for integration tests
    @Override
    public PlayerEntity createPlayer(PlayerEntity playerEntity) throws EntityExistsException {
        return createPlayer(playerEntity, null);
    }

    @Override
    public PlayerEntity createPlayer(PlayerEntity playerEntity, String sessionId) throws EntityExistsException {
        // check if player with id already exists
        if(playerEntity.getId() != null && playerEntityRepository.findById(playerEntity.getId()).isPresent()) {
            throw new EntityExistsException(ErrorCode.ERROR_2002.getCode());
        }

        // check if player with username already exists --> extra method in repository: findPlayerEntitiesByUsername
        if(playerEntity.getUsername() != null && !playerEntityRepository.findPlayerEntitiesByUsername(playerEntity.getUsername()).isEmpty()) {
            throw new EntityExistsException(ErrorCode.ERROR_2003.getCode());
        }

        if(sessionId != null && !sessionId.isEmpty()) playerEntity.setSessionId(sessionId);
        return playerEntityRepository.save(playerEntity);
    }

    @Override
    public PlayerEntity updateUsername(PlayerEntity playerEntity) throws EntityNotFoundException {

        // retrieve player entity from database, then update only the username
        return playerEntityRepository.findById(playerEntity.getId()).map( playerEntityToUpdate -> {
            Optional.ofNullable(playerEntity.getUsername()).ifPresent(playerEntityToUpdate::setUsername);
            return playerEntityRepository.save(playerEntityToUpdate);
        }).orElseThrow(()-> new EntityNotFoundException(ErrorCode.ERROR_2001.getCode()));

    }


    /**
     * updates the PlayerEntity in the Database to reference gameLobbyEntity and
     * updates the PlayerEntity in the Database with one of 5 predefined PlayerColours and
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
            throw new EntityNotFoundException(ErrorCode.ERROR_1003.getCode());
        }

        // only allow lobby join if lobby is not full yet
        if (gameLobbyEntityInDatabase.get().getNumPlayers() > 4) {
            throw new RuntimeException(ErrorCode.ERROR_1004.getCode());
        }

        GameLobbyEntity gameLobbyEntity = gameLobbyEntityInDatabase.get();

        // update the PlayerEntity's gameLobby property to reference the gameLobbyEntity which should be joined
        playerEntity.setGameLobbyEntity(gameLobbyEntity);

        // Randomly assign player colour
        setPlayerColour(playerEntity, gameLobbyEntity);

        // reset player's points to 0
        playerEntity.setPoints(0);

        // update the numPlayers property of the gameLobbyEntity by 1
        gameLobbyEntity.setNumPlayers(gameLobbyEntity.getNumPlayers()+1);
        gameLobbyEntityRepository.save(gameLobbyEntity);

        // reset playerEntity canCheat property to false
        playerEntity.setCanCheat(false);

        return playerEntityRepository.save(playerEntity);
    }

    @Override
    public List<PlayerEntity> getAllPlayersForLobby(Long gameLobbyId) {
        if(gameLobbyEntityRepository.findById(gameLobbyId).isEmpty()) {
            throw new RuntimeException(ErrorCode.ERROR_1003.getCode());
        }
        return playerEntityRepository.findPlayerEntitiesByGameLobbyEntity_Id(gameLobbyId);
    }


    @Override
    public PlayerEntity leaveLobby(PlayerEntity playerEntity) throws EntityNotFoundException {

        if (playerEntityRepository.findById(playerEntity.getId()).isEmpty()) {
            throw new EntityNotFoundException(ErrorCode.ERROR_2001.getCode());
        }

        GameLobbyEntity gameLobbyEntity = playerEntity.getGameLobbyEntity();

        if (gameLobbyEntity == null) {
            throw new EntityNotFoundException(ErrorCode.ERROR_2005.getCode());
        }

        playerEntity.setGameLobbyEntity(null);

        // Remove colour from player
        String playerColour = playerEntity.getPlayerColour();
        playerEntity.setPlayerColour(null);

        PlayerEntity updatedPlayerEntity =  playerEntityRepository.save(playerEntity);

        // Check if lobby can be deleted
        int numberOfPlayers = gameLobbyEntity.getNumPlayers();
        if(numberOfPlayers > 1) {
            gameLobbyEntity.setNumPlayers(gameLobbyEntity.getNumPlayers()-1);

            // Return playerColour to the list of available colors for reuse
            gameLobbyEntity.getAvailableColours().add(playerColour);

            if(playerEntity.getId() == gameLobbyEntity.getLobbyAdminId()) {
                // transfer lobby admin rights to another player
                List<PlayerEntity> remainingPlayersInLobby = playerEntityRepository.findPlayerEntitiesByGameLobbyEntity_Id(gameLobbyEntity.getId());
                PlayerEntity nextPlayerEntity = remainingPlayersInLobby.get(0);
                gameLobbyEntity.setLobbyAdminId(nextPlayerEntity.getId());
            }

            gameLobbyEntityRepository.save(gameLobbyEntity);
        } else {
            gameLobbyEntityRepository.deleteById(gameLobbyEntity.getId());
        }
        return updatedPlayerEntity;
    }
    public PlayerEntity leaveGameSession(PlayerEntity playerEntity){


        Optional<PlayerEntity> optionalPlayer = playerEntityRepository.findById(playerEntity.getId());
        GameSessionEntity gameSessionEntity= playerEntity.getGameSessionEntity();


        if (optionalPlayer.isEmpty()) {
            throw new EntityNotFoundException(ErrorCode.ERROR_2001.getCode());
        }
        PlayerEntity existingPlayer = optionalPlayer.get();


        // Überprüfen, ob der Spieler einer Spielsitzung zugeordnet ist
        if (gameSessionEntity == null) {

            throw new EntityNotFoundException(ErrorCode.ERROR_3003.getCode());
        }

        // Entfernen der Spieler-ID aus der Liste playerIds der GameSessionEntity
        List<Long> playerIds = gameSessionEntity.getPlayerIds();

        playerIds.remove(existingPlayer.getId());


        // Aktualisieren der Anzahl der Spieler in der Spielsitzung

        gameSessionEntity.setNumPlayers(playerIds.size());

        // Speichern der aktualisierten Spielsitzung

        gameSessionEntityRepository.save(gameSessionEntity);
        // Optional: Setzen des Spielers auf null, um sicherzustellen, dass er nicht mehr in der Spielsitzung ist
        if(playerIds.size()<=1){
            existingPlayer.setGameSessionEntity(null);}

        return playerEntityRepository.save(existingPlayer);
    }
    @Override
    public void deletePlayer(Long id) {
        Optional<PlayerEntity> playerEntityOptional = playerEntityRepository.findById(id);
        if(playerEntityOptional.isPresent()) {
            PlayerEntity playerEntity = playerEntityOptional.get();
            GameLobbyEntity gameLobbyEntity = playerEntity.getGameLobbyEntity();

            if(gameLobbyEntity != null) {
                leaveLobby(playerEntity);
            }
            playerEntityRepository.deleteById(id);
        } else {
            throw new EntityNotFoundException(ErrorCode.ERROR_2001.getCode());
        }
    }



    @Override
    public boolean exists(Long id) {
        return playerEntityRepository.existsById(id);
    }

    @Override
    public Optional<PlayerEntity> findPlayerById(Long id) {
        return playerEntityRepository.findById(id);
    }

    @Override
    public Optional<PlayerEntity> findPlayerBySessionId(String sessionId) {
        return playerEntityRepository.findBySessionId(sessionId);
    }

    @Override
    public List<PlayerEntity> findAllPlayers(List<Long> ids) {
        return playerEntityRepository.findAllById(ids);
    }
}
