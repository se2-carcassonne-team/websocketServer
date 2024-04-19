package at.aau.serg.websocketserver.controller;

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
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static at.aau.serg.websocketserver.controller.helper.HelperMethods.getGameLobbyDtoList;
import static at.aau.serg.websocketserver.controller.helper.HelperMethods.getPlayerDtosInLobbyList;

@Controller
public class PlayerController {

    private final SimpMessagingTemplate template;
    private final PlayerEntityService playerEntityService;
    private final GameLobbyEntityService gameLobbyEntityService;
    private final ObjectMapper objectMapper;
    private final PlayerMapper playerMapper;
    private final GameLobbyMapper gameLobbyMapper;

    public PlayerController(SimpMessagingTemplate template, PlayerEntityService playerEntityService, GameLobbyEntityService gameLobbyEntityService, ObjectMapper objectMapper, PlayerMapper playerMapper, GameLobbyMapper gameLobbyMapper) {
        this.template = template;
        this.playerEntityService = playerEntityService;
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.objectMapper = objectMapper;
        this.playerMapper = playerMapper;
        this.gameLobbyMapper = gameLobbyMapper;
    }

    @MessageMapping("/player-create")
    @SendToUser("/queue/response")
    public String handleCreatePlayer(String playerDtoJson, @Header("simpSessionId") String sessionId, Message message) throws JsonProcessingException {

        // for testing only:
//        MessageHeaders messageHeader = message.getHeaders();

        // read in the JSON String and convert to PlayerDTO Object
        PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

        PlayerEntity createdPlayerEntity = playerEntityService.createPlayer(playerMapper.mapToEntity(playerDto));
        // return the DTO of the created player
        return objectMapper.writeValueAsString(playerMapper.mapToDto(createdPlayerEntity));
    }



    // TODO: test topic 4) response
    /**
     * Ideas for the endpoint: /app/player-join-lobby
     * <p>sends responses to:</p>
     * <p> 1) /user/queue/response --> updated playerDto (id of the joined lobby now set)</p>
     * <p> 2) /topic/lobby-list --> updated list of lobbies (numPlayers of the joined lobby incremented) (might be a lot of data to be sent when there are a lot of lobbies, but it's just Strings, so not really that much data when you think about it) </p>
     * <p> 3) /topic/lobby-$id --> updated list of players in lobby (response code: 201)</p>
     * <p> 4) /topic/lobby-$id/update --> updated gameLobbyDto (response code?)</p>
     * @param gameLobbyIdAndPlayerDtoJson String with id of the lobby to join and the playerDto, concatenated with |
     * @throws RuntimeException
     */
    @MessageMapping("/player-join-lobby")
    @SendToUser("/queue/response")
    public String handlePlayerJoinLobby(Message message, String gameLobbyIdAndPlayerDtoJson) throws RuntimeException {

        // testing only:
//        message.getHeaders();

        try {
            // 1) extract GameLobbyDto and PlayerDto objects from the string payload:
            String[] splitJsonStrings = gameLobbyIdAndPlayerDtoJson.split("\\|");

            Long gameLobbyId = Long.parseLong(splitJsonStrings[0]);
            String playerDtoJson = splitJsonStrings[1];

            PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);
            // 2) convert the DTOs to Entity Objects for Service:
            PlayerEntity playerEntity = playerMapper.mapToEntity(playerDto);

            // 3) player joins lobby:
            PlayerEntity updatedPlayerEntity = playerEntityService.joinLobby(gameLobbyId, playerEntity);

            PlayerDto dto = playerMapper.mapToDto(updatedPlayerEntity);

            // send response to /topic/lobby-$id --> updated list of players in lobby (later with response code: 201)
            List<PlayerDto> updatedPlayerEntitiesInLobby = getPlayerDtosInLobbyList(gameLobbyId, gameLobbyEntityService, playerEntityService, playerMapper);
            this.template.convertAndSend(
                    "/topic/lobby-"+gameLobbyId,
                    objectMapper.writeValueAsString(updatedPlayerEntitiesInLobby)
            );

            // send response to /topic/lobby-list --> updated list of lobbies (numPlayers of the joined lobby incremented) (later with response code: 301)
            List<GameLobbyDto> gameLobbyDtos = getGameLobbyDtoList(gameLobbyEntityService, gameLobbyMapper);
            this.template.convertAndSend(
                    "/topic/lobby-list",
                    objectMapper.writeValueAsString(gameLobbyDtos)
            );

            // send updated gameLobbyDto to all players in the lobby (relevant for lobbyCreator)
            this.template.convertAndSend(
                    "/topic/lobby-" + gameLobbyId + "/update",
                    objectMapper.writeValueAsString(gameLobbyEntityService.findById(gameLobbyId))
            );

            // send response to /user/queue/response --> updated playerDto (id of the joined lobby now set) (later with response code)
            return objectMapper.writeValueAsString(dto);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(ErrorCode.ERROR_2004.getErrorCode());
        } catch (NumberFormatException e) {
            throw new RuntimeException(ErrorCode.ERROR_1005.getErrorCode());
        }
    }


    // DONE
    /**
     * Ideas for the endpoint /app/player-list
     * <p>sends responses to: </p>
     * <p> 1) /user/queue/response --> list of players in the lobby</p>
     * <p>Relevant when first joining a lobby and getting the list of players!</p>
     * @param gameLobbyIdString
     * @return
     * @throws RuntimeException
     * @throws JsonProcessingException
     */
    @MessageMapping("/player-list")
    @SendToUser("/queue/player-list-response")
    public String getAllPlayersForLobby(String gameLobbyIdString) throws RuntimeException, JsonProcessingException {
        try {
            Long gameLobbyId = Long.parseLong(gameLobbyIdString);

            List<PlayerEntity> playerEntityList = playerEntityService.getAllPlayersForLobby(gameLobbyId);
            List<PlayerDto> playerDtoList = new ArrayList<>();

            for (PlayerEntity playerEntity : playerEntityList) {
                playerDtoList.add(playerMapper.mapToDto(playerEntity));
            }

            // later with response code
            return objectMapper.writeValueAsString(playerDtoList);
        } catch (NumberFormatException e) {
            throw new RuntimeException(ErrorCode.ERROR_1005.getErrorCode());
        }
    }

    // TODO: adapt
    /**
     * Ideas for the endpoint /app/player-update-username
     * <p>sends responses to: </p>
     * <p> 1) /user/queue/response --> updated playerDto (updated username) (response code: 101)</p>
     * <p> 2) /topic/lobby-$id --> updated list of players in lobby (response code: 201)</p>
     * @param playerDtoJson
     * @return
     * @throws JsonProcessingException
     */
    @MessageMapping("/player-update-username")
    @SendToUser("/queue/player-response")
    public String handlePlayerUpdateUsername(String playerDtoJson) throws JsonProcessingException {
        try{
            // convert the sent String content to the playerDto object we can work with:
            PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

            PlayerEntity playerEntity = playerMapper.mapToEntity(playerDto);

            PlayerEntity updatedPlayerEntity = playerEntityService.updateUsername(playerEntity);
            return objectMapper.writeValueAsString(playerMapper.mapToDto(updatedPlayerEntity));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(ErrorCode.ERROR_2004.getErrorCode());
        }

    }


    // DONE
    /**
     * Ideas for the endpoint /app/player-leave-lobby
     * <p>sends responses to:</p>
     * <p> 1) /user/queue/response --> updated playerDto (response code: 101)</p>
     * <p> 2) /topic/lobby-list --> updated list of lobbies (numPlayers of the left lobby decremented)
     * - (might be a lot of data to be sent when there are a lot of lobbies, but it's just Strings, so not really that much data when you think about it)
     * (response code: 301) </p>
     * <p> 3) /topic/lobby-$id --> updated list of players in lobby (response code: 201)</p>
     * <p> 4) /topic/lobby-$id/update --> updated gameLobbyDto (response code?)</p>
     * @param playerDtoJson playerDto that wants to leave the lobby he is currently in
     * @throws RuntimeException
     */
    @MessageMapping("/player-leave-lobby")
    @SendToUser("/queue/response")
    public String handlePlayerLeaveLobby(String playerDtoJson) throws RuntimeException {
        try {
            PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

            // 2) convert the DTO to Entity Object for Service:
            PlayerEntity playerEntity = playerMapper.mapToEntity(playerDto);

            Long gameLobbyId = playerDto.getGameLobbyId();

            // 3) player leaves lobby
            PlayerEntity updatedPlayerEntity = playerEntityService.leaveLobby(playerEntity);


            // send response to: /topic/lobby-list --> updated list of lobbies (later with response code 301)
            this.template.convertAndSend(
                    "/topic/lobby-list",
                    objectMapper.writeValueAsString(getGameLobbyDtoList(gameLobbyEntityService, gameLobbyMapper))
            );

            // send response to: /topic/lobby-$id --> updated list of players in lobby (later with response code: 201)
            this.template.convertAndSend(
                    "/topic/lobby-" + gameLobbyId,
                    objectMapper.writeValueAsString(getPlayerDtosInLobbyList(gameLobbyId, gameLobbyEntityService, playerEntityService, playerMapper))
            );

            // send updated gameLobbyDto to all players in the lobby (relevant for lobbyCreator)
            this.template.convertAndSend(
                    "/topic/lobby-" + gameLobbyId + "/update",
                    objectMapper.writeValueAsString(gameLobbyEntityService.findById(gameLobbyId))
            );

            // send response to: /user/queue/response --> updated playerDto (later with response code: 101)
            return objectMapper.writeValueAsString(playerMapper.mapToDto(updatedPlayerEntity));

        } catch (JsonProcessingException e) {
            throw new RuntimeException(ErrorCode.ERROR_2004.getErrorCode());
        }
    }

    // TODO: handle deletion of player inside a lobby properly?
    @MessageMapping("/player-delete")
    @SendToUser("/queue/player-response")
    public String handleDeletePlayer(String playerDtoJson) throws JsonProcessingException {
        PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

        playerEntityService.deletePlayer(playerDto.getId());
        Optional<PlayerEntity> shouldBeEmpty = playerEntityService.findPlayerById(playerDto.getId());
        if (shouldBeEmpty.isEmpty()) {
            return "DELETED";
        } else {
            throw new RuntimeException("player was not deleted");
        }
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        return "ERROR: " + exception.getMessage();
    }
}
