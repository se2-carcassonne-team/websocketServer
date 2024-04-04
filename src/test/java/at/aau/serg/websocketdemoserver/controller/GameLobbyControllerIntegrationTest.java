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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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

    @LocalServerPort
    private int port;
    private final String WEBSOCKET_URI = "ws://localhost:%d/websocket-broker";
    BlockingQueue<String> messages;

    @BeforeEach
    public void setUp() {
        messages = new LinkedBlockingDeque<>();
    }

    @AfterEach
    public void tearDown() {
        messages = null;
    }

    @Test
    void testCreateLobbyReturnsCreatedGameLobbyDto() throws Exception {
        StompSession session = initStompSession("/user/queue/lobby-response", messages);

        PlayerEntity playerEntity = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(playerEntity);

        PlayerDto playerDto = playerMapper.mapToDto(playerEntity);
        GameLobbyDto gameLobbyDto = TestDataUtil.createTestGameLobbyDtoA();

        String playerDtoJson = objectMapper.writeValueAsString(playerDto);
        String gameLobbyDtoJson = objectMapper.writeValueAsString(gameLobbyDto);

        String payload = gameLobbyDtoJson + "|" + playerDtoJson;

        assertThat(gameLobbyEntityService.findById(gameLobbyDto.getId())).isEqualTo(Optional.empty());
        assertThat(playerEntityService.findPlayerById(playerDto.getId()).get().getGameLobbyEntity()).isEqualTo(null);
        session.send("/app/lobby-create", payload);

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

    @Test
    void testUpdateLobbyNameReturnsUpdatedGameLobbyDto() throws Exception {
        StompSession session = initStompSession("/topic/game-lobby-response", messages);

        GameLobbyDto gameLobbyDto = TestDataUtil.createTestGameLobbyDtoA();
        gameLobbyEntityService.createLobby(gameLobbyMapper.mapToEntity(gameLobbyDto));

        gameLobbyDto.setName("lobbyB");
        session.send("/app/lobby-name-update", objectMapper.writeValueAsString(gameLobbyDto));

        String expectedResponse = objectMapper.writeValueAsString(gameLobbyDto);
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        Optional<GameLobbyEntity> updatedGameLobbyEntityOptional = gameLobbyEntityService.findById(gameLobbyDto.getId());
        assertTrue(updatedGameLobbyEntityOptional.isPresent());

        GameLobbyEntity updatedGameLobbyEntity = updatedGameLobbyEntityOptional.get();
        assertThat(updatedGameLobbyEntity.getName()).isEqualTo(gameLobbyMapper.mapToEntity(gameLobbyDto).getName());

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testUpdateLobbyNameReturnsErrorResponse() throws Exception {
        StompSession session = initStompSessionWithErrorTopic("/topic/game-lobby-response", "/user/queue/errors", messages);

        GameLobbyDto gameLobbyDto = TestDataUtil.createTestGameLobbyDtoA();
        gameLobbyEntityService.createLobby(gameLobbyMapper.mapToEntity(gameLobbyDto));

        gameLobbyDto.setId(10L);
        gameLobbyDto.setName("lobbyB");
        session.send("/app/lobby-name-update", objectMapper.writeValueAsString(gameLobbyDto));

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        assertThat(actualResponse).startsWith("ERROR");
    }

    @Test
    void getListOfLobbiesReturnsListOfGameLobbyDtos() throws Exception {
        StompSession session = initStompSession("/user/queue/lobby-response", messages);

        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        GameLobbyDto gameLobbyDtoB = TestDataUtil.createTestGameLobbyDtoB();

        gameLobbyEntityService.createLobby(gameLobbyMapper.mapToEntity(gameLobbyDtoA));
        gameLobbyEntityService.createLobby(gameLobbyMapper.mapToEntity(gameLobbyDtoB));

        List<GameLobbyDto> gameLobbyDtoList = new ArrayList<>();
        gameLobbyDtoList.add(gameLobbyDtoA);
        gameLobbyDtoList.add(gameLobbyDtoB);

        session.send("/app/lobby-list", "");
        String expectedResponse = objectMapper.writeValueAsString(gameLobbyDtoList);
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void getListOfLobbiesReturnsEmptyListOfGameLobbyDtos() throws Exception {
        StompSession session = initStompSession("/user/queue/lobby-response", messages);

        List<GameLobbyDto> gameLobbyDtoList = new ArrayList<>();

        session.send("/app/lobby-list", "");
        String expectedResponse = objectMapper.writeValueAsString(gameLobbyDtoList);
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void deleteLobbyReturnsSuccessMessage() throws Exception {
        StompSession session = initStompSession("/user/queue/lobby-response", messages);

        GameLobbyDto gameLobbyDto = TestDataUtil.createTestGameLobbyDtoA();

        gameLobbyEntityService.createLobby(gameLobbyMapper.mapToEntity(gameLobbyDto));
        assertThat(gameLobbyEntityService.findById(gameLobbyDto.getId()).isPresent()).isTrue();

        session.send("/app/lobby-delete", objectMapper.writeValueAsString(gameLobbyDto));

        String expectedResponse = "deleted";
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(gameLobbyEntityService.findById(gameLobbyDto.getId()).isPresent()).isFalse();
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    public StompSession initStompSession(String topic, BlockingQueue<String> messages) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());

        StompSession session = stompClient.connectAsync(String.format(WEBSOCKET_URI, port),
                        new StompSessionHandlerAdapter() {
                        })
                .get(1, TimeUnit.SECONDS);

        // subscribes to the topic defined in WebSocketBrokerController
        // and adds received messages to WebSocketBrokerIntegrationTest#messages
        session.subscribe(topic, new StompFrameHandlerClientImpl(messages));

        return session;
    }

    public StompSession initStompSessionWithErrorTopic(String topic, String errorTopic, BlockingQueue<String> messages) throws Exception {
        StompSession session = initStompSession(topic, messages);
        session.subscribe(errorTopic, new StompFrameHandlerClientImpl(messages));

        return session;
    }

}
