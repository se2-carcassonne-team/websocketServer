package at.aau.serg.websocketserver.controller;

import at.aau.serg.websocketserver.domain.dto.FinishedTurnDto;
import at.aau.serg.websocketserver.service.CheatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class CheatController {

    private final SimpMessagingTemplate template;
    private final ObjectMapper objectMapper;
    CheatService cheatService;

    public CheatController(SimpMessagingTemplate template, ObjectMapper objectMapper, CheatService cheatService) {
        this.template = template;
        this.objectMapper = objectMapper;
        this.cheatService = cheatService;
    }

    @MessageMapping("/cheat/add-points")
    @SendToUser("/queue/cheat-points")
    public String addPointsForPlayer(String playerIdStringAndFinishedTurnDtoString) throws JsonProcessingException {
        String[] splitJsonStrings = playerIdStringAndFinishedTurnDtoString.split("\\|");

        Long playerId = Long.parseLong(splitJsonStrings[0]);
        FinishedTurnDto finishedTurnDto = objectMapper.readValue(splitJsonStrings[1], FinishedTurnDto.class);

        int cheatPoints = cheatService.generateCheatPoints();
        cheatService.updatePlayerPoints(playerId, finishedTurnDto, cheatPoints);

        // Notify all other player that points changed
        this.template.convertAndSend("/topic/game-session-" + finishedTurnDto.getGameSessionId() + "/points-meeples", objectMapper.writeValueAsString(finishedTurnDto));

        // Notify cheating player how much points were added
        return objectMapper.writeValueAsString(cheatPoints);
    }

    @MessageMapping("/cheat/accuse")
    @SendToUser("/queue/cheat-accusation-result")
    public String handleCheatAccusation(String playerIdStringAndAccusedPlayerIdStringAndFinishedTurnDtoString) throws JsonProcessingException {
        String[] splitJsonStrings = playerIdStringAndAccusedPlayerIdStringAndFinishedTurnDtoString.split("\\|");

        Long playerId = Long.parseLong(splitJsonStrings[0]);
        Long accusedPlayerId = Long.parseLong(splitJsonStrings[1]);
        FinishedTurnDto finishedTurnDto = objectMapper.readValue(splitJsonStrings[2], FinishedTurnDto.class);

        Boolean accusedPlayerCheated = cheatService.checkIsPlayerCheater(accusedPlayerId);

        if(accusedPlayerCheated) {
            Integer cheatPoints = cheatService.getCheatPoints(accusedPlayerId);
            Integer penaltyPoints = cheatService.generatePenaltyPoints(cheatPoints);
            cheatService.updatePlayerPoints(playerId, finishedTurnDto, penaltyPoints);

            // Notify all other player that points changed
            this.template.convertAndSend("/topic/game-session-" + finishedTurnDto.getGameSessionId() + "/points-meeples", objectMapper.writeValueAsString(finishedTurnDto));

            // Notify cheater that he got penalized
            this.template.convertAndSend("/topic/game-session-" + finishedTurnDto.getGameSessionId() + "/player-" + accusedPlayerId + "/cheat-detected", objectMapper.writeValueAsString(penaltyPoints));
        }

        // TODO: Penalize player who accused in case it was a wrong accusation

        return objectMapper.writeValueAsString(accusedPlayerCheated);
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        return "ERROR: " + exception.getMessage();
    }

}
