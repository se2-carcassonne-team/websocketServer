package at.aau.serg.websocketdemoserver.controller;

import at.aau.serg.websocketdemoserver.demo.websocket.StompFrameHandlerClientImpl;
import at.aau.serg.websocketdemoserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketdemoserver.service.GameLobbyEntityService;
import at.aau.serg.websocketdemoserver.service.PlayerEntityService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private GameLobbyMapper gameLobbyMapper;

    @Autowired
    public GameLobbyControllerIntegrationTest(ObjectMapper objectMapper, GameLobbyEntityService gameLobbyEntityService, PlayerEntityService playerEntityService, GameLobbyMapper gameLobbyMapper) {
        this.objectMapper = objectMapper;
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.playerEntityService = playerEntityService;
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

// MOVED TO PLAYERCONTROLLERINTEGRATIONTEST!!!
//    @Test
//    public void testThatJoinLobbySuccessfullyReturnsUpdatedPlayerDto() throws Exception {
//        StompSession session = initStompSession();
//
//        PlayerDto testPlayerDtoA = TestDataUtil.createTestPlayerDtoA(null);
//
//        GameLobbyDto testGameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
//
//
//        // TODO: Fill Database
//        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
//        gameLobbyEntityService.createLobby(testGameLobbyEntityA);
//        playerEntityService.createPlayer(TestDataUtil.createTestPlayerEntityA(testGameLobbyEntityA));
//
//        // TODO: Figure out how to send multiple objects in one payload without causing too much work
//
//        // manually transform objects to JSON-strings:
//        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDtoA);
//        String gameLobbyDtoJson = objectMapper.writeValueAsString(testGameLobbyDtoA);
//
//        String payload = gameLobbyDtoJson + "|" +  playerDtoJson;
//
//        // Old Controller signature: String handleLobbyJoin(String gameLobbyDtoJson, String playerDtoJson)
//
//        session.send("/app/join-lobby", payload);
//
////        ArrayList<PlayerDto> playerDtosInLobby = new ArrayList<>();
////        playerDtosInLobby.add(testPlayerDtoA);
////        testGameLobbyDtoA.setPlayers(playerDtosInLobby);
//
//        String expectedResultJson = objectMapper.writeValueAsString(testGameLobbyDtoA);
//
//        var expectedResponse = "echo from broker: " + expectedResultJson;
//
//        assertThat(messages.poll(1, TimeUnit.SECONDS)).isEqualTo(expectedResponse);
//    }




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
