package at.aau.serg.websocketserver.controller;

import at.aau.serg.websocketserver.controller.helper.HelperMethods;
import at.aau.serg.websocketserver.domain.dto.PlacedTileDto;
import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketserver.domain.pojo.GameState;
import at.aau.serg.websocketserver.domain.dto.NextTurnDto;
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import at.aau.serg.websocketserver.domain.entity.TileDeckEntity;
import at.aau.serg.websocketserver.domain.entity.repository.TileDeckRepository;
import at.aau.serg.websocketserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketserver.mapper.GameSessionMapper;
import at.aau.serg.websocketserver.service.GameLobbyEntityService;
import at.aau.serg.websocketserver.service.GameSessionEntityService;
import at.aau.serg.websocketserver.service.impl.TileDeckEntityServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
public class GameSessionController {

    private final SimpMessagingTemplate template;
    private final GameSessionEntityService gameSessionEntityService;
    private final ObjectMapper objectMapper;
    private GameLobbyMapper gameLobbyMapper;
    private GameLobbyEntityService gameLobbyEntityService;

    private TileDeckRepository tileDeckRepository;

    private TileDeckEntityServiceImpl tileDeckEntityServiceImpl;
    private static final String GAME_SESSION_TOPIC = "/topic/game-session-";


    public GameSessionController(SimpMessagingTemplate template, GameSessionEntityService gameSessionEntityService, ObjectMapper objectMapper, GameLobbyMapper gameLobbyMapper, GameLobbyEntityService gameLobbyEntityService, TileDeckRepository tileDeckRepository, TileDeckEntityServiceImpl tileDeckEntityServiceImpl) {
        this.template = template;
        this.gameSessionEntityService = gameSessionEntityService;
        this.objectMapper = objectMapper;
        this.gameLobbyMapper = gameLobbyMapper;
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.tileDeckRepository = tileDeckRepository;
        this.tileDeckEntityServiceImpl = tileDeckEntityServiceImpl;
    }

    /**
     * Topics/Queues for the Endpoint /app/game-start
     * <p>1) /topic/lobby-$id-game-start -> id of the created gameSession (acts as a signal for all players in a lobby that a gameSession has started)</p>
     * <p>2) /user/queue/lobby-list-response -> updated list of gameLobbies</p>
     * <p>3) /user/queue/errors -> relay for Exceptions (once a exception occurs it will be sent to this topic)</p>
     *
     * @param gameLobbyIdString Id of the GameLobby, that serves as a basis for the GameSession
     * @return
     * @throws JsonProcessingException
     */
    @MessageMapping("/game-start")
    @SendToUser("/queue/lobby-list-response")
    public String createGameSession(String gameLobbyIdString) throws JsonProcessingException {
        Long gameLobbyId = Long.parseLong(gameLobbyIdString);
        GameSessionEntity gameSessionEntity = gameSessionEntityService.createGameSession(gameLobbyId);

        // Get list of lobbies and broadcast it to all subscribers
        List<GameLobbyDto> gameLobbyDtoList = HelperMethods.getGameLobbyDtoList(gameLobbyEntityService, gameLobbyMapper);
        this.template.convertAndSend("/topic/lobby-" + gameLobbyId + "/game-start", objectMapper.writeValueAsString(gameSessionEntity.getId()));

        this.template.convertAndSend("/topic/lobby-list", objectMapper.writeValueAsString(gameLobbyDtoList));

        return objectMapper.writeValueAsString(gameLobbyDtoList);
    }

    /**
     * Topics/Queues for the Endpoint /app/next-turn
     * <p>1) /user/queue/next-turn-response -> nextTurnDto</p>
     * <p>2) /user/queue/errors -> relay for Exceptions (once a exception occurs it will be sent to this topic)</p>
     *
     * @param gameSessionId Id of the GameSession
     * @return
     * @throws JsonProcessingException
     */
    @MessageMapping("/next-turn")
    public String getNextPlayerIdAndNextCardId(String gameSessionId) throws JsonProcessingException {

        Long gameSessionIdLong = Long.parseLong(gameSessionId);

        Optional<GameSessionEntity> optionalGameSession = gameSessionEntityService.findById(gameSessionIdLong);

        if (optionalGameSession.isPresent()) {

            GameSessionEntity currentGameSession = optionalGameSession.get();

            if (!currentGameSession.getGameState().equals(GameState.FINISHED.name())) {
                Long playerId = gameSessionEntityService.calculateNextPlayer(gameSessionIdLong);

//              Get the right tile deck based on gameId and check if it is empty
                TileDeckEntity tileDeck = tileDeckRepository.findByGameSessionId(gameSessionIdLong);
                if (!tileDeckEntityServiceImpl.isTileDeckEmpty(tileDeck)) {
//                    If not empty draw the next tile
                    Long drawnCardId = tileDeckEntityServiceImpl.drawNextTile(tileDeck);

                    // Create the nextTurnDto
                    NextTurnDto nextTurnDto = new NextTurnDto(playerId, drawnCardId);
//                    Send the nextTurnDto to the user to specific gameSession
                    this.template.convertAndSend(GAME_SESSION_TOPIC + gameSessionId + "/next-turn-response", objectMapper.writeValueAsString(nextTurnDto));
                    return objectMapper.writeValueAsString(nextTurnDto);
                } else {
//                    If the deck is empty finish the game
                    gameSessionEntityService.terminateGameSession(gameSessionIdLong);
//                    Send the finish game message to all users when the game is finished
                    String currentGameState = gameSessionEntityService.findById(gameSessionIdLong).get().getGameState();
                    this.template.convertAndSend(GAME_SESSION_TOPIC + gameSessionId + "/game-finished", currentGameState);
                    return GameState.FINISHED.name();
                }
            } else {
                throw new IllegalStateException("Game is already finished.");
            }
        } else {
            throw new IllegalStateException("GameSession not found.");
        }
    }

    /**
     * forwards the placed tile to all other players in the gameSession
     * @param placedTile the tile placed by the player
     * @throws JsonProcessingException
     */
    @MessageMapping("/place-tile")
    public void forwardPlacedTile(String placedTile) throws JsonProcessingException {
        // TODO: catch json processing error
        PlacedTileDto placedTileDto = objectMapper.readValue(placedTile, PlacedTileDto.class);

        // forward placedTile to the other players:
        this.template.convertAndSend(
                GAME_SESSION_TOPIC + placedTileDto.getGameSessionId() + "/tile",
                placedTile
        );
    }



    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        return "ERROR: " + exception.getMessage();
    }
}
