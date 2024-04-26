package at.aau.serg.websocketserver.controller;

import at.aau.serg.websocketserver.controller.helper.HelperMethods;
import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
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

@Controller
public class GameSessionController {

    private final SimpMessagingTemplate template;
    private final GameSessionEntityService gameSessionEntityService;
    private final ObjectMapper objectMapper;
    private GameSessionMapper gameSessionMapper;
    private GameLobbyMapper gameLobbyMapper;
    private GameLobbyEntityService gameLobbyEntityService;

    private TileDeckRepository tileDeckRepository;

    private TileDeckEntityServiceImpl tileDeckEntityServiceImpl;



    public GameSessionController(SimpMessagingTemplate template,
                                 GameSessionEntityService gameSessionEntityService,
                                 ObjectMapper objectMapper,
                                 GameSessionMapper gameSessionMapper,
                                 GameLobbyMapper gameLobbyMapper,
                                 GameLobbyEntityService gameLobbyEntityService,
                                 TileDeckRepository tileDeckRepository,
                                 TileDeckEntityServiceImpl tileDeckEntityServiceImpl){
        this.template = template;
        this.gameSessionEntityService = gameSessionEntityService;
        this.objectMapper = objectMapper;
        this.gameSessionMapper = gameSessionMapper;
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
    @SendToUser("/queue/next-turn-response")
    public String getPlayerIdAndNextCardId(String gameSessionId) throws JsonProcessingException {

        Long gameSessionIdLong = Long.parseLong(gameSessionId);

//         Get the next player id from the gameSession
        Long playerId = gameSessionEntityService.calculateNextPlayer(gameSessionIdLong);

//        Get the right tile deck based on gameId and draw the next card
        TileDeckEntity tileDeck = tileDeckRepository.findByGameSessionId(gameSessionIdLong);
        Long drawnCardId = tileDeckEntityServiceImpl.drawNextTile(tileDeck);

//        Create the nextTurnDto
        NextTurnDto nextTurnDto = new NextTurnDto(playerId, drawnCardId);

        return objectMapper.writeValueAsString(nextTurnDto);
    }



    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        return "ERROR: " + exception.getMessage();
    }
}
