package at.aau.serg.websocketserver.controller;

import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketserver.domain.dto.PlayerDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.errorcode.ErrorCode;
import at.aau.serg.websocketserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketserver.mapper.PlayerMapper;
import at.aau.serg.websocketserver.service.GameLobbyEntityService;
import at.aau.serg.websocketserver.service.PlayerEntityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
public class GameLobbyController {

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
    @SendTo("/topic/game-lobby-response")
    public String handleLobbyCreation(String gameLobbyDtoAndPlayerDtoJson) {

        try {
            String[] splitJsonStrings = gameLobbyDtoAndPlayerDtoJson.split("\\|");

            GameLobbyDto gameLobbyDto = objectMapper.readValue(splitJsonStrings[0], GameLobbyDto.class);

            try {
                PlayerDto playerDto = objectMapper.readValue(splitJsonStrings[1], PlayerDto.class);
                gameLobbyDto.setLobbyCreatorId(playerDto.getId());

                GameLobbyEntity createdGameLobbyEntity = gameLobbyService.createLobby(gameLobbyMapper.mapToEntity(gameLobbyDto));
                PlayerEntity playerEntity = playerService.joinLobby(createdGameLobbyEntity.getId(), playerMapper.mapToEntity(playerDto));

                return objectMapper.writeValueAsString(gameLobbyMapper.mapToDto(playerEntity.getGameLobbyEntity()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(ErrorCode.ERROR_2004.getErrorCode());
            }

            // TODO: check if backend updates the gameLobbyDto Id to the one contained in the returned PlayerDto Object
        } catch (JsonProcessingException e) {
            throw new RuntimeException(ErrorCode.ERROR_1006.getErrorCode());
        }
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
    public String handleDeleteLobby(String gameLobbyIdString) {

        Long gameLobbyId = Long.parseLong(gameLobbyIdString);

        gameLobbyService.deleteLobby(gameLobbyId);
        if (gameLobbyService.findById(gameLobbyId).isPresent()) {
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
