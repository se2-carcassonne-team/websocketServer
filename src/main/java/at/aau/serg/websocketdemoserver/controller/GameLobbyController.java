package at.aau.serg.websocketdemoserver.controller;

import at.aau.serg.websocketdemoserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketdemoserver.domain.dto.PlayerDto;
import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketdemoserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketdemoserver.mapper.PlayerMapper;
import at.aau.serg.websocketdemoserver.service.GameLobbyEntityService;
import at.aau.serg.websocketdemoserver.service.PlayerEntityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class GameLobbyController {

    /* Autowired explained:

    Autowired automatically handles the injection of an object into our controller class

    Example:

    @Autowired:
    Service service;

    Without this annotation you would have to manually instantiate the class the old fashioned way:

    Service service = new Service();
    */

    private GameLobbyEntityService gameLobbyService;
    private PlayerEntityService playerService;
    private final ObjectMapper objectMapper;
    private final GameLobbyMapper gameLobbyMapper;
    private final PlayerMapper playerMapper;

    public GameLobbyController(GameLobbyEntityService gameLobbyService, PlayerEntityService playerService, ObjectMapper objectMapper, GameLobbyMapper gameLobbyMapper, PlayerMapper playerMapper) {
        this.gameLobbyService = gameLobbyService;
        this.playerService = playerService;
        this.objectMapper = objectMapper;
        this.gameLobbyMapper = gameLobbyMapper;
        this.playerMapper = playerMapper;
    }

    @MessageMapping("/create-lobby")
    @SendTo("/topic/create-lobby-response")
    public String createLobby(String gameLobbyDtoAndPlayerDtoJson) throws JsonProcessingException {
        // TODO: Error handling

        String[] splitJsonStrings = gameLobbyDtoAndPlayerDtoJson.split("\\|");

        String gameLobbyDtoJson = splitJsonStrings[0];
        String playerDtoJson = splitJsonStrings[1];

        GameLobbyDto gameLobbyDto = objectMapper.readValue(gameLobbyDtoJson, GameLobbyDto.class);
        PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

        GameLobbyEntity gameLobbyEntity = gameLobbyMapper.mapToEntity(gameLobbyDto);
        PlayerEntity playerEntity = playerMapper.mapToEntity(playerDto);

        GameLobbyEntity createdGameLobbyEntity = gameLobbyService.createLobby(gameLobbyEntity);
        playerService.joinLobby(createdGameLobbyEntity, playerEntity);

        return objectMapper.writeValueAsString(gameLobbyMapper.mapToDto(createdGameLobbyEntity));
    }
}
