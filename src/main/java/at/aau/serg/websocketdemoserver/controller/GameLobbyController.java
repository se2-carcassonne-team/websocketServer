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
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

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
    public String handleLobbyCreation(String gameLobbyDtoAndPlayerDtoJson) throws JsonProcessingException {
        String[] splitJsonStrings = gameLobbyDtoAndPlayerDtoJson.split("\\|");

        GameLobbyDto gameLobbyDto = objectMapper.readValue(splitJsonStrings[0], GameLobbyDto.class);
        PlayerDto playerDto = objectMapper.readValue(splitJsonStrings[1], PlayerDto.class);
        GameLobbyEntity gameLobbyEntity = gameLobbyMapper.mapToEntity(gameLobbyDto);
        PlayerEntity playerEntity = playerMapper.mapToEntity(playerDto);

        try {
            GameLobbyEntity createdGameLobbyEntity = gameLobbyService.createLobby(gameLobbyEntity);
            playerService.joinLobby(createdGameLobbyEntity, playerEntity);
            return objectMapper.writeValueAsString(gameLobbyMapper.mapToDto(createdGameLobbyEntity));
        } catch (Exception e) {
            return "gameLobby creation failed";
        }
    }

    @MessageMapping("/update-lobby-name")
    @SendTo("/topic/update-lobby-name")
    public String handleLobbyNameUpdate(String gameLobbyDtoJson) throws JsonProcessingException {
        GameLobbyDto gameLobbyDto = objectMapper.readValue(gameLobbyDtoJson, GameLobbyDto.class);
        try {
            GameLobbyEntity updatedGameLobbyEntity = gameLobbyService.updateLobbyName(gameLobbyMapper.mapToEntity(gameLobbyDto));
            return objectMapper.writeValueAsString(gameLobbyMapper.mapToDto(updatedGameLobbyEntity));
        } catch (RuntimeException e) {
            return "gameLobby name update failed";
        }

    }

    // TODO: Should send to one user only
    @MessageMapping("/list-lobby")
    @SendTo("/topic/list-lobby-response")
    // Send to User
    public String handleGetAllLobbies() throws JsonProcessingException {
        List<GameLobbyEntity> gameLobbyEntities = gameLobbyService.getListOfLobbies();
        List<GameLobbyDto> gameLobbyDtos = new ArrayList<>();

        for (GameLobbyEntity gameLobbyEntity : gameLobbyEntities) {
            gameLobbyDtos.add(gameLobbyMapper.mapToDto(gameLobbyEntity));
        }

        return objectMapper.writeValueAsString(gameLobbyDtos);
    }

    @MessageMapping("/delete-lobby")
    @SendTo("/topic/delete-lobby-response")
    public String handleDeleteLobby(String gameLobbyDtoJson) throws JsonProcessingException {
        GameLobbyDto gameLobbyDto = objectMapper.readValue(gameLobbyDtoJson, GameLobbyDto.class);

        gameLobbyService.deleteLobby(gameLobbyDto.getId());
        if (gameLobbyService.findById(gameLobbyDto.getId()).isEmpty()) {
            return "gameLobby no longer exists";
        }

        return "ERROR! gameLobby still exists in database";
    }

    @MessageExceptionHandler
    @SendTo("/queue/errors")
    public String handleException(Throwable exception) {
        return "server exception: " + exception.getMessage();
    }
}
