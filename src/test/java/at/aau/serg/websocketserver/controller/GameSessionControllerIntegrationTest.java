package at.aau.serg.websocketserver.controller;

import at.aau.serg.websocketserver.TestDataUtil;
import at.aau.serg.websocketserver.demo.websocket.StompFrameHandlerClientImpl;
import at.aau.serg.websocketserver.domain.dto.PlacedTileDto;
import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketserver.domain.dto.GameSessionDto;
import at.aau.serg.websocketserver.domain.dto.NextTurnDto;
import at.aau.serg.websocketserver.domain.pojo.GameState;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.domain.entity.TileDeckEntity;
import at.aau.serg.websocketserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketserver.domain.entity.repository.GameSessionEntityRepository;
import at.aau.serg.websocketserver.domain.entity.repository.TileDeckRepository;
import at.aau.serg.websocketserver.domain.pojo.PlayerColour;
import at.aau.serg.websocketserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketserver.mapper.GameSessionMapper;
import at.aau.serg.websocketserver.mapper.PlayerMapper;
import at.aau.serg.websocketserver.service.GameLobbyEntityService;
import at.aau.serg.websocketserver.service.GameSessionEntityService;
import at.aau.serg.websocketserver.service.PlayerEntityService;
import at.aau.serg.websocketserver.service.impl.TileDeckEntityServiceImpl;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GameSessionControllerIntegrationTest {

    private final ObjectMapper objectMapper;
    private final GameLobbyMapper gameLobbyMapper;
    private final PlayerMapper playerMapper;
    private final GameSessionMapper gameSessionMapper;
    private final GameSessionEntityService gameSessionEntityService;
    private final GameLobbyEntityService gameLobbyEntityService;
    private final PlayerEntityService playerEntityService;
    private final TileDeckEntityServiceImpl tileDeckEntityService;
    private final TileDeckRepository tileDeckRepository;

    @Autowired
    public GameSessionControllerIntegrationTest(ObjectMapper objectMapper, GameLobbyMapper gameLobbyMapper, PlayerMapper playerMapper, GameSessionMapper gameSessionMapper, GameSessionEntityService gameSessionEntityService, GameLobbyEntityService gameLobbyEntityService, PlayerEntityService playerEntityService, TileDeckEntityServiceImpl tileDeckEntityService, TileDeckRepository tileDeckRepository) {
        this.objectMapper = objectMapper;
        this.gameLobbyMapper = gameLobbyMapper;
        this.playerMapper = playerMapper;
        this.gameSessionMapper = gameSessionMapper;
        this.gameSessionEntityService = gameSessionEntityService;
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.playerEntityService = playerEntityService;
        this.tileDeckEntityService = tileDeckEntityService;
        this.tileDeckRepository = tileDeckRepository;
    }



    @LocalServerPort
    private int port;
    private final String WEBSOCKET_URI = "ws://localhost:%d/websocket-broker";
    BlockingQueue<String> messages;
    BlockingQueue<String> messages2;
    BlockingQueue<String> messages3;
    BlockingQueue<String> messages4;
    @Autowired
    private GameSessionEntityRepository gameSessionEntityRepository;
    @Autowired
    private GameLobbyEntityRepository gameLobbyEntityRepository;


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
    void testThatCreateGameSessionReturnsCreatedGameSessionIdToTopic() throws Exception {
        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        GameLobbyEntity gameLobbyEntityA = gameLobbyMapper.mapToEntity(gameLobbyDtoA);

        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());

        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);
        playerEntityService.createPlayer(playerEntityC);

        gameLobbyEntityService.createLobby(gameLobbyEntityA);
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityB);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityC);

        GameSessionDto gameSessionDtoA = TestDataUtil.createTestGameSessionDtoA(playerMapper.mapToDto(playerEntityA));
        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isEmpty();

        StompSession session = initStompSession("/topic/lobby-" + gameLobbyDtoA.getId() + "/game-start", messages);
        StompSession session2 = initStompSession("/topic/lobby-list", messages2);
        session.send("/app/game-start", gameLobbyDtoA.getId() + "");

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        String actualResponse2 = messages2.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isPresent();
        assertThat(actualResponse).isEqualTo(gameSessionDtoA.getId() + "");

        assertThat(actualResponse2).isNotNull();

    }


    @Test
    void testThatCreateGameSessionReturnsUpdatedLobbyListToQueue() throws Exception {
        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        GameLobbyEntity gameLobbyEntityA = gameLobbyMapper.mapToEntity(gameLobbyDtoA);
        GameLobbyDto gameLobbyDtoB = TestDataUtil.createTestGameLobbyDtoB();
        GameLobbyEntity gameLobbyEntityB = gameLobbyMapper.mapToEntity(gameLobbyDtoB);

        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());
        gameLobbyEntityB.setLobbyAdminId(playerEntityA.getId());

        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);
        playerEntityService.createPlayer(playerEntityC);

        gameLobbyEntityService.createLobby(gameLobbyEntityA);
        gameLobbyEntityService.createLobby(gameLobbyEntityB);
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isPresent();
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoB.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityB);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityC);

        String playerEntityAColour = playerEntityService.findPlayerById(playerEntityA.getId()).get().getPlayerColour();
        String playerEntityBColour = playerEntityService.findPlayerById(playerEntityB.getId()).get().getPlayerColour();
        String playerEntityCColour = playerEntityService.findPlayerById(playerEntityC.getId()).get().getPlayerColour();

        GameSessionDto gameSessionDtoA = TestDataUtil.createTestGameSessionDtoA(playerMapper.mapToDto(playerEntityA));
        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isEmpty();

        StompSession session = initStompSession("/user/queue/lobby-list-response", messages);
        session.send("/app/game-start", gameLobbyDtoA.getId() + "");

        List<GameLobbyDto> gameLobbyDtoList = new ArrayList<>();
        gameLobbyDtoA.setGameState(GameState.IN_GAME);
        gameLobbyDtoA.setLobbyAdminId(playerEntityA.getId());
        gameLobbyDtoA.setNumPlayers(3);

        gameLobbyDtoA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsEnumListRemoveValue(gameLobbyDtoA.getAvailableColours(), PlayerColour.valueOf(playerEntityAColour)));
        gameLobbyDtoA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsEnumListRemoveValue(gameLobbyDtoA.getAvailableColours(), PlayerColour.valueOf(playerEntityBColour)));
        gameLobbyDtoA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsEnumListRemoveValue(gameLobbyDtoA.getAvailableColours(), PlayerColour.valueOf(playerEntityCColour)));
        gameLobbyDtoList.add(gameLobbyDtoA);

        gameLobbyDtoB.setLobbyAdminId(playerEntityA.getId());
        gameLobbyDtoList.add(gameLobbyDtoB);

        String expectedResponse = objectMapper.writeValueAsString(gameLobbyDtoList);
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isPresent();
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatFindByGameIdQueryInTileDeckEntityExecutesRight() throws Exception {
        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        GameLobbyEntity gameLobbyEntityA = gameLobbyMapper.mapToEntity(gameLobbyDtoA);


        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());

        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);
        playerEntityService.createPlayer(playerEntityC);

        gameLobbyEntityService.createLobby(gameLobbyEntityA);
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityB);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityC);

        GameSessionDto gameSessionDtoA = TestDataUtil.createTestGameSessionDtoA(playerMapper.mapToDto(playerEntityA));
        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isEmpty();

//        Subsribe to the topic for the game-start and get GameSessionEntityId back
        StompSession session = initStompSession("/topic/lobby-" + gameLobbyDtoA.getId() + "/game-start", messages2);
        session.send("/app/game-start", gameLobbyDtoA.getId() + "");

        //        Get the expected GameSessionEntity
        String expectedResponseGameSessionEntity = objectMapper.writeValueAsString(gameSessionDtoA.getId());
        String actualResponseGameSessionEntity = messages2.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isPresent();
        assertThat(actualResponseGameSessionEntity).isEqualTo(expectedResponseGameSessionEntity);

        TileDeckEntity tileDeck = tileDeckRepository.findByGameSessionId(gameSessionDtoA.getId());
        assertThat(tileDeck).isNotNull();
        Long expectedResponse = tileDeck.getGameSession().getId();

        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isPresent();
        assertThat(actualResponseGameSessionEntity).isEqualTo(expectedResponse.toString());
    }

    @Test
    void testThatGetNextPlayerIdAndNextTileIdReturnsTheRightNextPlayerId() throws Exception {
        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        GameLobbyEntity gameLobbyEntityA = gameLobbyMapper.mapToEntity(gameLobbyDtoA);
        GameLobbyDto gameLobbyDtoB = TestDataUtil.createTestGameLobbyDtoB();
        GameLobbyEntity gameLobbyEntityB = gameLobbyMapper.mapToEntity(gameLobbyDtoB);

        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());
        gameLobbyEntityB.setLobbyAdminId(playerEntityA.getId());

        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);
        playerEntityService.createPlayer(playerEntityC);

        gameLobbyEntityService.createLobby(gameLobbyEntityA);
        gameLobbyEntityService.createLobby(gameLobbyEntityB);
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isPresent();
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoB.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityB);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityC);

        String playerEntityAColour = playerEntityService.findPlayerById(playerEntityA.getId()).get().getPlayerColour();
        String playerEntityBColour = playerEntityService.findPlayerById(playerEntityB.getId()).get().getPlayerColour();
        String playerEntityCColour = playerEntityService.findPlayerById(playerEntityC.getId()).get().getPlayerColour();

        GameSessionDto gameSessionDtoA = TestDataUtil.createTestGameSessionDtoA(playerMapper.mapToDto(playerEntityA));
        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isEmpty();

        StompSession session = initStompSession("/user/queue/lobby-list-response", messages);
        initStompSession("/topic/lobby-" + gameLobbyDtoA.getId() + "/game-start", messages2);
        session.send("/app/game-start", gameLobbyDtoA.getId() + "");

        List<GameLobbyDto> gameLobbyDtoList = new ArrayList<>();
        gameLobbyDtoA.setGameState(GameState.IN_GAME);
        gameLobbyDtoA.setLobbyAdminId(playerEntityA.getId());
        gameLobbyDtoA.setNumPlayers(3);

        gameLobbyDtoA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsEnumListRemoveValue(gameLobbyDtoA.getAvailableColours(), PlayerColour.valueOf(playerEntityAColour)));
        gameLobbyDtoA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsEnumListRemoveValue(gameLobbyDtoA.getAvailableColours(), PlayerColour.valueOf(playerEntityBColour)));
        gameLobbyDtoA.setAvailableColours(TestDataUtil.getTestPlayerColoursAsEnumListRemoveValue(gameLobbyDtoA.getAvailableColours(), PlayerColour.valueOf(playerEntityCColour)));
        gameLobbyDtoList.add(gameLobbyDtoA);

        gameLobbyDtoB.setLobbyAdminId(playerEntityA.getId());
        gameLobbyDtoList.add(gameLobbyDtoB);

        String expectedResponse = objectMapper.writeValueAsString(gameLobbyDtoList);
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

//        Get the expected GameSessionEntity
        String expectedResponseGameSessionEntity = objectMapper.writeValueAsString(gameSessionDtoA.getId());
        String actualResponseGameSessionEntity = messages2.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isPresent();
        assertThat(actualResponse).isEqualTo(expectedResponse);
        assertThat(actualResponseGameSessionEntity).isEqualTo(expectedResponseGameSessionEntity);

        String gameSessionId = gameSessionDtoA.getId().toString();
        StompSession session2 = initStompSession("/topic/game-session-" + gameSessionId + "/next-turn-response", messages3);
        session2.send("/app/next-turn", gameSessionDtoA.getId() + "");

        Long expectedResponseNextPlayerId = playerEntityB.getId();
        String actualResponseNextTurnDto = messages3.poll(1, TimeUnit.SECONDS);
        NextTurnDto result = objectMapper.readValue(actualResponseNextTurnDto, NextTurnDto.class);

        assertThat(result.getPlayerId()).isEqualTo(expectedResponseNextPlayerId);
    }

    @Test
    void testThatGetNextPlayerIdAndNextTileIdReturnsTheRightNextTile() throws Exception {
        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        GameLobbyEntity gameLobbyEntityA = gameLobbyMapper.mapToEntity(gameLobbyDtoA);


        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());

        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);
        playerEntityService.createPlayer(playerEntityC);

        gameLobbyEntityService.createLobby(gameLobbyEntityA);
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityB);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityC);

        GameSessionDto gameSessionDtoA = TestDataUtil.createTestGameSessionDtoA(playerMapper.mapToDto(playerEntityA));
        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isEmpty();

        StompSession session = initStompSession("/user/queue/lobby-list-response", messages);
        initStompSession("/topic/lobby-" + gameLobbyDtoA.getId() + "/game-start", messages2);
        session.send("/app/game-start", gameLobbyDtoA.getId() + "");

//        Get the expected GameSessionEntity
        String expectedResponseGameSessionEntity = objectMapper.writeValueAsString(gameSessionDtoA.getId());
        String actualResponseGameSessionEntity = messages2.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isPresent();

//        Get the TileDeck from the gameSession
        TileDeckEntity tileDeck = tileDeckRepository.findByGameSessionId(gameSessionDtoA.getId());
        assertThat(tileDeck).isNotNull();
        assertFalse(tileDeck.getTileId().contains(0L), "TileDeck should not contain 0L as a value");

        List<Long> firstTile = tileDeck.getTileId();

//      Subscribe to the topic for the next turn
        String gameSessionId = gameSessionDtoA.getId().toString();
        StompSession session2 = initStompSession("/topic/game-session-" + gameSessionId + "/next-turn-response", messages3);
        session2.send("/app/next-turn", gameSessionDtoA.getId() + "");
//
//      Receive the next turn dto from the endpoint
        Long expectedFirstTile = firstTile.get(0);
        String actualResponseNextTurnDto = messages3.poll(1, TimeUnit.SECONDS);
        NextTurnDto result = objectMapper.readValue(actualResponseNextTurnDto, NextTurnDto.class);
//
//      Extract the next player id
        assertThat(result.getTileId()).isEqualTo(expectedFirstTile);
    }

    @Test
    void testThatGameInFinishedStateThrowsException() throws Exception {
        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        GameLobbyEntity gameLobbyEntityA = gameLobbyMapper.mapToEntity(gameLobbyDtoA);

        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());

        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);
        playerEntityService.createPlayer(playerEntityC);

        gameLobbyEntityService.createLobby(gameLobbyEntityA);
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityB);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityC);

        GameSessionDto gameSessionDtoA = TestDataUtil.createTestGameSessionDtoA(playerMapper.mapToDto(playerEntityA));
        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isEmpty();

//        Create gameSessionEntity
        StompSession session = initStompSession("/user/queue/lobby-list-response", messages);
        initStompSession("/topic/lobby-" + gameLobbyDtoA.getId() + "/game-start", messages2);
        session.send("/app/game-start", gameLobbyDtoA.getId() + "");

//        Get the expected GameSessionEntity
        String expectedResponseGameSessionEntity = objectMapper.writeValueAsString(gameSessionDtoA.getId());
        String actualResponseGameSessionEntity = messages2.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isPresent();
        assertThat(actualResponseGameSessionEntity).isEqualTo(expectedResponseGameSessionEntity);

//        Set the gameSessionEntity to finished
        GameSessionEntity gameSession = gameSessionEntityService.findById(gameSessionDtoA.getId()).get();
        gameSession.setGameState(GameState.FINISHED.name());
        gameSessionEntityRepository.save(gameSession);

        StompSession session2 = initStompSession("/user/queue/errors", messages3);
        session2.send("/app/next-turn", gameSessionDtoA.getId() + "");

        String expectedResponse = "ERROR: " + "Game is already finished.";
        String actualResponse = messages3.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatGameSessionControllerThrowsExceptionIfTheGameSessionIsDeleted() throws Exception {
        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        GameLobbyEntity gameLobbyEntityA = gameLobbyMapper.mapToEntity(gameLobbyDtoA);

        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());

        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);
        playerEntityService.createPlayer(playerEntityC);

        gameLobbyEntityService.createLobby(gameLobbyEntityA);
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityB);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityC);

        GameSessionDto gameSessionDtoA = TestDataUtil.createTestGameSessionDtoA(playerMapper.mapToDto(playerEntityA));
        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isEmpty();

//        Create gameSessionEntity
        StompSession session = initStompSession("/user/queue/lobby-list-response", messages);
        initStompSession("/topic/lobby-" + gameLobbyDtoA.getId() + "/game-start", messages2);
        session.send("/app/game-start", gameLobbyDtoA.getId() + "");

//        Get the expected GameSessionEntity
        String expectedResponseGameSessionEntity = objectMapper.writeValueAsString(gameSessionDtoA.getId());
        String actualResponseGameSessionEntity = messages2.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isPresent();
        assertThat(actualResponseGameSessionEntity).isEqualTo(expectedResponseGameSessionEntity);

        StompSession session2 = initStompSession("/user/queue/errors", messages3);

//        Delete the gameSessionEntity
        gameSessionEntityRepository.deleteById(gameSessionDtoA.getId());
        session2.send("/app/next-turn", gameSessionDtoA.getId() + "");

        String expectedResponse = "ERROR: " + "GameSession not found.";
        String actualResponse = messages3.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatGameSessionControllerThrowsExceptionIfTheWrongGameSessionIdIsSupplied() throws Exception {
        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        GameLobbyEntity gameLobbyEntityA = gameLobbyMapper.mapToEntity(gameLobbyDtoA);

        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());

        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);
        playerEntityService.createPlayer(playerEntityC);

        gameLobbyEntityService.createLobby(gameLobbyEntityA);
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityB);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityC);

        GameSessionDto gameSessionDtoA = TestDataUtil.createTestGameSessionDtoA(playerMapper.mapToDto(playerEntityA));
        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isEmpty();

//        Create gameSessionEntity
        StompSession session = initStompSession("/user/queue/lobby-list-response", messages);
        initStompSession("/topic/lobby-" + gameLobbyDtoA.getId() + "/game-start", messages2);
        session.send("/app/game-start", gameLobbyDtoA.getId() + "");

//        Get the expected GameSessionEntity
        String expectedResponseGameSessionEntity = objectMapper.writeValueAsString(gameSessionDtoA.getId());
        String actualResponseGameSessionEntity = messages2.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isPresent();
        assertThat(actualResponseGameSessionEntity).isEqualTo(expectedResponseGameSessionEntity);

        StompSession session2 = initStompSession("/user/queue/errors", messages3);

//        Provide the gameSessionEntityId that does not exist
        session2.send("/app/next-turn", 2L + "");

        String expectedResponse = "ERROR: " + "GameSession not found.";
        String actualResponse = messages3.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatGameTerminatesIfTheDeckIsEmpty() throws Exception {
        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        GameLobbyEntity gameLobbyEntityA = gameLobbyMapper.mapToEntity(gameLobbyDtoA);

        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());

        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);
        playerEntityService.createPlayer(playerEntityC);

        gameLobbyEntityService.createLobby(gameLobbyEntityA);
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityB);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityC);

        GameSessionDto gameSessionDtoA = TestDataUtil.createTestGameSessionDtoA(playerMapper.mapToDto(playerEntityA));
        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isEmpty();

//        Create gameSessionEntity
        StompSession session = initStompSession("/user/queue/lobby-list-response", messages);
        initStompSession("/topic/lobby-" + gameLobbyDtoA.getId() + "/game-start", messages2);
        session.send("/app/game-start", gameLobbyDtoA.getId() + "");

//        Get the expected GameSessionEntity
        String expectedResponseGameSessionEntity = objectMapper.writeValueAsString(gameSessionDtoA.getId());
        String actualResponseGameSessionEntity = messages2.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isPresent();
        assertThat(actualResponseGameSessionEntity).isEqualTo(expectedResponseGameSessionEntity);

//        Find the deck and set it to empty deck
        TileDeckEntity tileDeck = tileDeckRepository.findByGameSessionId(gameSessionDtoA.getId());
        tileDeck.setTileId(new ArrayList<>());
        tileDeckRepository.save(tileDeck);

        String gameSessionId = gameSessionDtoA.getId().toString();
        StompSession session2 = initStompSession("/topic/game-session-" + gameSessionId + "/game-finished", messages3);
        session2.send("/app/next-turn", gameSessionDtoA.getId() + "");

        String expectedResponse = "FINISHED";
        String actualResponse = messages3.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }
    @Test
    void testThatGameTerminatesIfTheDeckIsEmptyAndSendsTheMessageToTheTopic() throws Exception {
        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        GameLobbyEntity gameLobbyEntityA = gameLobbyMapper.mapToEntity(gameLobbyDtoA);

        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());

        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);
        playerEntityService.createPlayer(playerEntityC);

        gameLobbyEntityService.createLobby(gameLobbyEntityA);
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityB);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityC);

        GameSessionDto gameSessionDtoA = TestDataUtil.createTestGameSessionDtoA(playerMapper.mapToDto(playerEntityA));
        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isEmpty();

//        Create gameSessionEntity
        StompSession session = initStompSession("/user/queue/lobby-list-response", messages);
        initStompSession("/topic/lobby-" + gameLobbyDtoA.getId() + "/game-start", messages2);
        session.send("/app/game-start", gameLobbyDtoA.getId() + "");

//        Get the expected GameSessionEntity
        String expectedResponseGameSessionEntity = objectMapper.writeValueAsString(gameSessionDtoA.getId());
        String actualResponseGameSessionEntity = messages2.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isPresent();
        assertThat(actualResponseGameSessionEntity).isEqualTo(expectedResponseGameSessionEntity);

//        Find the deck and set it to empty deck
        TileDeckEntity tileDeck = tileDeckRepository.findByGameSessionId(gameSessionDtoA.getId());
        tileDeck.setTileId(new ArrayList<>());
        tileDeckRepository.save(tileDeck);

        StompSession session2 = initStompSession("/topic/game-session-" + gameLobbyDtoA.getId() + "/game-finished", messages3);
//        Subscribe to topic for the game state finish
        StompSession session3 = initStompSession("/topic/game-session-" + gameLobbyDtoA.getId() + "/game-finished", messages4);
//        Try to draw a tile
        session2.send("/app/next-turn", gameSessionDtoA.getId() + "");

        String expectedResponse = "FINISHED";
        String actualResponse = messages3.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo(expectedResponse);

        String expectedResponseTopic = "FINISHED";
        String actualResponseTopic = messages4.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponseTopic).isEqualTo(expectedResponseTopic);
    }

    @Test
    void testThatGameTerminatesWhenTheDeckIsEmptyAfterDrawingLastCard() throws Exception {
        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        GameLobbyEntity gameLobbyEntityA = gameLobbyMapper.mapToEntity(gameLobbyDtoA);

        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());

        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);
        playerEntityService.createPlayer(playerEntityC);

        gameLobbyEntityService.createLobby(gameLobbyEntityA);
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityB);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityC);

        GameSessionDto gameSessionDtoA = TestDataUtil.createTestGameSessionDtoA(playerMapper.mapToDto(playerEntityA));
        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isEmpty();

//        Create gameSessionEntity
        StompSession session = initStompSession("/user/queue/lobby-list-response", messages);
        initStompSession("/topic/lobby-" + gameLobbyDtoA.getId() + "/game-start", messages2);
        session.send("/app/game-start", gameLobbyDtoA.getId() + "");

//        Get the expected GameSessionEntity
        String expectedResponseGameSessionEntity = objectMapper.writeValueAsString(gameSessionDtoA.getId());
        String actualResponseGameSessionEntity = messages2.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isPresent();
        assertThat(actualResponseGameSessionEntity).isEqualTo(expectedResponseGameSessionEntity);

//        Find the deck and set it to the deck with one last tile
        TileDeckEntity tileDeck = tileDeckRepository.findByGameSessionId(gameSessionDtoA.getId());
        List<Long> lastTile = new ArrayList<>();
        lastTile.add(71L);
        tileDeck.setTileId(lastTile);
        tileDeckRepository.save(tileDeck);

        String gameSessionId = gameSessionDtoA.getId().toString();
        StompSession session2 = initStompSession("/topic/game-session-" + gameSessionId + "/next-turn-response", messages3);
        StompSession session3 = initStompSession("/topic/game-session-" + gameSessionId + "/game-finished", messages4);
        session2.send("/app/next-turn", gameSessionDtoA.getId() + "");

        String actualResponseBeforeDeckEmpty = messages3.poll(1, TimeUnit.SECONDS);
        NextTurnDto resultBeforeDeckEmpty = objectMapper.readValue(actualResponseBeforeDeckEmpty, NextTurnDto.class);
        NextTurnDto expectedBeforeDeckEmpty = new NextTurnDto(playerEntityB.getId(), 71L);

        assertThat(resultBeforeDeckEmpty).isEqualTo(expectedBeforeDeckEmpty);

//        Now the deck is empty
        tileDeck = tileDeckRepository.findByGameSessionId(gameSessionDtoA.getId());
        List<Long> expectedEmptyDeck = new ArrayList<>();
        List<Long> actualEmptyDeck = tileDeck.getTileId();

        assertThat(actualEmptyDeck).isEqualTo(expectedEmptyDeck);

//        Draw another tile
        session3.send("/app/next-turn", gameSessionDtoA.getId() + "");

//        Now we check if the user gets the right gameState message string
        String expectedResponse = "FINISHED";
        String actualResponse = messages4.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo(expectedResponse);
//
//        Now the gameState should be set to finished
        String expectedGameState = "FINISHED";
        String actualGameState = gameSessionEntityService.findById(gameSessionDtoA.getId()).get().getGameState();

        assertThat(actualGameState).isEqualTo(expectedGameState);
    }


    @Test
    void testThatCreateGameSessionWhenGivenInvalidLobbyIdFails() throws Exception {
        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        GameLobbyEntity gameLobbyEntityA = gameLobbyMapper.mapToEntity(gameLobbyDtoA);


        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());

        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);

        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isEmpty();

        GameSessionDto gameSessionDtoA = TestDataUtil.createTestGameSessionDtoA(playerMapper.mapToDto(playerEntityA));
        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isEmpty();

        StompSession session = initStompSession("/user/queue/errors", messages);
        session.send("/app/game-start", gameLobbyDtoA.getId() + "");

        String expectedResponse = "ERROR: " + ErrorCode.ERROR_1003.getCode();
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionDtoA.getId())).isEmpty();
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    // test forwarding of a placed GameBoardTileDto to all players in the game session
    @Test
    void testThatPlaceTileForwardsPlacedTileDtoToAllPlayers() throws Exception {

        GameLobbyEntity testGameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        //gameLobbyEntityService.createLobby(testGameLobbyEntityA);

        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(testGameLobbyEntityA);
        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(testGameLobbyEntityA);
        PlayerEntity testPlayerEntityC = TestDataUtil.createTestPlayerEntityC(testGameLobbyEntityA);

        //playerEntityService.createPlayer(testPlayerEntityA);
        //playerEntityService.createPlayer(testPlayerEntityB);
        //playerEntityService.createPlayer(testPlayerEntityC);

        GameSessionEntity gameSessionEntity = TestDataUtil.createTestGameSessionEntityWith3Players();
        //GameSessionEntity databaseGameSession = gameSessionEntityService.createGameSession(testGameLobbyEntityA.getId());

        StompSession session = initStompSession("/topic/game-session-" + gameSessionEntity.getId() + "/tile", messages);
        StompSession session2 = initStompSession("/topic/game-session-" + gameSessionEntity.getId() + "/tile", messages2);

        PlacedTileDto placedTileDto = TestDataUtil.createTestPlacedTileDto(gameSessionEntity.getId());

        session.send("/app/place-tile", objectMapper.writeValueAsString(placedTileDto));

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        String actualResponse2 = messages2.poll(1, TimeUnit.SECONDS);


        assertThat(actualResponse).isEqualTo(objectMapper.writeValueAsString(placedTileDto));
        assertThat(actualResponse2).isEqualTo(objectMapper.writeValueAsString(placedTileDto));
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

    @Test
    void testOnePlayerLeaveWhenMoreThanTwoPlayersRemain() throws Exception {
        // Create GameLobbyEntity
        GameLobbyEntity gameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();

        // create 3 player entities
        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());

        // save player entities to database
        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);
        playerEntityService.createPlayer(playerEntityC);
        // save gameLobbyEntity to database
        gameLobbyEntityService.createLobby(gameLobbyEntityA);

        GameLobbyDto gameLobbyDtoA = gameLobbyMapper.mapToDto(gameLobbyEntityA);
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityB);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityC);

        // save gameSession to database
        GameSessionEntity gameSessionEntityWith3Players = TestDataUtil.createTestGameSessionEntityWith3Players();
        gameSessionEntityService.createGameSession(gameLobbyEntityA.getId());

        playerEntityA.setGameSessionEntity(gameSessionEntityWith3Players);
        playerEntityB.setGameSessionEntity(gameSessionEntityWith3Players);
        playerEntityC.setGameSessionEntity(gameSessionEntityWith3Players);

        assertThat(gameSessionEntityService.findById(gameSessionEntityWith3Players.getId())).isPresent();

        StompSession session = initStompSession("/user/queue/response", messages);
        StompSession session2 = initStompSession("/topic/gamesession-" + gameSessionEntityWith3Players.getId() + "/update", messages2);

        session.send("/app/player-leave-gamesession", objectMapper.writeValueAsString(playerMapper.mapToDto(playerEntityA)));

        gameSessionEntityWith3Players.setNumPlayers(2);
        gameSessionEntityWith3Players.getPlayerIds().remove(playerEntityA.getId());


        GameSessionDto testGamesessionDtoA = gameSessionMapper.mapToDto(gameSessionEntityWith3Players);
        String payload = objectMapper.writeValueAsString(testGamesessionDtoA);

        session2.send("/topic/gamesession-" + gameSessionEntityWith3Players.getId() + "/update", payload);


        String expectedResponse = objectMapper.writeValueAsString(gameSessionMapper.mapToDto(gameSessionEntityWith3Players));

        String actualResponse = messages2.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionEntityWith3Players.getId())).isPresent();
        assertThat(actualResponse).isEqualTo(expectedResponse);


    }

    @Test
    void testTwoPlayersLeaveWhenMoreThanTwoPlayersRemain() throws Exception {
        // Create GameLobbyEntity
        GameLobbyEntity gameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();

        // create 3 player entities
        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);
        gameLobbyEntityA.setLobbyAdminId(playerEntityA.getId());

        // save player entities to database
        playerEntityService.createPlayer(playerEntityA);
        playerEntityService.createPlayer(playerEntityB);
        playerEntityService.createPlayer(playerEntityC);
        // save gameLobbyEntity to database
        gameLobbyEntityService.createLobby(gameLobbyEntityA);

        GameLobbyDto gameLobbyDtoA = gameLobbyMapper.mapToDto(gameLobbyEntityA);
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId())).isPresent();

        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityA);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityB);
        playerEntityService.joinLobby(gameLobbyEntityA.getId(), playerEntityC);

        // save gameSession to database
        GameSessionEntity gameSessionEntityWith3Players = TestDataUtil.createTestGameSessionEntityWith3Players();
        gameSessionEntityService.createGameSession(gameLobbyEntityA.getId());

        playerEntityA.setGameSessionEntity(gameSessionEntityWith3Players);
        playerEntityB.setGameSessionEntity(gameSessionEntityWith3Players);
        playerEntityC.setGameSessionEntity(gameSessionEntityWith3Players);

        assertThat(gameSessionEntityService.findById(gameSessionEntityWith3Players.getId())).isPresent();

        StompSession session = initStompSession("/user/queue/response", messages);
        StompSession session2 = initStompSession("/topic/gamesession-" + gameSessionEntityWith3Players.getId() + "/update", messages2);

        session.send("/app/player-leave-gamesession", objectMapper.writeValueAsString(playerMapper.mapToDto(playerEntityA)));
        gameSessionEntityWith3Players.setNumPlayers(1);
        gameSessionEntityWith3Players.getPlayerIds().remove(playerEntityA.getId());
        session.send("/app/player-leave-gamesession", objectMapper.writeValueAsString(playerMapper.mapToDto(playerEntityB)));

        gameSessionEntityWith3Players.setNumPlayers(1);
        gameSessionEntityWith3Players.getPlayerIds().remove(playerEntityB.getId());

        GameSessionDto testGamesessionDtoA = gameSessionMapper.mapToDto(gameSessionEntityWith3Players);
        String payload = objectMapper.writeValueAsString(testGamesessionDtoA);

        session2.send("/topic/gamesession-" + gameSessionEntityWith3Players.getId() + "/update", payload);

        String expectedResponse = objectMapper.writeValueAsString(gameSessionMapper.mapToDto(gameSessionEntityWith3Players));

        String actualResponse = messages2.poll(1, TimeUnit.SECONDS);

        assertThat(gameSessionEntityService.findById(gameSessionEntityWith3Players.getId())).isPresent();
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }



}
