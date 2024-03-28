package at.aau.serg.websocketdemoserver.controller;

import at.aau.serg.websocketdemoserver.TestDataUtil;
import at.aau.serg.websocketdemoserver.demo.websocket.StompFrameHandlerClientImpl;
import at.aau.serg.websocketdemoserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketdemoserver.domain.dto.PlayerDto;
import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketdemoserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketdemoserver.mapper.PlayerMapper;
import at.aau.serg.websocketdemoserver.service.GameLobbyEntityService;
import at.aau.serg.websocketdemoserver.service.PlayerEntityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PlayerControllerIntegrationTest {
    private ObjectMapper objectMapper;

    private PlayerEntityService playerEntityService;
    private GameLobbyEntityService gameLobbyEntityService;
    private PlayerMapper playerMapper;
    private GameLobbyMapper gameLobbyMapper;

    @Autowired
    public PlayerControllerIntegrationTest(ObjectMapper objectMapper, PlayerEntityService playerEntityService, GameLobbyEntityService gameLobbyEntityService, PlayerMapper playerMapper, GameLobbyMapper gameLobbyMapper) {
        this.objectMapper = objectMapper;
        this.playerEntityService = playerEntityService;
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.playerMapper = playerMapper;
        this.gameLobbyMapper = gameLobbyMapper;
    }





    /////////start: von Demo-Projekt übernommen & leicht geändert
    @LocalServerPort
    private int port;
    private final String WEBSOCKET_URI = "ws://localhost:%d/websocket-broker";
    private final String WEBSOCKET_TOPIC = "/topic/websocket-broker-response";

    /**
     * Queue of messages from the server.
     */
    BlockingQueue<String> messages = new LinkedBlockingDeque<>();
    /////////end: von Demo-Projekt übernommen & leicht geändert


    @Test
    public void testThatCreatePlayerSuccessfullyReturnsCreatedPlayerDto() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/create-user-response";
        StompSession session = initStompSession();

        PlayerDto playerDto = TestDataUtil.createTestPlayerDtoA(null);

        // if we want to manually transform the PlayerDto object to a JSON-string:
        String playerDtoJson = objectMapper.writeValueAsString(playerDto);

        session.send("/app/create-user", playerDtoJson);

        var expectedResponse = "response from broker: " + playerDtoJson;

        assertThat(messages.poll(1, TimeUnit.SECONDS)).isEqualTo(expectedResponse);
    }

    @Test
    // Maybe use @SQL Annotation
    void testThatJoinLobbySuccessfullyReturnsUpdatedPlayerDto() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/player-join-lobby-response";
        StompSession session = initStompSession();

        // Pre-populate the database
        gameLobbyEntityService.createLobby(TestDataUtil.createTestGameLobbyEntityA());
        playerEntityService.createPlayer(TestDataUtil.createTestPlayerEntityA(null));

        PlayerDto testPlayerDtoA = TestDataUtil.createTestPlayerDtoA(null);
        GameLobbyDto testGameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();

        // manually transform objects to JSON-strings and combine them:
        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDtoA);
        String gameLobbyDtoJson = objectMapper.writeValueAsString(testGameLobbyDtoA);

        String payload = gameLobbyDtoJson + "|" +  playerDtoJson;

        session.send("/app/player-join-lobby", payload);

        // expected response: updated playerDto with the Lobby, which itself should also be updated to have incremented numPlayers
        testGameLobbyDtoA.setNumPlayers(testGameLobbyDtoA.getNumPlayers()+1);
        testPlayerDtoA.setGameLobbyDto(testGameLobbyDtoA);
        var expectedResponse = "response from broker: " + objectMapper.writeValueAsString(testPlayerDtoA);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    //@Test
    void testThatUpdatePlayerUsernameSuccessfullyReturnsUpdatedPlayerDto() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/player-update-username-response";
        StompSession session = initStompSession();

        // Populate the database with testPlayerEntityA
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(testPlayerEntityA);

        PlayerDto testPlayerDtoA = TestDataUtil.createTestPlayerDtoA(null);
        testPlayerDtoA.setUsername("UPDATED");

        String payload = testPlayerDtoA.getId() + "|" + objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-update-username", payload);

        var expectedResponse = "response from broker: " + objectMapper.writeValueAsString(testPlayerDtoA);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatLeaveLobbySuccessfullyReturnsUpdatedPlayerDto() throws Exception {
        StompSession session = initStompSession();

        // Populate the database with testPlayerEntityA who joins testGameLobbyEntityA:
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(testPlayerEntityA);
        // TODO: maybe don't use joinLobby() and instead set the data manually?
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        playerEntityService.joinLobby(testGameLobbyEntityA, testPlayerEntityA);

        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        GameLobbyDto testGameLobbyDtoA = gameLobbyMapper.mapToDto(testGameLobbyEntityA);

        String payload = objectMapper.writeValueAsString(testGameLobbyDtoA)
                + "|"
                + objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);

        testPlayerDtoA.setGameLobbyDto(null);
        var expectedResponse = "response from broker: " + objectMapper.writeValueAsString(testPlayerDtoA);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatDeletePlayerReturnsSuccessfulDeleteResponse() throws Exception {
        StompSession session = initStompSession();

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(testPlayerEntityA);

        session.send("/app/player-delete", testPlayerEntityA.getId()+"");

        var expectedResponse = "response from broker: player no longer exists in database";


    }

    /////////start: von Demo-Projekt übernommen
    /**
     * @return The Stomp session for the WebSocket connection (Stomp - WebSocket is comparable to HTTP - TCP).
     */
    public StompSession initStompSession() throws Exception {
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
        session.subscribe(WEBSOCKET_TOPIC, new StompFrameHandlerClientImpl(messages));

        return session;
    }
    /////////end: von Demo-Projekt übernommen

}
