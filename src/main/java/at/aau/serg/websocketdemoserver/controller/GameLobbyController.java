package at.aau.serg.websocketdemoserver.controller;

import at.aau.serg.websocketdemoserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketdemoserver.domain.dto.PlayerDto;
import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.service.GameLobbyEntityService;
import at.aau.serg.websocketdemoserver.service.PlayerEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

@Controller
public class GameLobbyController {

    /* Autowired explained:

    Autowired automatically handles the injection of an object into our controller class

    Example:

    @Autowired:
    Service service;

    Without this annotation you would have to manually instantiate the class the old fashioned way:

    Service service = new Service();
    */

    @Autowired
    GameLobbyEntityService gameLobbyService;
    @Autowired
    PlayerEntityService playerService;

    public GameLobbyController(GameLobbyEntityService gameLobbyService, PlayerEntityService playerService) {
        this.gameLobbyService = gameLobbyService;
        this.playerService = playerService;
    }

    @MessageMapping("/join-lobby")
    @SendTo("/topic/join-lobby")
    public GameLobbyDto handleLobbyJoin(GameLobbyDto lobby, PlayerDto player) {
        GameLobbyDto dto = playerService.joinLobby(lobby, player);
    }
}
