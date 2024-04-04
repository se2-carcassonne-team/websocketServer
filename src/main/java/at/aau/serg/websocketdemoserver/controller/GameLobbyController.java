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
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
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


    @SubscribeMapping (could be useful, for example for initial loading of data):
    @SubscribeMapping works only once every time the user (re)subscribes to the application controller under /app/...
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

    @MessageMapping("/lobby-create")
    @SendToUser("/queue/lobby-response")
    public String handleLobbyCreation(String gameLobbyDtoAndPlayerDtoJson) throws JsonProcessingException {
        String[] splitJsonStrings = gameLobbyDtoAndPlayerDtoJson.split("\\|");

        GameLobbyDto gameLobbyDto = objectMapper.readValue(splitJsonStrings[0], GameLobbyDto.class);
        PlayerDto playerDto = objectMapper.readValue(splitJsonStrings[1], PlayerDto.class);
        GameLobbyEntity gameLobbyEntity = gameLobbyMapper.mapToEntity(gameLobbyDto);
        PlayerEntity playerEntity = playerMapper.mapToEntity(playerDto);

        GameLobbyEntity createdGameLobbyEntity = gameLobbyService.createLobby(gameLobbyEntity);
        playerService.joinLobby(createdGameLobbyEntity, playerEntity);
        return objectMapper.writeValueAsString(gameLobbyMapper.mapToDto(createdGameLobbyEntity));
    }

    @MessageMapping("/lobby-name-update")
    @SendTo("/topic/game-lobby-response")
    public String handleLobbyNameUpdate(String gameLobbyDtoJson) throws JsonProcessingException {
        GameLobbyDto gameLobbyDto = objectMapper.readValue(gameLobbyDtoJson, GameLobbyDto.class);
        GameLobbyEntity updatedGameLobbyEntity = gameLobbyService.updateLobbyName(gameLobbyMapper.mapToEntity(gameLobbyDto));
        return objectMapper.writeValueAsString(gameLobbyMapper.mapToDto(updatedGameLobbyEntity));
    }

    @MessageMapping("/lobby-list")
    @SendToUser("/queue/lobby-response")
    public String handleGetAllLobbies() throws JsonProcessingException {
        List<GameLobbyEntity> gameLobbyEntities = gameLobbyService.getListOfLobbies();
        List<GameLobbyDto> gameLobbyDtos = new ArrayList<>();

        for (GameLobbyEntity gameLobbyEntity : gameLobbyEntities) {
            gameLobbyDtos.add(gameLobbyMapper.mapToDto(gameLobbyEntity));
        }

        return objectMapper.writeValueAsString(gameLobbyDtos);
    }

    @MessageMapping("/lobby-delete")
    @SendToUser("/queue/lobby-response")
    public String handleDeleteLobby(String gameLobbyDtoJson) throws JsonProcessingException {
        GameLobbyDto gameLobbyDto = objectMapper.readValue(gameLobbyDtoJson, GameLobbyDto.class);

        gameLobbyService.deleteLobby(gameLobbyDto.getId());
        if (gameLobbyService.findById(gameLobbyDto.getId()).isPresent()) {
            throw new RuntimeException("gameLobby was not deleted");
        }
        return "deleted";
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        return "ERROR: " + exception.getMessage();
    }
}
