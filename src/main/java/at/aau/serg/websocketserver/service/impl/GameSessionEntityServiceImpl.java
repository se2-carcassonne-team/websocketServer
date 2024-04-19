package at.aau.serg.websocketserver.service.impl;

import at.aau.serg.websocketserver.domain.dto.GameState;
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

    public GameSessionEntityServiceImpl(GameLobbyEntityRepository gameLobbyEntityRepository, GameSessionEntityRepository gameSessionEntityRepository, PlayerEntityServiceImpl playerEntityService) {
        this.gameLobbyEntityRepository = gameLobbyEntityRepository;
        this.gameSessionEntityRepository = gameSessionEntityRepository;
        this.playerEntityService = playerEntityService;
    }

    @Override
    public GameSessionEntity createGameSession(Long gameLobbyId) throws EntityExistsException {

        Optional<GameLobbyEntity> gameLobbyEntityOptional = gameLobbyEntityRepository.findById(gameLobbyId);

        if(gameLobbyEntityOptional.isPresent()) {
            GameLobbyEntity gameLobbyEntity = gameLobbyEntityOptional.get();
            gameLobbyEntity.setGameState(GameState.IN_GAME.name());
            gameLobbyEntityRepository.save(gameLobbyEntity);

            GameSessionEntity gameSessionEntity = new GameSessionEntity();
            gameSessionEntity.setGameState(GameState.IN_GAME.name());
            gameSessionEntity.setTurnPlayerId(gameLobbyEntity.getLobbyCreatorId());

            List<PlayerEntity> playerEntityList = playerEntityService.getAllPlayersForLobby(gameLobbyId);

            List<Long> playerIds = new ArrayList<>(playerEntityList.size());
            for(int i = 0; i < playerEntityList.size(); i++) {
                playerIds.add(playerEntityList.get(i).getId());
            }
            gameSessionEntity.setPlayerIds(playerIds);

            return gameSessionEntityRepository.save(gameSessionEntity);
        } else {
            throw new EntityNotFoundException(ErrorCode.ERROR_1003.getErrorCode());
        }
    }
}
