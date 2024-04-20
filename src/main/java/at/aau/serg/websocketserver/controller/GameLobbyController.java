package at.aau.serg.websocketserver.controller;

import at.aau.serg.websocketserver.controller.helper.HelperMethods;
import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketserver.domain.dto.PlayerDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketserver.mapper.PlayerMapper;
import at.aau.serg.websocketserver.service.GameLobbyEntityService;
import at.aau.serg.websocketserver.service.PlayerEntityService;
import at.aau.serg.websocketserver.statuscode.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

import static at.aau.serg.websocketserver.controller.helper.HelperMethods.getGameLobbyDtoList;
import static at.aau.serg.websocketserver.controller.helper.HelperMethods.getPlayerDtosInLobbyList;

@Controller
public class GameLobbyController {

    private final GameLobbyEntityService gameLobbyEntityService;
    private final PlayerEntityService playerEntityService;
    private final ObjectMapper objectMapper;
    private final GameLobbyMapper gameLobbyMapper;
    private final PlayerMapper playerMapper;
    private final SimpMessagingTemplate template;

    public GameLobbyController(GameLobbyEntityService gameLobbyEntityService, PlayerEntityService playerEntityService, ObjectMapper objectMapper, GameLobbyMapper gameLobbyMapper, PlayerMapper playerMapper, SimpMessagingTemplate template) {
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.playerEntityService = playerEntityService;
        this.objectMapper = objectMapper;
        this.gameLobbyMapper = gameLobbyMapper;
        this.playerMapper = playerMapper;
        this.template = template;
    }


    /**
     * Ideas for the endpoint: /app/lobby-create
     * <p>sends responses to:</p>
     * <p> 1) /user/queue/response --> updated playerDto (with lobbyId set to id of the created lobby) (response code: 101)</p>
     * <p> 2) /topic/lobby-list --> updated list of lobbies (now includes the newly created lobby) (response code: 301)</p>
     * <p> 3) /topic/lobby-$id !!! NOT POSSIBLE !!!
     * lobby creator himself doesn't know the lobby id yet!
     * Do some trickery on frontend to get around this?
     * Or separate lobby joining from lobby creation and then make two subsequent calls on the frontend (1. create lobby, 2. join lobby)? </p>
     * @param gameLobbyDtoAndPlayerDtoJson
     * @return
     */
    @MessageMapping("/lobby-create")
    @SendToUser("/queue/response")
    public String handleLobbyCreation(String gameLobbyDtoAndPlayerDtoJson) {

        try {
            String[] splitJsonStrings = gameLobbyDtoAndPlayerDtoJson.split("\\|");

            GameLobbyDto gameLobbyDto = objectMapper.readValue(splitJsonStrings[0], GameLobbyDto.class);

            try {
                PlayerDto playerDto = objectMapper.readValue(splitJsonStrings[1], PlayerDto.class);
                gameLobbyDto.setLobbyCreatorId(playerDto.getId());

                GameLobbyEntity createdGameLobbyEntity = gameLobbyEntityService.createLobby(gameLobbyMapper.mapToEntity(gameLobbyDto));
                PlayerEntity playerEntity = playerEntityService.joinLobby(createdGameLobbyEntity.getId(), playerMapper.mapToEntity(playerDto));

                // send updated list of lobbies to /topic/lobby-list
                String updatedLobbyList = objectMapper.writeValueAsString(getGameLobbyDtoList(gameLobbyEntityService, gameLobbyMapper));
                this.template.convertAndSend("/topic/lobby-list", updatedLobbyList);

                // send updated list of players in lobby to /topic/lobby-$id
                // IMPORTANT: not relevant, as player does not know the lobby id when calling lobby-create
                String updatedPlayerList = objectMapper.writeValueAsString(getPlayerDtosInLobbyList(createdGameLobbyEntity.getId(), gameLobbyEntityService, playerEntityService, playerMapper));
                this.template.convertAndSend("/topic/lobby-"+createdGameLobbyEntity.getId(), updatedPlayerList);

                // return updated playerDto to queue
                return objectMapper.writeValueAsString(gameLobbyMapper.mapToDto(playerEntity.getGameLobbyEntity()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(ErrorCode.ERROR_2004.getErrorCode());
            }

            // TODO: check if backend updates the gameLobbyDto Id to the one contained in the returned PlayerDto Object
        } catch (JsonProcessingException e) {
            throw new RuntimeException(ErrorCode.ERROR_1006.getErrorCode());
        }
    }


    // TODO: adapt + test
    /**
     * Ideas for the endpoint: /app/lobby-name-update
     * <p>sends responses to:</p>
     * <p> 1) /topic/lobby-$id --> updated lobby name (response code: 202)</p>
     * <p> 2) /topic/lobby-list --> updated list of lobbies (with updated lobby-name) (response code: 301)</p>
     * @param gameLobbyDtoJson
     * @return
     * @throws JsonProcessingException
     */
    @MessageMapping("/lobby-name-update")
    @SendTo("/topic/game-lobby-response")
    public String handleLobbyNameUpdate(String gameLobbyDtoJson) throws JsonProcessingException {
        GameLobbyDto gameLobbyDto = objectMapper.readValue(gameLobbyDtoJson, GameLobbyDto.class);
        GameLobbyEntity updatedGameLobbyEntity = gameLobbyEntityService.updateLobbyName(gameLobbyMapper.mapToEntity(gameLobbyDto));
        return objectMapper.writeValueAsString(gameLobbyMapper.mapToDto(updatedGameLobbyEntity));
    }


    /**
     * Ideas for the endpoint: /app/lobby-list
     * <p>sends responses to:</p>
     * <p> 1) /user/queue/response --> current list of lobbies (response code: 301)</p>
     * @return
     * @throws JsonProcessingException
     */
    @MessageMapping("/lobby-list")
    @SendToUser("/queue/lobby-list-response")
    public String handleGetAllLobbies() throws JsonProcessingException {
        List<GameLobbyDto> gameLobbyDtos = HelperMethods.getGameLobbyDtoList(gameLobbyEntityService, gameLobbyMapper);
        return objectMapper.writeValueAsString(gameLobbyDtos);
    }


    // TODO: adapt + test
    /**
     * Ideas for the endpoint: /app/lobby-delete
     * <p>sends responses to:</p>
     * <p> 1) /topic/lobby-list --> updated list of lobbies (301)</p>
     * <p> 2) /topic/lobby-$id --> some kind of exit code that the lobby was deleted (response-code: 203) TODO frontend: handle this exit code (e.g. by leaving the lobby activity & setting lobbyId of playerDto to null)</p>
     * <p> 3) /user/queue/response ?not really needed? </p>
     * @param gameLobbyIdString
     * @return
     */
    @MessageMapping("/lobby-delete")
    @SendToUser("/queue/lobby-response")
    public String handleDeleteLobby(String gameLobbyIdString) {

        Long gameLobbyId = Long.parseLong(gameLobbyIdString);

        gameLobbyEntityService.deleteLobby(gameLobbyId);
        if (gameLobbyEntityService.findById(gameLobbyId).isPresent()) {
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
