package at.aau.serg.websocketdemoserver.controller;

import at.aau.serg.websocketdemoserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketdemoserver.domain.dto.PlayerDto;
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

    @Autowired
    GameLobbyEntityService gameLobbyService;
    @Autowired
    PlayerEntityService playerService;
    @Autowired
    ObjectMapper objectMapper;

    public GameLobbyController(GameLobbyEntityService gameLobbyService, PlayerEntityService playerService) {
        this.gameLobbyService = gameLobbyService;
        this.playerService = playerService;
    }

    @MessageMapping("/create-lobby")
    @SendTo("/topic/create-lobby-response")
    public String handleLobbyJoin(String gameLobbyDtoAndPlayerDtoJson) throws JsonProcessingException {
        // TODO: Replace!

        String[] splitJsonStrings = gameLobbyDtoAndPlayerDtoJson.split("\\|");

        String gameLobbyDtoJson = splitJsonStrings[0];
        String playerDtoJson = splitJsonStrings[1];

        GameLobbyDto gameLobbyDto = objectMapper.readValue(gameLobbyDtoJson, GameLobbyDto.class);
        PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

        return "echo from broker: " + objectMapper.writeValueAsString("");
    }
}
