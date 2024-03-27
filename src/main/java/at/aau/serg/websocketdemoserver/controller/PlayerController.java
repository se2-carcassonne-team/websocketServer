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
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class PlayerController {

    private PlayerEntityService playerEntityService;
    private GameLobbyEntityService gameLobbyEntityService;
    private ObjectMapper objectMapper;
    private PlayerMapper playerMapper;
    private GameLobbyMapper gameLobbyMapper;

    public PlayerController(PlayerEntityService playerEntityService, GameLobbyEntityService gameLobbyEntityService, ObjectMapper objectMapper, PlayerMapper playerMapper, GameLobbyMapper gameLobbyMapper) {
        this.playerEntityService = playerEntityService;
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.objectMapper = objectMapper;
        this.playerMapper = playerMapper;
        this.gameLobbyMapper = gameLobbyMapper;
    }

    // test value
    @MessageMapping("/create-user")
    //@SendTo("/topic/create-user-response")
    @SendTo("/topic/websocket-broker-response")
    public String handleCreateUser(String playerDtoJson) throws JsonProcessingException {
        // TODO handle the messages here
        // read in the JSON String and convert to PlayerDTO Object
        PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

        PlayerEntity playerEntity = playerEntityService.createPlayer(playerMapper.mapToEntity(playerDto));

        //return "echo from broker: " + HtmlUtils.htmlEscape(playerDto);
        return "response from broker: " + objectMapper.writeValueAsString(playerDto);
    }

    @MessageMapping("/player-join-lobby")
    //@SendTo("/topic/player-join-lobby-response")
    @SendTo("/topic/websocket-broker-response")
    public String handlePlayerJoinLobby(String gameLobbyDtoAndPlayerDtoJson) throws JsonProcessingException {

        // TODO: error handling, e.g. when the lobby to join doesn't exist, etc.

        // 1) extract GameLobbyDto and PlayerDto objects from the string payload:
        String[] splitJsonStrings = gameLobbyDtoAndPlayerDtoJson.split("\\|");
        String gameLobbyDtoJson = splitJsonStrings[0];
        String playerDtoJson = splitJsonStrings[1];

        GameLobbyDto gameLobbyDto = objectMapper.readValue(gameLobbyDtoJson, GameLobbyDto.class);
        PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

        // 2) convert the DTOs to Entity Objects for Service:
        PlayerEntity playerEntity = playerMapper.mapToEntity(playerDto);
        GameLobbyEntity gameLobbyEntity = gameLobbyMapper.mapToEntity(gameLobbyDto);

        // 3) player joins lobby:
        PlayerEntity updatedPlayerEntity = playerEntityService.joinLobby(gameLobbyEntity, playerEntity);


        // TODO: fix up code here, the check if the game lobby exists should be done earlier, as the joinLobby will (probably) create a new LobbyEntity if the given one doesn't exist yet (due to cascading)
        Optional<GameLobbyEntity> updatedGameLobbyEntityOptional = gameLobbyEntityService.findById(gameLobbyEntity.getId());
        if (updatedGameLobbyEntityOptional.isPresent()){
            GameLobbyEntity updatedGameLobbyEntity = updatedGameLobbyEntityOptional.get();
        } else {
            // lobby doesn't exist --> error handling
        }

        PlayerDto dto = playerMapper.mapToDto(updatedPlayerEntity);

        // return the dto equivalent of the updated player entity
        return "response from broker: " + objectMapper.writeValueAsString(dto);
    }

    @MessageMapping("/player-update-username")
    @SendTo("/topic/websocket-broker-response")
    public String handlePlayerUpdateUsername(String playerIdAndPlayerDto) throws JsonProcessingException {
        String[] splitJsonStrings = playerIdAndPlayerDto.split("\\|");
        String playerIdString = splitJsonStrings[0];
        String playerDtoJson = splitJsonStrings[1];

        // convert the sent String content to objects we can work with:
        PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);
        Long playerId = Long.parseLong(playerIdString);

        // TODO: error handling in case of faulty inputs! E.g. try-catch around the Long.parseLong()

        PlayerEntity playerEntity = playerMapper.mapToEntity(playerDto);

        PlayerEntity updatedPlayerEntity = playerEntityService.updateUsername(playerId, playerEntity);

        return "response from broker: " + objectMapper.writeValueAsString(playerMapper.mapToDto(updatedPlayerEntity));
    }

    @MessageMapping("/player-leave-lobby")
    @SendTo("/topic/websocket-broker-response")
    public String handlePlayerLeaveLobby(String gameLobbyDtoAndPlayerDtoJson) throws JsonProcessingException {
        // TODO: error handling

        // 1) extract GameLobbyDto and PlayerDto objects from the string payload:
        String[] splitJsonStrings = gameLobbyDtoAndPlayerDtoJson.split("\\|");
        String gameLobbyDtoJson = splitJsonStrings[0];
        String playerDtoJson = splitJsonStrings[1];

        GameLobbyDto gameLobbyDto = objectMapper.readValue(gameLobbyDtoJson, GameLobbyDto.class);
        PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

        // 2) convert the DTOs to Entity Objects for Service:
        PlayerEntity playerEntity = playerMapper.mapToEntity(playerDto);
        GameLobbyEntity gameLobbyEntity = gameLobbyMapper.mapToEntity(gameLobbyDto);

        // 3) player leaves lobby
        PlayerEntity updatedPlayerEntity = playerEntityService.leaveLobby(gameLobbyEntity, playerEntity);

        return "response from broker: " + objectMapper.writeValueAsString(playerMapper.mapToDto(updatedPlayerEntity));
    }

    @MessageMapping("/player-delete")
    @SendTo("/topic/websocket-broker-response")
    public String handleDeletePlayer(String playerDtoJson) throws JsonProcessingException {
        PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

        playerEntityService.deletePlayer(playerDto.getId());
        Optional<PlayerEntity> shouldBeEmpty = playerEntityService.findPlayerById(playerDto.getId());
        if (shouldBeEmpty.isEmpty()){
            return  "response from broker: player no longer exists in database";
        } else {
            return "response from broker: player still exists in database";
        }

    }
}
