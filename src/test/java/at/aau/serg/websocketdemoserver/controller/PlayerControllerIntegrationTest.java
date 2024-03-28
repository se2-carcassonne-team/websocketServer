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

import static org.assertj.core.api.Assertions.as;
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
    public void testThatCreatePlayerHandlerSuccessfullyCreatesPlayer() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/create-user-response";
        StompSession session = initStompSession();

        PlayerDto testPlayerDto = TestDataUtil.createTestPlayerDtoA(null);
        PlayerEntity testPlayerEntity = playerMapper.mapToEntity(testPlayerDto);

        // assert that the test player doesn't exist in the database yet
        assertThat(playerEntityService.findPlayerById(testPlayerEntity.getId())).isEmpty();

        // manually transform the PlayerDto object to a JSON-string:
        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDto);

        session.send("/app/create-user", playerDtoJson);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // assert that the player now exists in the database
        assertThat(playerEntityService.findPlayerById(testPlayerEntity.getId())).isPresent();
        assertThat(playerEntityService.findPlayerById(testPlayerEntity.getId()).get()).isEqualTo(testPlayerEntity);

        // assert that the controller response is as we expect. The controller should return the DTO of the created player
        assertThat(actualResponse).isEqualTo(playerDtoJson);
    }

    @Test
    // Maybe use @SQL Annotation
    void testThatJoinLobbySuccessfullyMakesPlayerJoinLobby() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/player-join-lobby-response";
        StompSession session = initStompSession();

        // Pre-populate the database
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        gameLobbyEntityService.createLobby(testGameLobbyEntityA);
        playerEntityService.createPlayer(testPlayerEntityA);

        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        GameLobbyDto testGameLobbyDtoA = gameLobbyMapper.mapToDto(testGameLobbyEntityA);

        // manually transform objects to JSON-strings and combine them:
        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDtoA);
        String gameLobbyDtoJson = objectMapper.writeValueAsString(testGameLobbyDtoA);

        String payload = gameLobbyDtoJson + "|" +  playerDtoJson;

        // before sending payload:
        // 1) assert that the test player doesn't reference a game lobby
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(null);
        // 2) assert the numPlayers of the test game lobby is 0
        assertThat(gameLobbyEntityService.findById(testPlayerEntityA.getId()).get().getNumPlayers()).isEqualTo(0);

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after sending & controller processing payload:
        // 1) assert that the test game lobby now has numPlayers = 1
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(1);
        // 2) assert that the test player references the test game lobby
        testGameLobbyEntityA.setNumPlayers(testGameLobbyEntityA.getNumPlayers()+1);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(testGameLobbyEntityA);


        // expected response: updated playerDto with the Lobby, which itself should also be updated to have incremented numPlayers
        testGameLobbyDtoA.setNumPlayers(testGameLobbyDtoA.getNumPlayers()+1);
        testPlayerDtoA.setGameLobbyDto(testGameLobbyDtoA);
        var expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatUpdatePlayerUsernameSuccessfullyReturnsUpdatedPlayerDto() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/player-update-username-response";
        StompSession session = initStompSession();

        // Populate the database with testPlayerEntityA
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(testPlayerEntityA);


        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        testPlayerDtoA.setUsername("UPDATED");

        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        // before controller method is called:
        // assert that the player to update exists in the database and has the original name
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getUsername()).isEqualTo(testPlayerEntityA.getUsername());

        session.send("/app/player-update-username", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after controller method is called:
        // assert that the player in the database has the updated username and that nothing else changed
        testPlayerEntityA.setUsername("UPDATED");
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);

        // expected response: the dto of the updated player
        var expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatLeaveLobbySuccessfullyRemovesPlayerFromGameLobby() throws Exception {
        StompSession session = initStompSession();

        // Populate the database with testPlayerEntityA who joins testGameLobbyEntityA:
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        testPlayerEntityA.setGameLobbyEntity(testGameLobbyEntityA);
        testGameLobbyEntityA.setNumPlayers(1);
        playerEntityService.createPlayer(testPlayerEntityA);
        // the referenced game lobby should automatically be created as well due to cascading (see entity definition)

        // before controller method call:
        // assert that the player and the game lobby (that player is in) exist in the database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get()).isEqualTo(testGameLobbyEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        GameLobbyDto testGameLobbyDtoA = gameLobbyMapper.mapToDto(testGameLobbyEntityA);
        String payload = objectMapper.writeValueAsString(testGameLobbyDtoA)
                + "|"
                + objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);


        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after controller method call:
        // assert that the player and game lobby entities in the database have updated as expected
        testGameLobbyEntityA.setNumPlayers(0);
        assertThat(gameLobbyEntityService.findById(testPlayerDtoA.getId()).get()).isEqualTo(testGameLobbyEntityA);
        testPlayerEntityA.setGameLobbyEntity(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);

        testPlayerDtoA.setGameLobbyDto(null);
        var expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatDeletePlayerSuccessfullyDeletesPlayer() throws Exception {
        StompSession session = initStompSession();

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(testPlayerEntityA);

        // assert that player currently exists in database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);

        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        session.send("/app/player-delete", objectMapper.writeValueAsString(testPlayerDtoA));

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // assert that player no longer exists in database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();

        var expectedResponse = "player no longer exists in database";
        assertThat(actualResponse).isEqualTo(expectedResponse);
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
