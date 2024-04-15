package at.aau.serg.websocketserver.controller;

import at.aau.serg.websocketserver.domain.dto.PlayerDto;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.statuscode.ErrorCode;
import at.aau.serg.websocketserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketserver.mapper.PlayerMapper;
import at.aau.serg.websocketserver.service.GameLobbyEntityService;
import at.aau.serg.websocketserver.service.PlayerEntityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class PlayerController {

    private SimpMessagingTemplate template;
    private PlayerEntityService playerEntityService;
    private GameLobbyEntityService gameLobbyEntityService;
    private ObjectMapper objectMapper;
    private PlayerMapper playerMapper;
    private GameLobbyMapper gameLobbyMapper;

    public PlayerController(SimpMessagingTemplate template, PlayerEntityService playerEntityService, GameLobbyEntityService gameLobbyEntityService, ObjectMapper objectMapper, PlayerMapper playerMapper, GameLobbyMapper gameLobbyMapper) {
        this.template = template;
        this.playerEntityService = playerEntityService;
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.objectMapper = objectMapper;
        this.playerMapper = playerMapper;
        this.gameLobbyMapper = gameLobbyMapper;
    }

    // test value
    @MessageMapping("/player-create")
    @SendToUser("/queue/player-response")
    public String handleCreatePlayer(String playerDtoJson) throws JsonProcessingException {
        // read in the JSON String and convert to PlayerDTO Object
        PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

        PlayerEntity createdPlayerEntity = playerEntityService.createPlayer(playerMapper.mapToEntity(playerDto));
        // return the DTO of the created player
        return objectMapper.writeValueAsString(playerMapper.mapToDto(createdPlayerEntity));
    }


    /**
     * Ideas for the endpoint: /app/player-join-lobby
     * <p>sends responses to:</p>
     * <p> 1) /user/queue/player-response --> updated playerDto (id of the joined lobby now set)</p>
     * <p> 2) /topic/lobby-list --> updated list of lobbies (numPlayers of the joined lobby incremented) (might be a lot of data to be sent when there are a lot of lobbies, but it's just Strings, so not really that much data when you think about it) </p>
     * <p> 3) /topic/lobby-$id --> updated list of players in lobby (response code: 201)</p>
     * @param gameLobbyIdAndPlayerDtoJson String with id of the lobby to join and the playerDto, concatenated with |
     * @throws RuntimeException
     */
    @MessageMapping("/player-join-lobby")
    //@SendTo("/topic/player-join-response")
    public void handlePlayerJoinLobby(String gameLobbyIdAndPlayerDtoJson) throws RuntimeException {

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

            // return the dto equivalent of the updated player entity
            this.template.convertAndSend("/topic/player-join-lobby-"+gameLobbyId, objectMapper.writeValueAsString(dto));

        } catch (JsonProcessingException e) {
            throw new RuntimeException(ErrorCode.ERROR_2004.getErrorCode());
        } catch (NumberFormatException e) {
            throw new RuntimeException(ErrorCode.ERROR_1005.getErrorCode());
        }
        //return objectMapper.writeValueAsString(dto);
    }

    @MessageMapping("/player-list")
    @SendToUser("/topic/player-response")
    public String getAllPlayersForLobby(String gameLobbyIdString) throws RuntimeException, JsonProcessingException {
        try {
            Long gameLobbyId = Long.parseLong(gameLobbyIdString);

            List<PlayerEntity> playerEntityList = playerEntityService.getAllPlayersForLobby(gameLobbyId);
            List<PlayerDto> playerDtoList = new ArrayList<>();

            for (PlayerEntity playerEntity : playerEntityList) {
                playerDtoList.add(playerMapper.mapToDto(playerEntity));
            }

            return objectMapper.writeValueAsString(playerDtoList);
        } catch (NumberFormatException e) {
            throw new RuntimeException(ErrorCode.ERROR_1005.getErrorCode());
        }
    }

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

    /**
     * Ideas for the endpoint /app/player-leave-lobby
     * <p>sends responses to:</p>
     * <p> 1) /user/queue/response --> updated playerDto (response code: 101)</p>
     * <p> 2) /topic/lobby-list --> updated list of lobbies (numPlayers of the left lobby decremented)
     * - (might be a lot of data to be sent when there are a lot of lobbies, but it's just Strings, so not really that much data when you think about it)
     * (response code: 301) </p>
     * <p> 3) /topic/lobby-$id --> updated list of players in lobby (response code: 201)</p>
     * @param playerDtoJson playerDto that wants to leave the lobby he is currently in
     * @throws RuntimeException
     */
    @MessageMapping("/player-leave-lobby")
    //@SendTo("/topic/player-leave-response")
    public void handlePlayerLeaveLobby(String playerDtoJson) throws RuntimeException {
        try {
            PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

            // 2) convert the DTO to Entity Object for Service:
            PlayerEntity playerEntity = playerMapper.mapToEntity(playerDto);

            Long gameLobbyId = playerDto.getGameLobbyId();

            // 3) player leaves lobby
            PlayerEntity updatedPlayerEntity = playerEntityService.leaveLobby(playerEntity);

            this.template.convertAndSend(
                    "/topic/player-leave-lobby-"+gameLobbyId,
                    objectMapper.writeValueAsString(playerMapper.mapToDto(updatedPlayerEntity))
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(ErrorCode.ERROR_2004.getErrorCode());
        }


        //return objectMapper.writeValueAsString(playerMapper.mapToDto(updatedPlayerEntity));
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
