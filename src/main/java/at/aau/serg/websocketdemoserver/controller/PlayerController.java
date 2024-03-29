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
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
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
    @MessageMapping("/player-create")
    //@SendTo("/topic/create-user-response")
    @SendTo("/topic/websocket-broker-response")
    public String handleCreatePlayer(String playerDtoJson) {
        try {
            // read in the JSON String and convert to PlayerDTO Object
            PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

            PlayerEntity createdPlayerEntity = playerEntityService.createPlayer(playerMapper.mapToEntity(playerDto));
            // return the DTO of the created player
            return objectMapper.writeValueAsString(playerMapper.mapToDto(createdPlayerEntity));

        } catch (JsonProcessingException e) {
            return e.getMessage();
        } catch (EntityExistsException e) {
            return e.getMessage();
        }

    }

    @MessageMapping("/player-join-lobby")
    //@SendTo("/topic/player-join-lobby-response")
    @SendTo("/topic/websocket-broker-response")
    public String handlePlayerJoinLobby(String gameLobbyDtoAndPlayerDtoJson) {

        // TODO: error handling, e.g. when the lobby to join doesn't exist, etc.

        // 1) extract GameLobbyDto and PlayerDto objects from the string payload:
        String[] splitJsonStrings = gameLobbyDtoAndPlayerDtoJson.split("\\|");
        String gameLobbyDtoJson = splitJsonStrings[0];
        String playerDtoJson = splitJsonStrings[1];

        try {
            GameLobbyDto gameLobbyDto = objectMapper.readValue(gameLobbyDtoJson, GameLobbyDto.class);
            PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);
            // 2) convert the DTOs to Entity Objects for Service:
            PlayerEntity playerEntity = playerMapper.mapToEntity(playerDto);
            GameLobbyEntity gameLobbyEntity = gameLobbyMapper.mapToEntity(gameLobbyDto);

            // 3) player joins lobby:
            PlayerEntity updatedPlayerEntity = playerEntityService.joinLobby(gameLobbyEntity, playerEntity);

            PlayerDto dto = playerMapper.mapToDto(updatedPlayerEntity);

            // return the dto equivalent of the updated player entity
            return objectMapper.writeValueAsString(dto);

        } catch (JsonProcessingException e) {
            return e.getMessage();
        } catch (EntityNotFoundException e) {
            return e.getMessage();
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @MessageMapping("/player-update-username")
    @SendTo("/topic/websocket-broker-response")
    public String handlePlayerUpdateUsername(String playerDtoJson) throws JsonProcessingException {

        // convert the sent String content to the playerDto object we can work with:
        PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

        PlayerEntity playerEntity = playerMapper.mapToEntity(playerDto);

        PlayerEntity updatedPlayerEntity = playerEntityService.updateUsername(playerEntity.getId(), playerEntity);
        return objectMapper.writeValueAsString(playerMapper.mapToDto(updatedPlayerEntity));

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

        // TODO: think into the future --> is this response message enough or should we also include the updated gameLobbyDto?
        return objectMapper.writeValueAsString(playerMapper.mapToDto(updatedPlayerEntity));
    }

    @MessageMapping("/player-delete")
    @SendTo("/topic/websocket-broker-response")
    public String handleDeletePlayer(String playerDtoJson) throws JsonProcessingException {
        PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

        playerEntityService.deletePlayer(playerDto.getId());
        Optional<PlayerEntity> shouldBeEmpty = playerEntityService.findPlayerById(playerDto.getId());
        if (shouldBeEmpty.isEmpty()){
            return  "player no longer exists in database";
        } else {
            return "ERROR! player still exists in database";
        }

    }
}
