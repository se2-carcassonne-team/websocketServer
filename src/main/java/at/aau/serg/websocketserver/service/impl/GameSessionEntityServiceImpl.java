package at.aau.serg.websocketserver.service.impl;

import at.aau.serg.websocketserver.domain.pojo.GameState;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketserver.domain.entity.repository.GameSessionEntityRepository;
import at.aau.serg.websocketserver.service.GameSessionEntityService;
import at.aau.serg.websocketserver.statuscode.ErrorCode;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GameSessionEntityServiceImpl implements GameSessionEntityService {

    GameLobbyEntityRepository gameLobbyEntityRepository;
    GameSessionEntityRepository gameSessionEntityRepository;
    PlayerEntityServiceImpl playerEntityService;
    TileDeckEntityServiceImpl tileDeckEntityService;


    public GameSessionEntityServiceImpl(GameLobbyEntityRepository gameLobbyEntityRepository,
                                        GameSessionEntityRepository gameSessionEntityRepository,
                                        PlayerEntityServiceImpl playerEntityService,
                                        TileDeckEntityServiceImpl tileDeckEntityService) {
        this.gameLobbyEntityRepository = gameLobbyEntityRepository;
        this.gameSessionEntityRepository = gameSessionEntityRepository;
        this.playerEntityService = playerEntityService;
        this.tileDeckEntityService = tileDeckEntityService;
    }

    @Override
    public GameSessionEntity createGameSession(Long gameLobbyId) throws EntityExistsException {

        Optional<GameLobbyEntity> gameLobbyEntityOptional = gameLobbyEntityRepository.findById(gameLobbyId);

        if (gameLobbyEntityOptional.isPresent()) {
            // Get lobby from database, update its status and write changes back to the database
            GameLobbyEntity gameLobbyEntity = gameLobbyEntityOptional.get();
            gameLobbyEntity.setGameState(GameState.IN_GAME.name());
            gameLobbyEntityRepository.save(gameLobbyEntity);

            // Create a new gameSession
            GameSessionEntity gameSessionEntity = new GameSessionEntity();
            gameSessionEntity.setGameState(GameState.IN_GAME.name());
            gameSessionEntity.setTurnPlayerId(gameLobbyEntity.getLobbyAdminId());

            List<PlayerEntity> playerEntityList = playerEntityService.getAllPlayersForLobby(gameLobbyId);

            List<Long> playerIds = new ArrayList<>(playerEntityList.size());
            for (PlayerEntity playerEntity : playerEntityList) {
                playerIds.add(playerEntity.getId());
            }
            gameSessionEntity.setPlayerIds(playerIds);

            GameSessionEntity gameSessionEntityReturned = gameSessionEntityRepository.save(gameSessionEntity);
            tileDeckEntityService.createTileDeck(gameSessionEntity.getId());
            return gameSessionEntityReturned;
        } else {
            throw new EntityNotFoundException(ErrorCode.ERROR_1003.getCode());
        }
    }

    @Override
    public Optional<GameSessionEntity> findById(Long id) throws EntityNotFoundException {
        return gameSessionEntityRepository.findById(id);
    }

    @Override
    public Long calculateNextPlayer(Long gameSessionId) {
        Optional<GameSessionEntity> gameSessionOptional = gameSessionEntityRepository.findById(gameSessionId);

        if (gameSessionOptional.isPresent()) {
//            Get the current player from the gameSession
            GameSessionEntity gameSession = gameSessionOptional.get();
            List<Long> playerIds = gameSession.getPlayerIds();
            Long currentTurnPlayerId = gameSession.getTurnPlayerId();
//        Get the index of the current player
            int currentIndex = playerIds.indexOf(currentTurnPlayerId);
            int nextIndex = (currentIndex + 1) % playerIds.size();
//        Get the next player
            Long nextTurnPlayerId = playerIds.get(nextIndex);
            gameSession.setTurnPlayerId(nextTurnPlayerId);
//        Update the gameSession
            gameSessionEntityRepository.save(gameSession);

            return nextTurnPlayerId;
        } else {
            throw new EntityNotFoundException(ErrorCode.ERROR_3003.getCode());
        }
    }

    @Override
    public GameSessionEntity terminateGameSession(Long gameSessionId) {
        Optional<GameSessionEntity> gameSessionOptional = gameSessionEntityRepository.findById(gameSessionId);

        if (gameSessionOptional.isPresent()) {
            GameSessionEntity gameSession = gameSessionOptional.get();
            gameSession.setGameState(GameState.FINISHED.name());
            return gameSessionEntityRepository.save(gameSession);
        } else {
            throw new EntityNotFoundException(ErrorCode.ERROR_3003.getCode());
        }
    }
}
