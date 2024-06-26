package at.aau.serg.websocketserver.controller;

import at.aau.serg.websocketserver.TestDataUtil;
import at.aau.serg.websocketserver.demo.websocket.StompFrameHandlerClientImpl;
import at.aau.serg.websocketserver.domain.dto.FinishedTurnDto;
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.domain.entity.repository.GameSessionEntityRepository;
import at.aau.serg.websocketserver.service.GameSessionEntityService;
import at.aau.serg.websocketserver.service.PlayerEntityService;
import at.aau.serg.websocketserver.statuscode.ErrorCode;
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CheatControllerIntegrationTest {

    private final GameSessionEntityRepository gameSessionEntityRepository;
    private final GameSessionEntityService gameSessionEntityService;
    private final PlayerEntityService playerEntityService;
    private final ObjectMapper objectMapper;

    @Autowired
    public CheatControllerIntegrationTest(GameSessionEntityRepository gameSessionEntityRepository, GameSessionEntityService gameSessionEntityService, PlayerEntityService playerEntityService, ObjectMapper objectMapper) {
        this.gameSessionEntityRepository = gameSessionEntityRepository;
        this.gameSessionEntityService = gameSessionEntityService;
        this.playerEntityService = playerEntityService;
        this.objectMapper = objectMapper;
    }

    @LocalServerPort
    private int port;
    private final String WEBSOCKET_URI = "ws://localhost:%d/websocket-broker";
    BlockingQueue<String> messages;
    BlockingQueue<String> messages2;

    @BeforeEach
    public void setUp() {
        messages = new LinkedBlockingDeque<>();
        messages2 = new LinkedBlockingDeque<>();
    }

    @AfterEach
    public void tearDown() {
        messages = null;
        messages2 = null;
    }

    @Test
    void testThatAddPointsForPlayerReturnsUpdatedPointsToTopic() throws Exception {
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        testPlayerEntityA.setCanCheat(true);

        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);

        playerEntityService.createPlayer(testPlayerEntityA);
        playerEntityService.createPlayer(testPlayerEntityB);

        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isPresent();
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).isPresent();

        GameSessionEntity testGameSessionEntity = TestDataUtil.createTestGameSessionEntityWith2Players();
        FinishedTurnDto finishedTurnDto = TestDataUtil.getTestFinishedTurnDto();

        gameSessionEntityRepository.save(testGameSessionEntity);
        assertThat(gameSessionEntityService.findById(testGameSessionEntity.getId())).isNotNull();

        String playerIdJson = objectMapper.writeValueAsString(testPlayerEntityA.getId());
        String finishedTurnDtoJson = objectMapper.writeValueAsString(finishedTurnDto);

        String payload = playerIdJson + "|" + finishedTurnDtoJson;

        StompSession session = initStompSessionWithTopicAndQueue("/topic/game-session-" + testGameSessionEntity.getId() + "/points-meeples", messages, "/user/queue/cheat-points", messages2);
        session.send("/app/cheat/add-points", payload);

        String cheatPointsString = messages2.poll(1, TimeUnit.SECONDS);
        Integer cheatPoints = objectMapper.readValue(cheatPointsString, Integer.class);

        FinishedTurnDto updatedFinishedTurnDto = TestDataUtil.getTestFinishedTurnDtoWithCheatPoints(cheatPoints);

        String expectedResponse = objectMapper.writeValueAsString(updatedFinishedTurnDto);
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatAddPointsForPlayerWithNonExistentPlayerFails() throws Exception {
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);

        GameSessionEntity testGameSessionEntity = TestDataUtil.createTestGameSessionEntityWith2Players();
        FinishedTurnDto finishedTurnDto = TestDataUtil.getTestFinishedTurnDto();

        gameSessionEntityRepository.save(testGameSessionEntity);
        assertThat(gameSessionEntityService.findById(testGameSessionEntity.getId())).isNotNull();

        String playerIdJson = objectMapper.writeValueAsString(testPlayerEntityA.getId());
        String finishedTurnDtoJson = objectMapper.writeValueAsString(finishedTurnDto);

        String payload = playerIdJson + "|" + finishedTurnDtoJson;

        StompSession session = initStompSession("/user/queue/errors", messages);
        session.send("/app/cheat/add-points", payload);

        String expectedResponse = "ERROR: " + ErrorCode.ERROR_2001.getCode();
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

//    @Test
//    void testThatAddPointsForPlayerPlayerCannotCheatFails() throws Exception {
//        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
//
//        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);
//
//        playerEntityService.createPlayer(testPlayerEntityA);
//        playerEntityService.createPlayer(testPlayerEntityB);
//
//        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isPresent();
//        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).isPresent();
//
//        GameSessionEntity testGameSessionEntity = TestDataUtil.createTestGameSessionEntityWith2Players();
//        FinishedTurnDto finishedTurnDto = TestDataUtil.getTestFinishedTurnDto();
//
//        gameSessionEntityRepository.save(testGameSessionEntity);
//        assertThat(gameSessionEntityService.findById(testGameSessionEntity.getId())).isNotNull();
//
//        String playerIdJson = objectMapper.writeValueAsString(testPlayerEntityA.getId());
//        String finishedTurnDtoJson = objectMapper.writeValueAsString(finishedTurnDto);
//
//        String payload = playerIdJson + "|" + finishedTurnDtoJson;
//
//        StompSession session = initStompSession("/user/queue/errors", messages);
//        session.send("/app/cheat/add-points", payload);
//
//        String expectedResponse = "ERROR: " + ErrorCode.ERROR_3005.getCode();
//        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
//
//        assertThat(actualResponse).isEqualTo(expectedResponse);
//    }

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

    @Test
    void testHandleCanICheatTrue() throws Exception {
        // Arrange
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        testPlayerEntityA.setCanCheat(true);
        playerEntityService.createPlayer(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isPresent();

        String playerIdJson = objectMapper.writeValueAsString(testPlayerEntityA.getId());

        // Act
        StompSession session = initStompSession("/user/queue/cheat-can-i-cheat", messages);
        session.send("/app/cheat/can-i-cheat", playerIdJson);

        // Assert
        String expectedResponse = "true";
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testHandleCanICheatFalse() throws Exception {
        // Arrange
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        testPlayerEntityA.setCanCheat(false);
        playerEntityService.createPlayer(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isPresent();

        String playerIdJson = objectMapper.writeValueAsString(testPlayerEntityA.getId());

        // Act
        StompSession session = initStompSession("/user/queue/cheat-can-i-cheat", messages);
        session.send("/app/cheat/can-i-cheat", playerIdJson);

        // Assert
        String expectedResponse = "false";
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testHandleCorrectCheaterAccusation() throws Exception {
        // Arrange
        // Accusing player
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        testPlayerEntityA.setCanCheat(false);
        playerEntityService.createPlayer(testPlayerEntityA);

        // Accused player
        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        testPlayerEntityB.setCanCheat(true);
        testPlayerEntityB.setCheatPoints(8);
        playerEntityService.createPlayer(testPlayerEntityB);

        GameSessionEntity testGameSessionEntity = TestDataUtil.createTestGameSessionEntityWith2Players();
        FinishedTurnDto finishedTurnDto = TestDataUtil.getTestFinishedTurnDto2();

        gameSessionEntityRepository.save(testGameSessionEntity);

        String playerIdJson = "" + testPlayerEntityA.getId();
        String accusedPlayerIdJson = ""+ testPlayerEntityB.getId();
        String finishedTurnDtoJson = objectMapper.writeValueAsString(finishedTurnDto);

        String payload = playerIdJson + "|" + accusedPlayerIdJson + "|" + finishedTurnDtoJson;

        // Act
        StompSession session = initStompSession("/topic/game-session-" + testGameSessionEntity.getId() + "/cheat-detected", messages);
        initStompSession("/topic/game-session-" + testGameSessionEntity.getId() + "/points-meeples", messages2);

        session.send("/app/cheat/accuse", payload);

        // Assert
        String expectedResponse = "true";
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        assertThat(actualResponse).isEqualTo(expectedResponse);

        String actualResponse2 = messages2.poll(1, TimeUnit.SECONDS);
        FinishedTurnDto actualFinishedTurnDto = objectMapper.readValue(actualResponse2, FinishedTurnDto.class);
        // assert that the points hashmap of the finishedTurnDto contains a negative value for the accused player (id 2) and a positive value for the accusing player (id 1)
        assertThat(actualFinishedTurnDto.getPoints().get(testPlayerEntityA.getId())).isPositive();
        assertThat(actualFinishedTurnDto.getPoints().get(testPlayerEntityB.getId())).isNegative();
    }

    @Test
    void testHandleIncorrectCheaterAccusation() throws Exception {
        // Arrange
        // Accusing player
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        testPlayerEntityA.setCanCheat(false);
        playerEntityService.createPlayer(testPlayerEntityA);

        // Accused player
        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        testPlayerEntityB.setCanCheat(false);
        playerEntityService.createPlayer(testPlayerEntityB);

        GameSessionEntity testGameSessionEntity = TestDataUtil.createTestGameSessionEntityWith2Players();
        FinishedTurnDto finishedTurnDto = TestDataUtil.getTestFinishedTurnDto2();

        gameSessionEntityRepository.save(testGameSessionEntity);

        String playerIdJson = "" + testPlayerEntityA.getId();
        String accusedPlayerIdJson = ""+ testPlayerEntityB.getId();
        String finishedTurnDtoJson = objectMapper.writeValueAsString(finishedTurnDto);

        String payload = playerIdJson + "|" + accusedPlayerIdJson + "|" + finishedTurnDtoJson;

        // Act
        StompSession session = initStompSession("/topic/game-session-" + testGameSessionEntity.getId() + "/points-meeples", messages);

        session.send("/app/cheat/accuse", payload);

        // Assert
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        FinishedTurnDto actualFinishedTurnDto = objectMapper.readValue(actualResponse, FinishedTurnDto.class);
        // assert that the points hashmap of the finishedTurnDto contains 0 for the accused player (id 2) since he is not the cheater and a negative value for the accusing player (id 1)
        assertThat(actualFinishedTurnDto.getPoints().get(testPlayerEntityA.getId())).isNegative();
        assertThat(actualFinishedTurnDto.getPoints().get(testPlayerEntityB.getId())).isZero();
    }

    @Test
    void testHandleCheaterAccusationWhereCheaterHasNotCheatedYet() throws Exception {
        // Arrange
        // Accusing player
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        testPlayerEntityA.setCanCheat(false);
        playerEntityService.createPlayer(testPlayerEntityA);

        // Accused player
        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        testPlayerEntityB.setCanCheat(true);
        testPlayerEntityB.setCheatPoints(0);
        playerEntityService.createPlayer(testPlayerEntityB);

        GameSessionEntity testGameSessionEntity = TestDataUtil.createTestGameSessionEntityWith2Players();
        FinishedTurnDto finishedTurnDto = TestDataUtil.getTestFinishedTurnDto2();

        gameSessionEntityRepository.save(testGameSessionEntity);

        String playerIdJson = "" + testPlayerEntityA.getId();
        String accusedPlayerIdJson = ""+ testPlayerEntityB.getId();
        String finishedTurnDtoJson = objectMapper.writeValueAsString(finishedTurnDto);

        String payload = playerIdJson + "|" + accusedPlayerIdJson + "|" + finishedTurnDtoJson;

        // Act
        StompSession session = initStompSession("/topic/game-session-" + testGameSessionEntity.getId() + "/points-meeples", messages);

        session.send("/app/cheat/accuse", payload);

        // Assert
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        FinishedTurnDto actualFinishedTurnDto = objectMapper.readValue(actualResponse, FinishedTurnDto.class);
        // assert that the points hashmap of the finishedTurnDto contains 0 for the accused player (id 2) since he has not cheated yet and a negative value for the accusing player (id 1)
        assertThat(actualFinishedTurnDto.getPoints().get(testPlayerEntityA.getId())).isNegative();
        assertThat(actualFinishedTurnDto.getPoints().get(testPlayerEntityB.getId())).isZero();
    }



    public StompSession initStompSessionWithTopicAndQueue(String topic, BlockingQueue<String> messages, String queue, BlockingQueue<String> messages2) throws Exception {
        StompSession session = initStompSession(topic, messages);
        session.subscribe(queue, new StompFrameHandlerClientImpl(messages2));

        return session;
    }


}
