package at.aau.serg.websocketserver.websocket;

import at.aau.serg.websocketserver.TestDataUtil;
import at.aau.serg.websocketserver.demo.websocket.StompFrameHandlerClientImpl;
import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketserver.domain.dto.PlayerDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.domain.pojo.PlayerColour;
import at.aau.serg.websocketserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketserver.mapper.PlayerMapper;
import at.aau.serg.websocketserver.service.GameLobbyEntityService;
import at.aau.serg.websocketserver.service.PlayerEntityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.messaging.support.MessageBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ClientDisconnectListenerIntegrationTest {

    PlayerEntityService playerEntityService;
    GameLobbyEntityService gameLobbyEntityService;
    PlayerMapper playerMapper;
    GameLobbyMapper gameLobbyMapper;
    ObjectMapper objectMapper;
    ApplicationEventPublisher eventPublisher;

    @LocalServerPort
    private int port;
    private final String WEBSOCKET_URI = "ws://localhost:%d/websocket-broker";
    BlockingQueue<String> messages;

    @Autowired
    public ClientDisconnectListenerIntegrationTest(PlayerEntityService playerEntityService, GameLobbyEntityService gameLobbyEntityService, PlayerMapper playerMapper, GameLobbyMapper gameLobbyMapper, ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
        this.playerEntityService = playerEntityService;
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.playerMapper = playerMapper;
        this.gameLobbyMapper = gameLobbyMapper;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @BeforeEach
    public void setUp() {
        messages = new LinkedBlockingDeque<>();
    }

    @AfterEach
    public void tearDown() {
        messages = null;
    }

    @Test
    void testThatDisconnectWithoutLobbySuccessfullyDeletesExistingPlayer() throws Exception {
        StompSession session = initStompSession();

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(testPlayerEntityA, session.getSessionId());

        // assert that player currently exists in database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);

        // Simulate a session disconnect event and publish it
        SessionDisconnectEvent event = createSessionDisconnectEvent(this,session.getSessionId());
        eventPublisher.publishEvent(event);

        // assert that player no longer exists in database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
    }

    @Test
    void testThatDisconnectWithPlayerInLobbySuccessfullyRemovesPlayerFromLobbyAndDeletesExistingPlayer() throws Exception {
        GameLobbyEntity gameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        StompSession session = initStompSession();
        session.subscribe("/topic/lobby-" + gameLobbyEntityA.getId(), new StompFrameHandlerClientImpl(messages));

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);

        GameLobbyDto gameLobbyDtoA = gameLobbyMapper.mapToDto(gameLobbyEntityA);
        gameLobbyDtoA.setNumPlayers(2);

        PlayerDto playerDtoB = playerMapper.mapToDto(testPlayerEntityB);
        playerDtoB.setGameLobbyId(gameLobbyEntityA.getId());
        List<PlayerDto> playerDtoList = new ArrayList<>();

        playerEntityService.createPlayer(testPlayerEntityA, session.getSessionId());
        playerEntityService.createPlayer(testPlayerEntityB);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId()).get()).isEqualTo(testPlayerEntityB);

        gameLobbyEntityService.createLobby(gameLobbyEntityA);
        assertThat(gameLobbyEntityService.findById(gameLobbyEntityA.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), testPlayerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), testPlayerEntityB);

        // Simulate a session disconnect event and publish it
        SessionDisconnectEvent event = createSessionDisconnectEvent(this,session.getSessionId());
        eventPublisher.publishEvent(event);

        // Randomness-Workaround
        playerDtoB.setPlayerColour(PlayerColour.valueOf(playerEntityService.findPlayerById(testPlayerEntityB.getId()).get().getPlayerColour()));
        playerDtoList.add(playerDtoB);

        String expectedResponse = objectMapper.writeValueAsString(playerDtoList);
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(gameLobbyEntityService.findById(gameLobbyEntityA.getId())).isPresent();

        List<PlayerEntity> playerEntityList = playerEntityService.getAllPlayersForLobby(gameLobbyEntityA.getId());
        gameLobbyDtoA.setNumPlayers(gameLobbyDtoA.getNumPlayers() - 1);

        assertThat(playerEntityList.size()).isEqualTo(gameLobbyDtoA.getNumPlayers());
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatDisconnectWithPlayerOnlyPlayerInLobbySuccessfullyDeletesLobbyAndPlayer() throws Exception {
        GameLobbyEntity gameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        GameLobbyEntity gameLobbyEntityB = TestDataUtil.createTestGameLobbyEntityB();
        GameLobbyDto gameLobbyDtoA = gameLobbyMapper.mapToDto(gameLobbyEntityA);
        GameLobbyDto gameLobbyDtoB = gameLobbyMapper.mapToDto(gameLobbyEntityB);
        gameLobbyDtoA.setNumPlayers(2);

        List<GameLobbyDto> gameLobbyDtoList = new ArrayList<>();
        gameLobbyDtoList.add(gameLobbyDtoB);

        StompSession session = initStompSession();
        session.subscribe("/topic/lobby-list", new StompFrameHandlerClientImpl(messages));

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);

        playerEntityService.createPlayer(testPlayerEntityA, session.getSessionId());
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);

        gameLobbyEntityService.createLobby(gameLobbyEntityA);
        gameLobbyEntityService.createLobby(gameLobbyEntityB);
        assertThat(gameLobbyEntityService.findById(gameLobbyEntityA.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), testPlayerEntityA);

        // Simulate a session disconnect event and publish it
        SessionDisconnectEvent event = createSessionDisconnectEvent(this,session.getSessionId());
        eventPublisher.publishEvent(event);

        String expectedResponse = objectMapper.writeValueAsString(gameLobbyDtoList);
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(gameLobbyEntityService.findById(gameLobbyEntityA.getId())).isEmpty();
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    private SessionDisconnectEvent createSessionDisconnectEvent(Object source, String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId(sessionId);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(headerAccessor)
                .build();
        return new SessionDisconnectEvent(source, message, sessionId, CloseStatus.NORMAL);
    }

    public StompSession initStompSession() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());

        StompSession session = stompClient.connectAsync(String.format(WEBSOCKET_URI, port),
                        new StompSessionHandlerAdapter() {
                        })
                .get(1, TimeUnit.SECONDS);

        return session;
    }
}
