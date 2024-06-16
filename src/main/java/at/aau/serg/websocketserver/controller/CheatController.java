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

        // Notify cheating player how much points were added for him
        return objectMapper.writeValueAsString(cheatPoints);
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        return "ERROR: " + exception.getMessage();
    }

}
