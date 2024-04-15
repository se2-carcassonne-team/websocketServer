package at.aau.serg.websocketserver.controller;

import at.aau.serg.websocketserver.TestDataUtil;
import at.aau.serg.websocketserver.demo.websocket.StompFrameHandlerClientImpl;
import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketserver.domain.dto.PlayerDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
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
    public void testThatCreatePlayerHandlerSuccessfullyCreatesPlayer() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/create-user-response";
        StompSession session = initStompSession("/user/queue/response", messages);

        PlayerDto testPlayerDto = TestDataUtil.createTestPlayerDtoA(null);
        PlayerEntity testPlayerEntity = playerMapper.mapToEntity(testPlayerDto);

        // assert that the test player doesn't exist in the database yet
        assertThat(playerEntityService.findPlayerById(testPlayerEntity.getId())).isEmpty();

        // manually transform the PlayerDto object to a JSON-string:
        String playerDtoJson = objectMapper.writeValueAsString(testPlayerDto);

        session.send("/app/player-create", playerDtoJson);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

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


        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2002.getErrorCode());
    }

    @Test
    void testThatCreatePlayerWithExistingUsernameFails() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);

        // create player with username="taken" in database
        testPlayerEntityA.setUsername("taken");

        playerEntityService.createPlayer(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);

        // new player with same username:
        PlayerDto testPlayerDtoB = playerMapper.mapToDto(TestDataUtil.createTestPlayerEntityB(null));
        testPlayerDtoB.setUsername(testPlayerEntityA.getUsername());

        String playerDtoBJson = objectMapper.writeValueAsString(testPlayerDtoB);

        session.send("/app/player-create", playerDtoBJson);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);


        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2003.getErrorCode());
    }

    @Test
    void testThatJoinLobbySuccessfullyReturnsUpdatedPlayerDtoToPlayerQueue() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/player-join-lobby-response";
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
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(null);
        // 2) assert the numPlayers of the test game lobby is 0
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(0);

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
        testPlayerDtoA.setGameLobbyId(testGameLobbyDtoA.getId());
        var expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    // TODO: test multiple sessions in parallel (with subscriptions to the same topic)
    // TODO: test 3 sessions in parallel (queue, topic/lobby-list, topic/lobby-$id)


    @Test
    void testThatJoinLobbySuccessfullySendsUpdatedLobbyListToTopic() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/player-join-lobby-response";
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
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(null);
        // 2) assert the numPlayers of the test game lobby is 0
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(0);

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after sending & controller processing payload:
        // 1) assert that the test game lobby now has numPlayers = 1
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(1);
        // 2) assert that the test player references the test game lobby
        testGameLobbyEntityA.setNumPlayers(testGameLobbyEntityA.getNumPlayers()+1);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(testGameLobbyEntityA);


        // expected response: updated list of lobbies
        List<GameLobbyDto> listOfGameLobbyDtos = getGameLobbyDtoList();

        var expectedResponse = objectMapper.writeValueAsString(listOfGameLobbyDtos);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatJoinLobbySuccessfullySendsUpdatedListOfPlayersToLobbyTopic() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/player-join-lobby-response";
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
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(null);
        // 2) assert the numPlayers of the test game lobby is 0
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(0);

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after sending & controller processing payload:
        // 1) assert that the test game lobby now has numPlayers = 1
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(1);
        // 2) assert that the test player references the test game lobby
        testGameLobbyEntityA.setNumPlayers(testGameLobbyEntityA.getNumPlayers()+1);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(testGameLobbyEntityA);


        // expected response: updated list of players in lobby
        List<PlayerDto> updatedListOfPlayersInLobby = getPlayerDtosInLobbyList(testPlayerEntityA.getId());

        var expectedResponse = objectMapper.writeValueAsString(updatedListOfPlayersInLobby);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }
    @Test
    void testThatJoinLobbyWithFaultyGameLobbyIdReturnsExpectedResult() throws Exception {
        // TODO: implement
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

        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(null);

        // JsonProcessingException:   "Unrecognized token 'faulty': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false') at [Source: (String)"faulty JSON"; line: 1, column: 7]"
        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_1005.getErrorCode());
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
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        GameLobbyDto testGameLobbyDtoA = gameLobbyMapper.mapToDto(testGameLobbyEntityA);

        String playerDtoJson = "not valid JSON";

        String payload = testGameLobbyDtoA.getId() + "|" +  playerDtoJson;

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(null);

        // JsonProcessingException:   "Unrecognized token 'faulty': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false') at [Source: (String)"faulty JSON"; line: 1, column: 7]"
        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2004.getErrorCode());
    }

    @Test
    void testThatPlayerCannotJoinFullGameLobby() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/player-join-lobby-response";
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
        assertThat(playerEntityService.findPlayerById(testPlayerEntityF.getId()).get().getGameLobbyEntity()).isEqualTo(null);
        // 2) assert the numPlayers of the test game lobby is 5
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(5);

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after sending & controller processing payload:
        // 1) assert that the test game lobby still has numPlayers = 5
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get().getNumPlayers()).isEqualTo(5);
        // 2) assert that the seventh player still doesn't reference a lobby
        assertThat(playerEntityService.findPlayerById(testPlayerEntityF.getId()).get().getGameLobbyEntity()).isEqualTo(null);

        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_1004.getErrorCode());
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
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get().getGameLobbyEntity()).isEqualTo(null);
        // 2) assert that no gameLobby exists in the database
        assertThat(gameLobbyEntityService.getListOfLobbies()).isEmpty();

        session.send("/app/player-join-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_1003.getErrorCode());
    }

    @Test
    void testThatListAllPlayersForALobbyReturnsExpectedResult() throws Exception {
        StompSession session = initStompSession("/user/queue/response", messages);

        GameLobbyEntity gameLobbyEntity = TestDataUtil.createTestGameLobbyEntityA();
        gameLobbyEntityService.createLobby(gameLobbyEntity);

        assertThat(gameLobbyEntityService.findById(gameLobbyEntity.getId()).isPresent()).isTrue();

        List<PlayerEntity> playerEntityList = new ArrayList<>();
        List<PlayerDto> playerDtoList = new ArrayList<>();

        playerEntityList.add(TestDataUtil.createTestPlayerEntityA(gameLobbyEntity));
        playerEntityList.add(TestDataUtil.createTestPlayerEntityB(gameLobbyEntity));
        playerEntityList.add(TestDataUtil.createTestPlayerEntityC(null));

        for(PlayerEntity playerEntity : playerEntityList) {
            playerEntityService.createPlayer(playerEntity);
            assertThat(playerEntityService.findPlayerById(playerEntity.getId()).isPresent()).isTrue();

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
        StompSession session = initStompSession("/user/queue/response", messages);

        GameLobbyEntity gameLobbyEntity = TestDataUtil.createTestGameLobbyEntityA();
        gameLobbyEntityService.createLobby(gameLobbyEntity);

        assertThat(gameLobbyEntityService.findById(gameLobbyEntity.getId()).isPresent()).isTrue();
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
        assertThat(gameLobbyEntityService.findById(gameLobbyEntity.getId()).isPresent()).isFalse();

        session.send("/app/player-list", gameLobbyEntity.getId() +  "");
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_1003.getErrorCode());
    }

    @Test
    void testThatUpdatePlayerUsernameWithGivenGameLobbySuccessfullyReturnsUpdatedPlayerDto() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/player-update-username-response";
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
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);

        // expected response: the dto of the updated player
        var expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatUpdatePlayerUsernameWithoutGameLobbySuccessfullyReturnsUpdatedPlayerDto() throws Exception {
        //WEBSOCKET_TOPIC = "/topic/player-update-username-response";
        StompSession session = initStompSession("/user/queue/player-response", messages);

        // Populate the database with testPlayerEntityA
        GameLobbyEntity gameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
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
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);

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

        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2001.getErrorCode());
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

        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2004.getErrorCode());
    }

    @Test
    void testThatLeaveLobbyWithMoreThanOnePlayerSuccessfullyRemovesPlayerFromGameLobbyAndReturnsUpdatedPlayerDto() throws Exception {

        // Populate the database with testPlayerEntityA who joins testGameLobbyEntityA:
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
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
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId()).get()).isEqualTo(testPlayerEntityB);
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get()).isEqualTo(testGameLobbyEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after controller method call:
        // assert that the player and game lobby entities in the database have updated as expected
        testGameLobbyEntityA.setNumPlayers(1);
        assertThat(gameLobbyEntityService.findById(testPlayerDtoA.getId()).get()).isEqualTo(testGameLobbyEntityA);
        testPlayerEntityA.setGameLobbyEntity(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);

        testPlayerDtoA.setGameLobbyId(null);
        var expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatLeaveLobbyWithMoreThanOnePlayerSuccessfullyRemovesPlayerFromGameLobbyAndReturnsUpdatedListOfLobbies() throws Exception {

        // Populate the database with testPlayerEntityA who joins testGameLobbyEntityA:
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        testPlayerEntityA.setGameLobbyEntity(testGameLobbyEntityA);

        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);
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
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId()).get()).isEqualTo(testPlayerEntityB);
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get()).isEqualTo(testGameLobbyEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after controller method call:
        // assert that the player and game lobby entities in the database have updated as expected
        testGameLobbyEntityA.setNumPlayers(1);
        assertThat(gameLobbyEntityService.findById(testPlayerDtoA.getId()).get()).isEqualTo(testGameLobbyEntityA);
        testPlayerEntityA.setGameLobbyEntity(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);
        testPlayerDtoA.setGameLobbyId(null);

        var expectedResponse = objectMapper.writeValueAsString(getGameLobbyDtoList());
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatLeaveLobbyWithMoreThanOnePlayerSuccessfullyRemovesPlayerFromGameLobbyAndReturnsUpdatedListPlayers() throws Exception {

        // Populate the database with testPlayerEntityA who joins testGameLobbyEntityA:
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        testPlayerEntityA.setGameLobbyEntity(testGameLobbyEntityA);

        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        testPlayerEntityB.setGameLobbyEntity(testGameLobbyEntityA);

        StompSession session = initStompSession("/topic/lobby-" + testGameLobbyEntityA.getId(), messages);


        testGameLobbyEntityA.setNumPlayers(2);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId())).isEmpty();

        playerEntityService.createPlayer(testPlayerEntityA);
        playerEntityService.createPlayer(testPlayerEntityB);
        // the referenced game lobby should automatically be created as well due to cascading (see entity definition)

        // before controller method call:
        // assert that the player and the game lobby (that player is in) exist in the database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityB.getId()).get()).isEqualTo(testPlayerEntityB);
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get()).isEqualTo(testGameLobbyEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after controller method call:
        // assert that the player and game lobby entities in the database have updated as expected
        testGameLobbyEntityA.setNumPlayers(1);
        assertThat(gameLobbyEntityService.findById(testPlayerDtoA.getId()).get()).isEqualTo(testGameLobbyEntityA);
        testPlayerEntityA.setGameLobbyEntity(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);
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
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get()).isEqualTo(testGameLobbyEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).isEmpty()).isTrue();
        testPlayerEntityA.setGameLobbyEntity(null);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);

        testPlayerDtoA.setGameLobbyId(null);
        var expectedResponse = objectMapper.writeValueAsString(testPlayerDtoA);
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
        assertThat(gameLobbyEntityService.findById(testGameLobbyEntityA.getId()).get()).isEqualTo(testGameLobbyEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);


        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after controller method call:
        assertThat(gameLobbyEntityService.findById(testPlayerDtoA.getId()).get()).isEqualTo(testGameLobbyEntityA);
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();


        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2001.getErrorCode());
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
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);

        // create payload string:
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        String payload = objectMapper.writeValueAsString(testPlayerDtoA);

        session.send("/app/player-leave-lobby", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // after controller method call nothing should have changed in the database:
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);

        var expectedResponse = "Player is not in a game lobby.";
        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2005.getErrorCode());
    }

    @Test
    void testThatDeletePlayerSuccessfullyDeletesExistingPlayer() throws Exception {
        StompSession session = initStompSession("/user/queue/player-response", messages);

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(testPlayerEntityA);

        // assert that player currently exists in database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId()).get()).isEqualTo(testPlayerEntityA);

        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        session.send("/app/player-delete", objectMapper.writeValueAsString(testPlayerDtoA));

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // assert that player no longer exists in database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();

        var expectedResponse = "DELETED";
        assertThat(actualResponse).contains(expectedResponse);
    }

    @Test
    void testThatDeletePlayerSuccessfullyDeletesNonExistentPlayer() throws Exception {
        StompSession session = initStompSession("/user/queue/player-response", messages);

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(null);

        // assert that player currently doesn't exist in database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();

        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        session.send("/app/player-delete", objectMapper.writeValueAsString(testPlayerDtoA));

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        // assert that player no longer exists in database
        assertThat(playerEntityService.findPlayerById(testPlayerEntityA.getId())).isEmpty();

        var expectedResponse = "DELETED";
        assertThat(actualResponse).contains(expectedResponse);
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
