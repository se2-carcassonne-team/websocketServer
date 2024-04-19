package at.aau.serg.websocketserver.controller;

import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import at.aau.serg.websocketserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketserver.mapper.GameSessionMapper;
import at.aau.serg.websocketserver.service.GameLobbyEntityService;
import at.aau.serg.websocketserver.service.GameSessionEntityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
public class GameSessionController {

    private final SimpMessagingTemplate template;
    private final GameSessionEntityService gameSessionEntityService;
    private final ObjectMapper objectMapper;
    private GameSessionMapper gameSessionMapper;
    private GameLobbyMapper gameLobbyMapper;
    private GameLobbyEntityService gameLobbyEntityService;


    public GameSessionController(SimpMessagingTemplate template, GameSessionEntityService gameSessionEntityService, ObjectMapper objectMapper, GameSessionMapper gameSessionMapper, GameLobbyMapper gameLobbyMapper, GameLobbyEntityService gameLobbyEntityService) {
        this.template = template;
        this.gameSessionEntityService = gameSessionEntityService;
        this.objectMapper = objectMapper;
        this.gameSessionMapper = gameSessionMapper;
        this.gameLobbyMapper = gameLobbyMapper;
        this.gameLobbyEntityService = gameLobbyEntityService;
    }

    @MessageMapping("/game-start")
    @SendToUser("/queue/lobby-list-response")
    public String createGameSession(String gameLobbyIdString) throws JsonProcessingException {
        // Get GameSessionEntity and update lobby as not available
        Long gameLobbyId = Long.parseLong(gameLobbyIdString);
        GameSessionEntity gameSessionEntity = gameSessionEntityService.createGameSession(gameLobbyId);

        List<GameLobbyEntity> gameLobbyEntities = gameLobbyEntityService.getListOfLobbies();
        List<GameLobbyDto> gameLobbyDtos = new ArrayList<>();

        for (GameLobbyEntity gameLobbyEntity : gameLobbyEntities) {
            gameLobbyDtos.add(gameLobbyMapper.mapToDto(gameLobbyEntity));
        }

        this.template.convertAndSend("/topic/lobby-" + gameLobbyId + "/game-start", objectMapper.writeValueAsString(gameSessionEntity.getId()));
        return objectMapper.writeValueAsString(gameLobbyDtos);
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        return "ERROR: " + exception.getMessage();
    }
}
