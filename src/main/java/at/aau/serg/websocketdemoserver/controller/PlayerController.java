package at.aau.serg.websocketdemoserver.controller;

import at.aau.serg.websocketdemoserver.domain.dto.PlayerDto;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketdemoserver.mapper.PlayerMapper;
import at.aau.serg.websocketdemoserver.service.PlayerEntityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

@Controller
public class PlayerController {

    private PlayerEntityService playerEntityService;
    private ObjectMapper objectMapper;
    private PlayerMapper playerMapper;

    public PlayerController(PlayerEntityService playerEntityService, ObjectMapper objectMapper, PlayerMapper playerMapper) {
        this.playerEntityService = playerEntityService;
        this.objectMapper = objectMapper;
        this.playerMapper = playerMapper;
    }

    // test value
    @MessageMapping("/create-user")
    @SendTo("/topic/create-user-response")
    public String handleCreateUser(String playerDtoJson) throws JsonProcessingException {
        // TODO handle the messages here
        // read in the JSON String and convert to PlayerDTO Object
        PlayerDto playerDto = objectMapper.readValue(playerDtoJson, PlayerDto.class);

        PlayerEntity playerEntity = playerEntityService.createPlayer(playerMapper.mapToEntity(playerDto));

        //return "echo from broker: " + HtmlUtils.htmlEscape(playerDto);
        return "echo from broker: " + objectMapper.writeValueAsString(playerDto);
    }

}
