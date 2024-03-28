package at.aau.serg.websocketdemoserver.controller;

import at.aau.serg.websocketdemoserver.TestDataUtil;
import at.aau.serg.websocketdemoserver.demo.websocket.StompFrameHandlerClientImpl;
import at.aau.serg.websocketdemoserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketdemoserver.domain.dto.PlayerDto;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketdemoserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketdemoserver.mapper.PlayerMapper;
import at.aau.serg.websocketdemoserver.service.GameLobbyEntityService;
import at.aau.serg.websocketdemoserver.service.PlayerEntityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GameLobbyControllerIntegrationTest {

    private ObjectMapper objectMapper;

    private GameLobbyEntityService gameLobbyEntityService;
    private PlayerEntityService playerEntityService;
    private PlayerMapper playerMapper;
    private GameLobbyMapper gameLobbyMapper;

    @Autowired
    public GameLobbyControllerIntegrationTest(ObjectMapper objectMapper, GameLobbyEntityService gameLobbyEntityService, PlayerEntityService playerEntityService, PlayerMapper playerMapper, GameLobbyMapper gameLobbyMapper) {
        this.objectMapper = objectMapper;
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.playerEntityService = playerEntityService;
        this.playerMapper = playerMapper;
        this.gameLobbyMapper = gameLobbyMapper;
    }

    /////////start: von Demo-Projekt übernommen & leicht geändert
    @LocalServerPort
    private int port;
    private final String WEBSOCKET_URI = "ws://localhost:%d/websocket-broker";
    private final String WEBSOCKET_TOPIC = "/topic/join-lobby-response";

    /**
     * Queue of messages from the server.
     */
    BlockingQueue<String> messages = new LinkedBlockingDeque<>();
    /////////end: von Demo-Projekt übernommen & leicht geändert


    @Test
    void testCreateLobbyReturnsCreatedGameLobbyDto() throws Exception {
        StompSession session = initStompSession("/topic/create-lobby-response");

        PlayerEntity playerEntity = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(playerEntity);

        PlayerDto playerDto = playerMapper.mapToDto(playerEntity);
        GameLobbyDto gameLobbyDto = TestDataUtil.createTestGameLobbyDtoA();

        String playerDtoJson = objectMapper.writeValueAsString(playerDto);
        String gameLobbyDtoJson = objectMapper.writeValueAsString(gameLobbyDto);

        String payload = gameLobbyDtoJson + "|" +  playerDtoJson;

        assertThat(gameLobbyEntityService.findById(gameLobbyDto.getId())).isEqualTo(Optional.empty());
        assertThat(playerEntityService.findPlayerById(playerDto.getId()).get().getGameLobbyEntity()).isEqualTo(null);
        session.send("/app/create-lobby", payload);

        gameLobbyDto.setNumPlayers(gameLobbyDto.getNumPlayers() + 1);
        playerDto.setGameLobbyDto(gameLobbyDto);

        String expectedResponse = objectMapper.writeValueAsString(gameLobbyDto);
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        Optional<PlayerEntity> updatedPlayerEntityOptional = playerEntityService.findPlayerById(playerDto.getId());
        assertTrue(updatedPlayerEntityOptional.isPresent());
        PlayerEntity updatedPlayerEntity = updatedPlayerEntityOptional.get();
        assertThat(updatedPlayerEntity.getGameLobbyEntity()).isEqualTo(gameLobbyMapper.mapToEntity(gameLobbyDto));

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }





    /////////start: von Demo-Projekt übernommen
    /**
     * @return The Stomp session for the WebSocket connection (Stomp - WebSocket is comparable to HTTP - TCP).
     */
    public StompSession initStompSession(String topic) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());

        // connect client to the websocket server
        StompSession session = stompClient.connectAsync(String.format(WEBSOCKET_URI, port),
                        new StompSessionHandlerAdapter() {
                        })
                // wait 1 sec for the client to be connected
                .get(1, TimeUnit.SECONDS);

        // subscribes to the topic defined in WebSocketBrokerController
        // and adds received messages to WebSocketBrokerIntegrationTest#messages
        session.subscribe(topic, new StompFrameHandlerClientImpl(messages));

        return session;
    }
    /////////end: von Demo-Projekt übernommen

}
