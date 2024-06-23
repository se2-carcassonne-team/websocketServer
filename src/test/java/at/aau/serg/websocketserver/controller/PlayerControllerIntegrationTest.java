package at.aau.serg.websocketserver.controller;

import at.aau.serg.websocketserver.TestDataUtil;
import at.aau.serg.websocketserver.demo.websocket.StompFrameHandlerClientImpl;
import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketserver.domain.dto.PlayerDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.domain.pojo.PlayerColour;
import at.aau.serg.websocketserver.statuscode.ErrorCode;
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
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PlayerControllerIntegrationTest {
    private final ObjectMapper objectMapper;
    private final PlayerEntityService playerEntityService;
    private final GameLobbyEntityService gameLobbyEntityService;
    private final PlayerMapper playerMapper;
    private final GameLobbyMapper gameLobbyMapper;

    @Autowired
    public PlayerControllerIntegrationTest(ObjectMapper objectMapper, PlayerEntityService playerEntityService, GameLobbyEntityService gameLobbyEntityService, PlayerMapper playerMapper, GameLobbyMapper gameLobbyMapper) {
        this.objectMapper = objectMapper;
        this.playerEntityService = playerEntityService;
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.playerMapper = playerMapper;
        this.gameLobbyMapper = gameLobbyMapper;
    }

    @LocalServerPort
    private int port;
    private final String WEBSOCKET_URI = "ws://localhost:%d/websocket-broker";
    BlockingQueue<String> messages;
    BlockingQueue<String> messages2;
    BlockingQueue<String> messages3;
    BlockingQueue<String> messages4;

    @BeforeEach
    public void setUp() {
        messages = new LinkedBlockingDeque<>();
        messages2 = new LinkedBlockingDeque<>();
        messages3 = new LinkedBlockingDeque<>();
        messages4 = new LinkedBlockingDeque<>();
    }

    @AfterEach
    public void tearDown() {
        messages = null;
        messages2 = null;
        messages3 = null;
        messages4 = null;
    }

    @Test
    void testThatCreatePlayerHandlerSuccessfullyCreatesPlayer() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/create-user-response";
        StompSession session = initStompSession("/user/queue/response", messages);

        PlayerDto testPlayerDto = TestDataUtil.createTestPlayerDtoA(null);
        PlayerEntity testPlayerEntity = playerMapper.mapToEntity(testPlayerDto);

        // assert that the test player doesn't exist in the database yet
        assertThat(playerEntityService.findPlayerById(testPlayerEntity.getId())).isEmpty();

        // manually transform the PlayerDto object to a JSON-string:
        testPlayerDto.setCheatPoints(0);
        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDto);

        session.send("/app/player-create", playerDtoJson);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // Handling of the session id
        String playerSessionId = playerEntityService.findPlayerById(testPlayerEntity.getId()).get().getSessionId();
        testPlayerDto.setSessionId(playerSessionId);
        testPlayerEntity = playerMapper.mapToEntity(testPlayerDto);
        playerDtoJson = objectMapper.writeValueAsString(testPlayerDto);

        // assert that the player now exists in the database
        assertThat(playerEntityService.findPlayerById(testPlayerEntity.getId())).isPresent();
        assertThat(playerEntityService.findPlayerById(testPlayerEntity.getId()).get()).isEqualTo(testPlayerEntity);

        // assert that the controller response is as we expect. The controller should return the DTO of the created player
        assertThat(actualResponse).isEqualTo(playerDtoJson);
    }

    @Test
    void testThatCreatePlayerWithFaultyJsonInputReturnsExpectedResponse() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);
        String faultyInput = "Not a Json of a PlayerDto";
        session.send("/app/player-create", faultyInput);
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // JsonProcessingException for this particular input:
        // Received message from server: Unrecognized token 'Not a Json of a PlayerDto': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false') at [Source: (String)"notADtoInJson"; line: 1, column: 14]

        assertThat(actualResponse).contains("Unrecognized token");
    }

    @Test
    void testThatCreatePlayerWithExistingIdFails() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);

        // create player with id=1L in database
        playerEntityService.createPlayer(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isPresent();

        // new player with same id:
        PlayerDto testPlayerDtoB = playerMapper.mapToDto(TestDataUtil.createTestPlayerEntityB(null));
        testPlayerDtoB.setId(testPlayerEntityA.getId());

        String playerDtoBJson = objectMapper.writeValueAsString(testPlayerDtoB);

        session.send("/app/player-create", playerDtoBJson);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);


        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2002.getCode());
    }

    @Test
    void testThatCreatePlayerWithExistingUsernameFails() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);

        // create player with username="taken" in database
        testPlayerEntityA.setUsername("taken");

        playerEntityService.createPlayer(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);

        // new player with same username:
        PlayerDto testPlayerDtoB = playerMapper.mapToDto(TestDataUtil.createTestPlayerEntityB(null));
        testPlayerDtoB.setUsername(testPlayerEntityA.getUsername());

        String playerDtoBJson = objectMapper.writeValueAsString(testPlayerDtoB);

        session.send("/app/player-create", playerDtoBJson);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);


        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2003.getCode());
    }

    @Test
    void testThatJoinLobbySuccessfullyReturnsUpdatedPlayerDtoToPlayerQueue() throws Exception {
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();

        StompSession session = initStompSession("/user/queue/response", messages);

        // Pre-populate the database
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).isEmpty();
        gameLobbyEntityService.createLobby(testGameLobbyEntityA);
        playerEntityService.createPlayer(testPlayerEntityA);

        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        GameLobbyDto testGameLobbyDtoA = gameLobbyMapper.mapToDto(testGameLobbyEntityA);

        // manually transform objects to JSON-strings and combine them:
        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDtoA);

        String payload = testGameLobbyDtoA.getId() + "|" +  playerDtoJson;

        // before sending payload:
        // 1) assert that the test player doesn't reference a game lobby
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isNull();
        // 2) assert the numPlayers of the test game lobby is 0
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isZero();

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(1);

        PlayerEntity updatedPlayerEntity = playerEntityService.findPlayerById(testPlayerEntityA.getId()).get();
        String updatedPlayerEntityColour = updatedPlayerEntity.getPlayerColour();

        testGameLobbyEntityA.setNumPlayers(testGameLobbyEntityA.getNumPlayers()+1);
        testGameLobbyEntityA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsStringListRemoveValue(updatedPlayerEntityColour));
        assertThat(updatedPlayerEntity.getGameLobbyEntity()).isEqualTo(testGameLobbyEntityA);

        testGameLobbyDtoA.setNumPlayers(testGameLobbyDtoA.getNumPlayers()+1);
        testPlayerDtoA.setGameLobbyId(testGameLobbyDtoA.getId());

        // Randomness-Workaround for playerColour
        testPlayerDtoA.setPlayerColour(PlayerColour.valueOf(updatedPlayerEntityColour));

        var expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatJoinLobbySuccessfullySendsUpdatedLobbyListToTopic() throws Exception {
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();

        StompSession session = initStompSession("/topic/lobby-list", messages);
        // Pre-populate the database
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).isEmpty();
        gameLobbyEntityService.createLobby(testGameLobbyEntityA);
        playerEntityService.createPlayer(testPlayerEntityA);

        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        GameLobbyDto testGameLobbyDtoA = gameLobbyMapper.mapToDto(testGameLobbyEntityA);

        // manually transform objects to JSON-strings and combine them:
        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDtoA);

        String payload = testGameLobbyDtoA.getId() + "|" +  playerDtoJson;

        // before sending payload:
        // 1) assert that the test player doesn't reference a game lobby
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isNull();
        // 2) assert the numPlayers of the test game lobby is 0
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isZero();

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after sending & controller processing payload:
        // 1) assert that the test game lobby now has numPlayers = 1
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(1);
        // 2) assert that the test player references the test game lobby
        PlayerEntity updatedPlayerEntity = playerEntityService.findPlayerById(testPlayerEntityA.getId()).get();
        testGameLobbyEntityA.setNumPlayers(testGameLobbyEntityA.getNumPlayers()+1);
        testGameLobbyEntityA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsStringListRemoveValue(updatedPlayerEntity.getPlayerColour()));
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(testGameLobbyEntityA);


        // expected response: updated list of lobbies
        List<GameLobbyDto> listOfGameLobbyDtos = getGameLobbyDtoList();

        var expectedResponse = objectMapper.writeValueAsString(listOfGameLobbyDtos);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatJoinLobbySuccessfullySendsUpdatedListOfPlayersToLobbyTopic() throws Exception {
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();

        StompSession session = initStompSession("/topic/lobby-" + testGameLobbyEntityA.getId(), messages);
        // Pre-populate the database
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).isEmpty();
        gameLobbyEntityService.createLobby(testGameLobbyEntityA);
        playerEntityService.createPlayer(testPlayerEntityA);

        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        GameLobbyDto testGameLobbyDtoA = gameLobbyMapper.mapToDto(testGameLobbyEntityA);

        // manually transform objects to JSON-strings and combine them:
        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDtoA);

        String payload = testGameLobbyDtoA.getId() + "|" +  playerDtoJson;

        // before sending payload:
        // 1) assert that the test player doesn't reference a game lobby
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isNull();
        // 2) assert the numPlayers of the test game lobby is 0
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isZero();

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after sending & controller processing payload:
        // 1) assert that the test game lobby now has numPlayers = 1
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(1);
        // 2) assert that the test player references the test game lobby
        PlayerEntity updatedPlayerEntity = playerEntityService.findPlayerById(testPlayerEntityA.getId()).get();
        testGameLobbyEntityA.setNumPlayers(testGameLobbyEntityA.getNumPlayers()+1);
        testGameLobbyEntityA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsStringListRemoveValue(updatedPlayerEntity.getPlayerColour()));
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(testGameLobbyEntityA);


        // expected response: updated list of players in lobby
        List<PlayerDto> updatedListOfPlayersInLobby = getPlayerDtosInLobbyList(testPlayerEntityA.getId());

        var expectedResponse = objectMapper.writeValueAsString(updatedListOfPlayersInLobby);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    // DONE: test 3 sessions in parallel (queue, topic/lobby-list, topic/lobby-$id)
    /**
     * Test if 3 sessions at the same time all receive their respective responses:
     * <ol>
     *     <li>/user/queue/response expects updated playerDtp</li>
     *     <li>/topic/lobby-list expects updated list of lobbies</li>
     *     <li>/topic/lobby-$id expects updated list of players in lobby</li>
     * </ol>
     * @throws Exception
     */
    @Test
    void testThatJoinLobbySuccessfullyReturnsCorrectResponsesToAllThreeTopicsInParallel() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/player-join-lobby-response";
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();

        StompSession session = initStompSession("/user/queue/response", messages);
        initStompSession("/topic/lobby-list", messages2);
        initStompSession("/topic/lobby-" + testGameLobbyEntityA.getId(), messages3);

        // Pre-populate the database
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).isEmpty();
        gameLobbyEntityService.createLobby(testGameLobbyEntityA);
        playerEntityService.createPlayer(testPlayerEntityA);

        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        GameLobbyDto testGameLobbyDtoA = gameLobbyMapper.mapToDto(testGameLobbyEntityA);

        // manually transform objects to JSON-strings and combine them:
        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDtoA);

        String payload = testGameLobbyDtoA.getId() + "|" +  playerDtoJson;

        // before sending payload:
        // 1) assert that the test player doesn't reference a game lobby
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isNull();
        // 2) assert the numPlayers of the test game lobby is 0
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isZero();

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        String actualResponse2 = messages2.poll(1, TimeUnit.SECONDS);
        String actualResponse3 = messages3.poll(1, TimeUnit.SECONDS);

        // after sending & controller processing payload:
        // 1) assert that the test game lobby now has numPlayers = 1
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(1);
        // 2) assert that the test player references the test game lobby
        PlayerEntity updatedPlayerEntity = playerEntityService.findPlayerById(testPlayerEntityA.getId()).get();
        testGameLobbyEntityA.setNumPlayers(testGameLobbyEntityA.getNumPlayers()+1);
        testGameLobbyEntityA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsStringListRemoveValue(updatedPlayerEntity.getPlayerColour()));
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(testGameLobbyEntityA);


        // expected response: updated playerDto with the Lobby, which itself should also be updated to have incremented numPlayers, and the playerColour should be set
        testGameLobbyDtoA.setNumPlayers(testGameLobbyDtoA.getNumPlayers()+1);
        testPlayerDtoA.setGameLobbyId(testGameLobbyDtoA.getId());
        testPlayerDtoA.setPlayerColour(PlayerColour.valueOf(updatedPlayerEntity.getPlayerColour()));
        var expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);
        assertThat(actualResponse).isEqualTo(expectedResponse);

        // expected response: updated list of lobbies
        List<GameLobbyDto> listOfGameLobbyDtos = getGameLobbyDtoList();
        var expectedResponse2 = objectMapper.writeValueAsString(listOfGameLobbyDtos);
        assertThat(actualResponse2).isEqualTo(expectedResponse2);

        // expected response3: updated list of players in lobby
        List<PlayerDto> updatedListOfPlayersInLobby = getPlayerDtosInLobbyList(testPlayerEntityA.getId());
        var expectedResponse3 = objectMapper.writeValueAsString(updatedListOfPlayersInLobby);
        assertThat(actualResponse3).isEqualTo(expectedResponse3);
    }

    // DONE: test multiple sessions in parallel (with subscriptions to the same topic)
    /**
     * Test if 4 sessions at the same time all receive their respective responses:
     * <ol>
     *     <li>/user/queue/response expects updated playerDtp</li>
     *     <li>session2: /topic/lobby-list expects updated list of lobbies</li>
     *     <li>session3: /topic/lobby-list expects updated list of lobbies</li>
     *     <li>session4: /topic/lobby-list expects updated list of lobbies</li>
     * </ol>
     * @throws Exception
     */
    @Test
    void testThatJoinLobbySuccessfullyReturnsCorrectResponsesToMultipleClientsOnSameTopic() throws Exception {
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();

        StompSession session = initStompSession("/user/queue/response", messages);
        initStompSession("/topic/lobby-list", messages2);
        initStompSession("/topic/lobby-list", messages3);
        initStompSession("/topic/lobby-list", messages4);


        // Pre-populate the database
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).isEmpty();
        gameLobbyEntityService.createLobby(testGameLobbyEntityA);
        playerEntityService.createPlayer(testPlayerEntityA);

        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        GameLobbyDto testGameLobbyDtoA = gameLobbyMapper.mapToDto(testGameLobbyEntityA);

        // manually transform objects to JSON-strings and combine them:
        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDtoA);

        String payload = testGameLobbyDtoA.getId() + "|" +  playerDtoJson;

        // before sending payload:
        // 1) assert that the test player doesn't reference a game lobby
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isNull();
        // 2) assert the numPlayers of the test game lobby is 0
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isZero();

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        String actualResponse2 = messages2.poll(1, TimeUnit.SECONDS);
        String actualResponse3 = messages3.poll(1, TimeUnit.SECONDS);
        String actualResponse4 = messages4.poll(1, TimeUnit.SECONDS);

        // after sending & controller processing payload:
        // 1) assert that the test game lobby now has numPlayers = 1
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(1);
        // 2) assert that the test player references the test game lobby
        PlayerEntity updatedPlayerEntity = playerEntityService.findPlayerById(testPlayerEntityA.getId()).get();
        testGameLobbyEntityA.setNumPlayers(testGameLobbyEntityA.getNumPlayers()+1);
        testGameLobbyEntityA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsStringListRemoveValue(updatedPlayerEntity.getPlayerColour()));
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(testGameLobbyEntityA);


        // expected response: updated playerDto with the Lobby, which itself should also be updated to have incremented numPlayers
        testGameLobbyDtoA.setNumPlayers(testGameLobbyDtoA.getNumPlayers()+1);
        testPlayerDtoA.setGameLobbyId(testGameLobbyDtoA.getId());
        testPlayerDtoA.setPlayerColour(PlayerColour.valueOf(updatedPlayerEntity.getPlayerColour()));
        var expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);
        assertThat(actualResponse).isEqualTo(expectedResponse);

        // expected responses for sessions 2-4: updated list of lobbies
        List<GameLobbyDto> listOfGameLobbyDtos = getGameLobbyDtoList();
        var expectedResponseListOfLobbies = objectMapper.writeValueAsString(listOfGameLobbyDtos);
        assertThat(actualResponse2).isEqualTo(expectedResponseListOfLobbies);
        assertThat(actualResponse3).isEqualTo(expectedResponseListOfLobbies);
        assertThat(actualResponse4).isEqualTo(expectedResponseListOfLobbies);
    }

    @Test
    void testThatJoinLobbyWithFaultyGameLobbyIdReturnsExpectedResult() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        // Pre-populate the database
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).isEmpty();
        gameLobbyEntityService.createLobby(testGameLobbyEntityA);
        playerEntityService.createPlayer(testPlayerEntityA);

        // player will now try to join a lobby but some of the JSON is faulty
        // manually transform objects to JSON-strings and combine them:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        GameLobbyDto testGameLobbyDtoA = gameLobbyMapper.mapToDto(testGameLobbyEntityA);

        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDtoA);
        String invalidGameLobbyId = "not an ID";

        String payload = invalidGameLobbyId + "|" +  playerDtoJson;

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isNull();

        // JsonProcessingException:   "Unrecognized token 'faulty': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false') at [Source: (String)"faulty JSON"; line: 1, column: 7]"
        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_1005.getCode());
    }

    @Test
    void testThatJoinLobbyWithFaultyPlayerDtoJsonReturnsExpectedResult() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        // Pre-populate the database
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).isEmpty();
        gameLobbyEntityService.createLobby(testGameLobbyEntityA);
        playerEntityService.createPlayer(testPlayerEntityA);

        // player will now try to join a lobby but some of the JSON is faulty
        // manually transform objects to JSON-strings and combine them:
        GameLobbyDto testGameLobbyDtoA = gameLobbyMapper.mapToDto(testGameLobbyEntityA);

        String playerDtoJson = "not valid JSON";

        String payload = testGameLobbyDtoA.getId() + "|" +  playerDtoJson;

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isNull();

        // JsonProcessingException:   "Unrecognized token 'faulty': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false') at [Source: (String)"faulty JSON"; line: 1, column: 7]"
        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2004.getCode());
    }

    @Test
    void testThatPlayerCannotJoinFullGameLobby() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        // Pre-populate the database
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).isEmpty();
        gameLobbyEntityService.createLobby(testGameLobbyEntityA);

        // 6 players already in the lobby:
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity testPlayerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        PlayerEntity testPlayerEntityD = TestDataUtil.createTestPlayerEntityD(null);
        PlayerEntity testPlayerEntityE = TestDataUtil.createTestPlayerEntityE(null);

        playerEntityService.createPlayer(testPlayerEntityA);
        playerEntityService.createPlayer(testPlayerEntityB);
        playerEntityService.createPlayer(testPlayerEntityC);
        playerEntityService.createPlayer(testPlayerEntityD);
        playerEntityService.createPlayer(testPlayerEntityE);

        playerEntityService.joinLobby(testGameLobbyEntityA.getId(), testPlayerEntityA);
        playerEntityService.joinLobby(testGameLobbyEntityA.getId(), testPlayerEntityB);
        playerEntityService.joinLobby(testGameLobbyEntityA.getId(), testPlayerEntityC);
        playerEntityService.joinLobby(testGameLobbyEntityA.getId(), testPlayerEntityD);
        playerEntityService.joinLobby(testGameLobbyEntityA.getId(), testPlayerEntityE);
        // lobby is now be full: 5/5 players

        // seventh player should not be able to join the same lobby:
        PlayerEntity testPlayerEntityF = TestDataUtil.createTestPlayerEntityG(null);
        playerEntityService.createPlayer(testPlayerEntityF);

        PlayerDto testPlayerDtoG = playerMapper.mapToDto(testPlayerEntityA);
        GameLobbyDto testGameLobbyDtoA = gameLobbyMapper.mapToDto(testGameLobbyEntityA);

        // manually transform objects to JSON-strings and combine them:
        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDtoG);

        String payload = testGameLobbyDtoA.getId() + "|" +  playerDtoJson;

        // before sending payload:
        // 1) assert that the test player doesn't reference a game lobby
        assertThat(playerEntityService.findPlayerById(testPlayerEntityF.getId()).get().getGameLobbyEntity()).isNull();
        // 2) assert the numPlayers of the test game lobby is 5
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(5);

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after sending & controller processing payload:
        // 1) assert that the test game lobby still has numPlayers = 5
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(5);
        // 2) assert that the seventh player still doesn't reference a lobby
        assertThat(playerEntityService.findPlayerById(testPlayerEntityF.getId()).get().getGameLobbyEntity()).isNull();

        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_1004.getCode());
    }

    @Test
    void tesThatJoinNonExistentLobbyReturnsExpectedResult() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        // Pre-populate the database: only save the testPlayerEntityA to the database!
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        playerEntityService.createPlayer(testPlayerEntityA);

        // player will now try to join a lobby that doesn't exist:
        // manually transform objects to JSON-strings and combine them:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        GameLobbyDto testGameLobbyDtoA = gameLobbyMapper.mapToDto(testGameLobbyEntityA);

        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDtoA);

        String payload = testGameLobbyDtoA.getId() + "|" +  playerDtoJson;

        // before sending payload:
        // 1) assert that the test player doesn't reference a game lobby
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isNull();
        // 2) assert that no gameLobby exists in the database
        assertThat(gameLobbyEntityService.getListOfLobbies()).isEmpty();

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_1003.getCode());
    }

    @Test
    void testThatListAllPlayersForALobbyReturnsExpectedResult() throws Exception {
        StompSession session = initStompSession("/user/queue/player-list-response", messages);

        GameLobbyEntity gameLobbyEntity = TestDataUtil.createTestGameLobbyEntityA();
        gameLobbyEntityService.createLobby(gameLobbyEntity);

        assertThat(gameLobbyEntityService.findById(gameLobbyEntity.getId())).isPresent();

        List<PlayerEntity> playerEntityList = new ArrayList<>();
        List<PlayerDto> playerDtoList = new ArrayList<>();

        playerEntityList.add(TestDataUtil.createTestPlayerEntityA(gameLobbyEntity));
        playerEntityList.add(TestDataUtil.createTestPlayerEntityB(gameLobbyEntity));
        playerEntityList.add(TestDataUtil.createTestPlayerEntityC(null));

        for(PlayerEntity playerEntity : playerEntityList) {
            playerEntityService.createPlayer(playerEntity);
            assertThat(playerEntityService.findPlayerById(playerEntity.getId())).isPresent();

            if(playerEntity.getGameLobbyEntity() != null) {
                playerDtoList.add(playerMapper.mapToDto(playerEntity));
            }
        }

        session.send("/app/player-list", gameLobbyEntity.getId() + "");
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        String expectedResponse = objectMapper.writeValueAsString(playerDtoList);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatListAllPlayersForALobbyReturnsNoPlayers() throws Exception {
        StompSession session = initStompSession("/user/queue/player-list-response", messages);

        GameLobbyEntity gameLobbyEntity = TestDataUtil.createTestGameLobbyEntityA();
        gameLobbyEntityService.createLobby(gameLobbyEntity);

        assertThat(gameLobbyEntityService.findById(gameLobbyEntity.getId())).isPresent();
        List<PlayerDto> playerDtoList = new ArrayList<>();

        session.send("/app/player-list", gameLobbyEntity.getId() + "");
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        String expectedResponse = objectMapper.writeValueAsString(playerDtoList);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatListAllPlayersOfNonExistentLobbyFails() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        GameLobbyEntity gameLobbyEntity = TestDataUtil.createTestGameLobbyEntityA();
        assertThat(gameLobbyEntityService.findById(gameLobbyEntity.getId())).isNotPresent();

        session.send("/app/player-list", gameLobbyEntity.getId() +  "");
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_1003.getCode());
    }

    @Test
    void testThatUpdatePlayerUsernameWithGivenGameLobbySuccessfullyReturnsUpdatedPlayerDto() throws Exception {
        StompSession session = initStompSession("/user/queue/player-response", messages);

        // Populate the database with testPlayerEntityA
        GameLobbyEntity gameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(gameLobbyEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
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
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);

        // expected response: the dto of the updated player
        var expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatUpdatePlayerUsernameWithoutGameLobbySuccessfullyReturnsUpdatedPlayerDto() throws Exception {
        StompSession session = initStompSession("/user/queue/player-response", messages);

        // Populate the database with testPlayerEntityA
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
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
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);

        // expected response: the dto of the updated player
        var expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatUpdateUsernameOfNonExistentPlayerFails() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);

        // assert that the player entity doesn't exist in the database:
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();

        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        testPlayerDtoA.setUsername("UPDATED");

        String payload = objectMapper.writeValueAsString(testPlayerDtoA);
        session.send("/app/player-update-username", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // assert that the player still doesn't exist
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();

        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2001.getCode());
    }

    @Test
    void testThatUpdatePlayerUsernameWithFaultyJsonFails() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(testPlayerEntityA);
        // assert that the player entity exists in the database:
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isPresent();

        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        testPlayerDtoA.setUsername("UPDATED");

        String payload = "not a JSON";

        session.send("/app/player-update-username", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // assert that the player still exists
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isPresent();
        // assert that the username of the player hasn't changed to "UPDATED"
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getUsername()).isEqualTo(testPlayerEntityA.getUsername());

        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2004.getCode());
    }

    @Test
    void testThatLeaveLobbyWithMoreThanOnePlayerSuccessfullyRemovesPlayerFromGameLobbyAndReturnsUpdatedPlayerDto() throws Exception {

        // Populate the database with testPlayerEntityA who joins testGameLobbyEntityA:
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        testPlayerEntityA.setPlayerColour(PlayerColour.RED.name());
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        testGameLobbyEntityA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsStringListRemoveValue(PlayerColour.RED.name()));
        testPlayerEntityA.setGameLobbyEntity(testGameLobbyEntityA);

        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        testPlayerEntityB.setGameLobbyEntity(testGameLobbyEntityA);

        StompSession session = initStompSession("/user/queue/response", messages);

        testGameLobbyEntityA.setNumPlayers(2);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).isEmpty();

        playerEntityService.createPlayer(testPlayerEntityA);
        playerEntityService.createPlayer(testPlayerEntityB);
        // the referenced game lobby should automatically be created as well due to cascading (see entity definition)

        // before controller method call:
        // assert that the player and the game lobby (that player is in) exist in the database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).contains(testPlayerEntityB);
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).contains(testGameLobbyEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        PlayerEntity updatedTestPlayerEntityA = playerEntityService.findPlayerById(testPlayerEntityA.getId()).get();

        // after controller method call:
        // assert that the player and game lobby entities in the database have updated as expected
        testGameLobbyEntityA.setNumPlayers(1);
        testGameLobbyEntityA.getAvailableColours().add(testPlayerEntityA.getPlayerColour());
        assertThat(gameLobbyEntityService.findById(testPlayerDtoA.getId())).contains(testGameLobbyEntityA);

        testPlayerEntityA.setGameLobbyEntity(null);
        testPlayerEntityA.setPlayerColour(null);
        assertThat(updatedTestPlayerEntityA).isEqualTo(testPlayerEntityA);

        testPlayerDtoA.setGameLobbyId(null);
        testPlayerDtoA.setPlayerColour(null);
        var expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatLeaveLobbyAsLobbyLeaderWithMoreThanOnePlayerSuccessfullyTransfersGameLobbyAdmin() throws Exception {

        // Populate the database with testPlayerEntityA who joins testGameLobbyEntityA:
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        testPlayerEntityA.setPlayerColour(PlayerColour.RED.name());

        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        testGameLobbyEntityA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsStringListRemoveValue(PlayerColour.RED.name()));
        testPlayerEntityA.setGameLobbyEntity(testGameLobbyEntityA);
        testGameLobbyEntityA.setLobbyAdminId(testGameLobbyEntityA.getId());

        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        testPlayerEntityB.setGameLobbyEntity(testGameLobbyEntityA);

        StompSession session = initStompSession("/topic/lobby-" + testGameLobbyEntityA.getId() + "/update", messages);


        testGameLobbyEntityA.setNumPlayers(2);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).isEmpty();

        playerEntityService.createPlayer(testPlayerEntityA);
        playerEntityService.createPlayer(testPlayerEntityB);
        // the referenced game lobby should automatically be created as well due to cascading (see entity definition)

        // before controller method call:
        // assert that the player and the game lobby (that player is in) exist in the database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).contains(testPlayerEntityB);
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).contains(testGameLobbyEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after controller method call:
        // assert that the player and game lobby entities in the database have updated as expected
        testGameLobbyEntityA.setNumPlayers(1);
        // new: update lobbyCreator id
        testGameLobbyEntityA.setLobbyAdminId(testPlayerEntityB.getId());
        testGameLobbyEntityA.getAvailableColours().add(testPlayerEntityA.getPlayerColour());
        assertThat(gameLobbyEntityService.findById(testPlayerDtoA.getId())).contains(testGameLobbyEntityA);

        testPlayerEntityA.setGameLobbyEntity(null);
        testPlayerEntityA.setPlayerColour(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);

        testPlayerDtoA.setGameLobbyId(null);
        var expectedResponse = objectMapper.writeValueAsString(testGameLobbyEntityA);
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatLeaveLobbyAsNotLobbyLeaderWithMoreThanOnePlayerDoesNotTransfersGameLobbyAdmin() throws Exception {

        // Populate the database with testPlayerEntityA who joins testGameLobbyEntityA:
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);

        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        testGameLobbyEntityA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsStringListRemoveValue(PlayerColour.RED.name()));
        testPlayerEntityA.setGameLobbyEntity(testGameLobbyEntityA);

        // testPlayerA is the lobby creator:
        testGameLobbyEntityA.setLobbyAdminId(testGameLobbyEntityA.getId());

        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        testPlayerEntityB.setPlayerColour(PlayerColour.YELLOW.name());
        testGameLobbyEntityA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsStringListRemoveValue(testGameLobbyEntityA.getAvailableColours(), PlayerColour.YELLOW.name()));
        testPlayerEntityB.setGameLobbyEntity(testGameLobbyEntityA);

        StompSession session = initStompSession("/topic/lobby-" + testGameLobbyEntityA.getId() + "/update", messages);


        testGameLobbyEntityA.setNumPlayers(2);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).isEmpty();

        playerEntityService.createPlayer(testPlayerEntityA);
        playerEntityService.createPlayer(testPlayerEntityB);
        // the referenced game lobby should automatically be created as well due to cascading (see entity definition)

        // before controller method call:
        // assert that the player and the game lobby (that player is in) exist in the database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).contains(testPlayerEntityB);
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).contains(testGameLobbyEntityA);

        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getLobbyAdminId()).get().getLobbyAdminId()).isEqualTo(testPlayerEntityA.getId());

        // create payload string (player who is not a lobbyCreator)
        PlayerDto testPlayerDtoB = playerMapper.mapToDto(testPlayerEntityB);
        String payload = objectMapper.writeValueAsString(testPlayerDtoB);

        session.send("/app/player-leave-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after controller method call:
        // assert that the player and game lobby entities in the database have updated as expected
        testGameLobbyEntityA.setNumPlayers(1);
        testGameLobbyEntityA.getAvailableColours().add(testPlayerEntityB.getPlayerColour());
        GameLobbyEntity test = gameLobbyEntityService.findById(testPlayerEntityA.getId()).get();
        assertThat(gameLobbyEntityService.findById(testPlayerEntityA.getId())).contains(testGameLobbyEntityA);

        testPlayerEntityB.setGameLobbyEntity(null);
        testPlayerEntityB.setPlayerColour(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).contains(testPlayerEntityB);

        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getLobbyAdminId()).get().getLobbyAdminId()).isEqualTo(testPlayerEntityA.getId());

        testPlayerDtoB.setGameLobbyId(null);
        var expectedResponse = objectMapper.writeValueAsString(testGameLobbyEntityA);
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatLeaveLobbyWithMoreThanOnePlayerSuccessfullyRemovesPlayerFromGameLobbyAndReturnsUpdatedListOfLobbies() throws Exception {

        // Populate the database with testPlayerEntityA who joins testGameLobbyEntityA:
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        testPlayerEntityA.setPlayerColour(PlayerColour.RED.name());

        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        testGameLobbyEntityA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsStringListRemoveValue(PlayerColour.RED.name()));
        testPlayerEntityA.setGameLobbyEntity(testGameLobbyEntityA);

        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        testPlayerEntityB.setPlayerColour(PlayerColour.YELLOW.name());
        List<String> availableColours = testGameLobbyEntityA.getAvailableColours();
        testGameLobbyEntityA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsStringListRemoveValue(availableColours, PlayerColour.YELLOW.name()));
        testPlayerEntityB.setGameLobbyEntity(testGameLobbyEntityA);

        StompSession session = initStompSession("/topic/lobby-list", messages);


        testGameLobbyEntityA.setNumPlayers(2);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).isEmpty();

        playerEntityService.createPlayer(testPlayerEntityA);
        playerEntityService.createPlayer(testPlayerEntityB);
        // the referenced game lobby should automatically be created as well due to cascading (see entity definition)

        // before controller method call:
        // assert that the player and the game lobby (that player is in) exist in the database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).contains(testPlayerEntityB);
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).contains(testGameLobbyEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after controller method call:
        // assert that the player and game lobby entities in the database have updated as expected
        testGameLobbyEntityA.setNumPlayers(1);
        testGameLobbyEntityA.getAvailableColours().add(testPlayerEntityA.getPlayerColour());
        assertThat(gameLobbyEntityService.findById(testPlayerDtoA.getId())).contains(testGameLobbyEntityA);
        testPlayerEntityA.setGameLobbyEntity(null);
        testPlayerEntityA.setPlayerColour(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);
        testPlayerDtoA.setGameLobbyId(null);

        var expectedResponse = objectMapper.writeValueAsString(getGameLobbyDtoList());
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatLeaveLobbyWithMoreThanOnePlayerSuccessfullyRemovesPlayerFromGameLobbyAndReturnsUpdatedListPlayers() throws Exception {

        // Populate the database with testPlayerEntityA who joins testGameLobbyEntityA:
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        testPlayerEntityA.setPlayerColour(PlayerColour.RED.name());

        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        testGameLobbyEntityA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsStringListRemoveValue(PlayerColour.RED.name()));
        testPlayerEntityA.setGameLobbyEntity(testGameLobbyEntityA);

        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        testPlayerEntityB.setPlayerColour(PlayerColour.YELLOW.name());
        testPlayerEntityB.setGameLobbyEntity(testGameLobbyEntityA);

        List<String> availableColours = testGameLobbyEntityA.getAvailableColours();
        testGameLobbyEntityA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsStringListRemoveValue(availableColours, PlayerColour.YELLOW.name()));

        StompSession session = initStompSession("/topic/lobby-" + testGameLobbyEntityA.getId(), messages);


        testGameLobbyEntityA.setNumPlayers(2);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).isEmpty();

        playerEntityService.createPlayer(testPlayerEntityA);
        playerEntityService.createPlayer(testPlayerEntityB);
        // the referenced game lobby should automatically be created as well due to cascading (see entity definition)

        // before controller method call:
        // assert that the player and the game lobby (that player is in) exist in the database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).contains(testPlayerEntityB);
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).contains(testGameLobbyEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after controller method call:
        // assert that the player and game lobby entities in the database have updated as expected
        testGameLobbyEntityA.setNumPlayers(1);
        testGameLobbyEntityA.getAvailableColours().add(testPlayerEntityA.getPlayerColour());
        assertThat(gameLobbyEntityService.findById(testPlayerDtoA.getId())).contains(testGameLobbyEntityA);
        testPlayerEntityA.setGameLobbyEntity(null);
        testPlayerEntityA.setPlayerColour(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);
        testPlayerDtoA.setGameLobbyId(null);

        // better test: manually create expected list, but more work...
        var expectedResponse = objectMapper.writeValueAsString(getPlayerDtosInLobbyList(testGameLobbyEntityA.getId()));
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatLeaveLobbySuccessfullyWithOnePlayerRemovesPlayerFromGameLobbyAndDeletesLobby() throws Exception {

        // Populate the database with testPlayerEntityA who joins testGameLobbyEntityA:
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        testPlayerEntityA.setGameLobbyEntity(testGameLobbyEntityA);

        StompSession session = initStompSession("/user/queue/response", messages);

        testGameLobbyEntityA.setNumPlayers(1);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();

        playerEntityService.createPlayer(testPlayerEntityA);
        // the referenced game lobby should automatically be created as well due to cascading (see entity definition)

        // before controller method call:
        // assert that the player and the game lobby (that player is in) exist in the database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).contains(testGameLobbyEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).isEmpty()).isTrue();
        testPlayerEntityA.setGameLobbyEntity(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);

        testPlayerDtoA.setGameLobbyId(null);
        String expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatNonExistentPlayerLeaveLobbyFails() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        testPlayerEntityA.setGameLobbyEntity(testGameLobbyEntityA);

        // create only the game lobby in the database
        gameLobbyEntityService.createLobby(testGameLobbyEntityA);

        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId())).contains(testGameLobbyEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);


        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after controller method call:
        assertThat(gameLobbyEntityService.findById(testPlayerDtoA.getId())).contains(testGameLobbyEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();


        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2001.getCode());
    }

    @Test
    void testThatPlayerLeaveLobbyWhenNotInAnyLobbyFails() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        // Populate the database with testPlayerEntityA who joins testGameLobbyEntityA:
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();

        playerEntityService.createPlayer(testPlayerEntityA);
        // the referenced game lobby should automatically be created as well due to cascading (see entity definition)

        // before controller method call player with gameLobbyEntity=null should be in the database:
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after controller method call nothing should have changed in the database:
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).contains(testPlayerEntityA);

        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2005.getCode());
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

    public StompSession initStompSessionWithSecondTopic(String topic, String secondTopic, BlockingQueue<String> messages, BlockingQueue<String> messages2) throws Exception {
        StompSession session = initStompSession(topic, messages);
        session.subscribe(secondTopic, new StompFrameHandlerClientImpl(messages2));

        return session;
    }

    private List<GameLobbyDto> getGameLobbyDtoList() {
        List<GameLobbyEntity> gameLobbyEntities = gameLobbyEntityService.getListOfLobbies();
        List<GameLobbyDto> gameLobbyDtos = new ArrayList<>();
        if(gameLobbyEntities.isEmpty()){
            return gameLobbyDtos;
        }
        for (GameLobbyEntity gameLobbyEntity : gameLobbyEntities) {
            gameLobbyDtos.add(gameLobbyMapper.mapToDto(gameLobbyEntity));
        }
        return gameLobbyDtos;
    }

    private List<PlayerDto> getPlayerDtosInLobbyList(Long gameLobbyId) {
        List<PlayerEntity> playerEntityList = playerEntityService.getAllPlayersForLobby(gameLobbyId);
        List<PlayerDto> playerDtos = new ArrayList<>();
        if (playerEntityList.isEmpty()){
            return playerDtos;
        }
        for (PlayerEntity playerEntity : playerEntityList) {
            playerDtos.add(playerMapper.mapToDto(playerEntity));
        }
        return playerDtos;
    }
}
