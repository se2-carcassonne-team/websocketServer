package at.aau.serg.websocketserver.websocket;

import at.aau.serg.websocketserver.domain.dto.PlayerDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.domain.entity.repository.GameSessionEntityRepository;
import at.aau.serg.websocketserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketserver.mapper.PlayerMapper;
import at.aau.serg.websocketserver.service.GameLobbyEntityService;
import at.aau.serg.websocketserver.service.GameSessionEntityService;
import at.aau.serg.websocketserver.service.PlayerEntityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Optional;

import static at.aau.serg.websocketserver.controller.helper.HelperMethods.getGameLobbyDtoList;
import static at.aau.serg.websocketserver.controller.helper.HelperMethods.getPlayerDtosInLobbyList;

@Component
public class ClientDisconnectListener {
    private final SimpMessagingTemplate template;
    private final PlayerEntityService playerEntityService;
    private final GameLobbyEntityService gameLobbyEntityService;
    private final GameSessionEntityService gameSessionEntityService;
    private final ObjectMapper objectMapper;
    private final PlayerMapper playerMapper;
    private final GameLobbyMapper gameLobbyMapper;
    private final GameSessionEntityRepository gameSessionEntityRepository;

    public ClientDisconnectListener(SimpMessagingTemplate template, PlayerEntityService playerEntityServiceImpl, GameLobbyEntityService gameLobbyEntityService, GameSessionEntityService gameSessionEntityService, ObjectMapper objectMapper, PlayerMapper playerMapper, GameLobbyMapper gameLobbyMapper, GameSessionEntityRepository gameSessionEntityRepository) {
        this.template = template;
        this.playerEntityService = playerEntityServiceImpl;
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.gameSessionEntityService = gameSessionEntityService;
        this.objectMapper = objectMapper;
        this.playerMapper = playerMapper;
        this.gameLobbyMapper = gameLobbyMapper;
        this.gameSessionEntityRepository = gameSessionEntityRepository;
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) throws JsonProcessingException {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        Optional<PlayerEntity> playerEntityOptional = playerEntityService.findPlayerBySessionId(sessionId);
        if(playerEntityOptional.isPresent()) {
            PlayerEntity playerEntity = playerEntityOptional.get();
            PlayerDto playerDto = playerMapper.mapToDto(playerEntity);

            playerEntityService.deletePlayer(playerDto.getId());

            // Update logic if a player was part of a lobby
            if(playerDto.getGameLobbyId() != null) {
                Optional<GameLobbyEntity> gameLobbyEntityOptional = gameLobbyEntityService.findById(playerDto.getGameLobbyId());
                if(gameLobbyEntityOptional.isPresent()) {
                    GameLobbyEntity gameLobbyEntity = gameLobbyEntityOptional.get();
                    // send response to: /topic/lobby-$id --> updated list of players in lobby (later with response code: 201)
                    this.template.convertAndSend(
                            "/topic/lobby-" + gameLobbyEntity.getId(),
                            objectMapper.writeValueAsString(getPlayerDtosInLobbyList(gameLobbyEntity.getId(), gameLobbyEntityService, playerEntityService, playerMapper))
                    );
                } else {
                    // send response to: /topic/lobby-list --> updated list of lobbies (later with response code 301)
                    this.template.convertAndSend(
                            "/topic/lobby-list",
                            objectMapper.writeValueAsString(getGameLobbyDtoList(gameLobbyEntityService, gameLobbyMapper))
                    );
                }
            }

            Optional<GameSessionEntity> gameSessionEntityOptional = gameSessionEntityRepository.findByPlayerId(playerDto.getId());
            if(gameSessionEntityOptional.isPresent()) {
                GameSessionEntity gameSessionEntity = gameSessionEntityOptional.get();
                List<Long> playerIds = gameSessionEntity.getPlayerIds();

                playerIds.remove(playerDto.getId());
                gameSessionEntity.setPlayerIds(playerIds);
                gameSessionEntityRepository.save(gameSessionEntity);

                // Check if gameSession can be terminated
                if (playerIds.isEmpty()) {
                    gameSessionEntityService.terminateGameSession(gameSessionEntity.getId());
                }

            }

        }

    }
}
