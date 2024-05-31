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

    private final ObjectMapper objectMapper;
    private final GameLobbyEntityService gameLobbyEntityService;
    private final PlayerEntityService playerEntityService;
    private final PlayerMapper playerMapper;
    private final GameLobbyMapper gameLobbyMapper;

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
        List<PlayerDto> playerDtos = new ArrayList<>();
        if (gameLobbyEntityService.findById(gameLobbyId).isEmpty()){
            return playerDtos;
        }

        List<PlayerEntity> playerEntityList = playerEntityService.getAllPlayersForLobby(gameLobbyId);

        for (PlayerEntity playerEntity : playerEntityList) {
            playerDtos.add(playerMapper.mapToDto(playerEntity));
        }
        return playerDtos;
    }

    @Test
    void testThatCreateLobbyReturnsCreatedGameLobbyDtoToQueue() throws Exception {
        StompSession session = initStompSession("/user/queue/response", messages);

        PlayerEntity playerEntity = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(playerEntity);

        PlayerDto playerDto = playerMapper.mapToDto(playerEntity);
        GameLobbyDto gameLobbyDto = TestDataUtil.createTestGameLobbyDtoA();
        gameLobbyDto.setLobbyAdminId(playerEntity.getId());

        String playerDtoJson = objectMapper.writeValueAsString(playerDto);
        String gameLobbyDtoJson = objectMapper.writeValueAsString(gameLobbyDto);

        String payload = gameLobbyDtoJson + "|" + playerDtoJson;

        assertThat(gameLobbyEntityService.findById(gameLobbyDto.getId()).isEmpty()).isTrue();
        assertThat(playerEntityService.findPlayerById(playerDto.getId()).isPresent()).isTrue();
        session.send("/app/lobby-create", payload);

        gameLobbyDto.setNumPlayers(gameLobbyDto.getNumPlayers() + 1);
        //gameLobbyDto.setAvailableColours(TestDataUtil.getTestPlayerColoursAsEnumList());
        playerDto.setGameLobbyId(gameLobbyDto.getId());

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        Optional<PlayerEntity> updatedPlayerEntityOptional = playerEntityService.findPlayerById(playerDto.getId());
        assertTrue(updatedPlayerEntityOptional.isPresent());
        PlayerEntity updatedPlayerEntity = updatedPlayerEntityOptional.get();

        // Randomness-Workaround: Get colour from player, set all colours to lobby and remove the color from the list
        String playerColour = updatedPlayerEntity.getPlayerColour();
        PlayerColour playerColorEnum = PlayerColour.valueOf(playerColour);
        gameLobbyDto.setAvailableColours(TestDataUtil.getTestPlayerColoursAsEnumListRemoveValue(playerColorEnum));
        String expectedResponse = objectMapper.writeValueAsString(gameLobbyDto);

        assertThat(updatedPlayerEntity.getGameLobbyEntity()).isEqualTo(gameLobbyMapper.mapToEntity(gameLobbyDto));

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatCreateLobbyReturnsUpdatedLobbyListToTopic() throws Exception {
        StompSession session = initStompSession("/topic/lobby-list", messages);

        PlayerEntity playerEntity = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(playerEntity);

        PlayerDto playerDto = playerMapper.mapToDto(playerEntity);
        GameLobbyDto gameLobbyDto = TestDataUtil.createTestGameLobbyDtoA();
        gameLobbyDto.setLobbyAdminId(playerEntity.getId());

        String playerDtoJson = objectMapper.writeValueAsString(playerDto);
        String gameLobbyDtoJson = objectMapper.writeValueAsString(gameLobbyDto);

        String payload = gameLobbyDtoJson + "|" + playerDtoJson;

        assertThat(gameLobbyEntityService.findById(gameLobbyDto.getId()).isEmpty()).isTrue();
        assertThat(playerEntityService.findPlayerById(playerDto.getId()).isPresent()).isTrue();
        session.send("/app/lobby-create", payload);

        gameLobbyDto.setNumPlayers(gameLobbyDto.getNumPlayers() + 1);
        playerDto.setGameLobbyId(gameLobbyDto.getId());

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        Optional<PlayerEntity> updatedPlayerEntityOptional = playerEntityService.findPlayerById(playerDto.getId());
        assertTrue(updatedPlayerEntityOptional.isPresent());
        PlayerEntity updatedPlayerEntity = updatedPlayerEntityOptional.get();

        // Randomness-Workaround: Get colour from player, set all colours to lobby and remove the color from the list
        String playerColour = updatedPlayerEntity.getPlayerColour();
        PlayerColour playerColorEnum = PlayerColour.valueOf(playerColour);
        gameLobbyDto.setAvailableColours(TestDataUtil.getTestPlayerColoursAsEnumListRemoveValue(playerColorEnum));

        assertThat(updatedPlayerEntity.getGameLobbyEntity()).isEqualTo(gameLobbyMapper.mapToEntity(gameLobbyDto));

        String expectedResponse = objectMapper.writeValueAsString(getGameLobbyDtoList());

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatCreateLobbyReturnsUpdatedPlayerListToTopic() throws Exception {

        PlayerEntity playerEntity = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(playerEntity);

        PlayerDto playerDto = playerMapper.mapToDto(playerEntity);
        GameLobbyDto gameLobbyDto = TestDataUtil.createTestGameLobbyDtoA();
        gameLobbyDto.setLobbyAdminId(playerEntity.getId());

        StompSession session = initStompSession("/topic/lobby-" + gameLobbyDto.getId(), messages);

        String playerDtoJson = objectMapper.writeValueAsString(playerDto);
        String gameLobbyDtoJson = objectMapper.writeValueAsString(gameLobbyDto);

        String payload = gameLobbyDtoJson + "|" + playerDtoJson;

        assertThat(gameLobbyEntityService.findById(gameLobbyDto.getId()).isEmpty()).isTrue();
        assertThat(playerEntityService.findPlayerById(playerDto.getId()).isPresent()).isTrue();
        session.send("/app/lobby-create", payload);

        gameLobbyDto.setNumPlayers(gameLobbyDto.getNumPlayers() + 1);
        playerDto.setGameLobbyId(gameLobbyDto.getId());

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        Optional<PlayerEntity> updatedPlayerEntityOptional = playerEntityService.findPlayerById(playerDto.getId());
        assertTrue(updatedPlayerEntityOptional.isPresent());
        PlayerEntity updatedPlayerEntity = updatedPlayerEntityOptional.get();

        // Randomness-Workaround: Get colour from player, set all colours to lobby and remove the color from the list
        String playerColour = updatedPlayerEntity.getPlayerColour();
        PlayerColour playerColorEnum = PlayerColour.valueOf(playerColour);
        gameLobbyDto.setAvailableColours(TestDataUtil.getTestPlayerColoursAsEnumListRemoveValue(playerColorEnum));

        assertThat(updatedPlayerEntity.getGameLobbyEntity()).isEqualTo(gameLobbyMapper.mapToEntity(gameLobbyDto));

        String expectedResponse = objectMapper.writeValueAsString(getPlayerDtosInLobbyList(gameLobbyDto.getId()));

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatCreateLobbyReturnsUpdatedPlayerToTopic() throws Exception {
        StompSession session = initStompSession("/topic/lobby-creator", messages);

        PlayerEntity playerEntity = TestDataUtil.createTestPlayerEntityA(null);
        playerEntityService.createPlayer(playerEntity);

        PlayerDto playerDto = playerMapper.mapToDto(playerEntity);
        GameLobbyDto gameLobbyDto = TestDataUtil.createTestGameLobbyDtoA();
        gameLobbyDto.setLobbyAdminId(playerEntity.getId());

        String playerDtoJson = objectMapper.writeValueAsString(playerDto);
        String gameLobbyDtoJson = objectMapper.writeValueAsString(gameLobbyDto);

        String payload = gameLobbyDtoJson + "|" + playerDtoJson;

        assertThat(gameLobbyEntityService.findById(gameLobbyDto.getId()).isEmpty()).isTrue();
        assertThat(playerEntityService.findPlayerById(playerDto.getId()).isPresent()).isTrue();
        session.send("/app/lobby-create", payload);

        gameLobbyDto.setNumPlayers(gameLobbyDto.getNumPlayers() + 1);
        playerDto.setGameLobbyId(gameLobbyDto.getId());

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        Optional<PlayerEntity> updatedPlayerEntityOptional = playerEntityService.findPlayerById(playerDto.getId());
        assertTrue(updatedPlayerEntityOptional.isPresent());
        PlayerEntity updatedPlayerEntity = updatedPlayerEntityOptional.get();

        // Randomness-Workaround: Get colour from player entity and set it to player dto
        String playerColour = updatedPlayerEntity.getPlayerColour();
        PlayerColour playerColorEnum = PlayerColour.valueOf(playerColour);
        playerDto.setPlayerColour(playerColorEnum);
        String expectedResponse = objectMapper.writeValueAsString(playerDto);

        assertThat(updatedPlayerEntity).isEqualTo(playerMapper.mapToEntity(playerDto));

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatCreateLobbyWithDuplicateLobbyIdFails() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        PlayerEntity playerEntity = TestDataUtil.createTestPlayerEntityA(null);
        PlayerDto playerDto = playerMapper.mapToDto(playerEntity);
        GameLobbyDto gameLobbyDto = TestDataUtil.createTestGameLobbyDtoA();
        gameLobbyEntityService.createLobby(gameLobbyMapper.mapToEntity(gameLobbyDto));
        assertThat(gameLobbyEntityService.findById(gameLobbyDto.getId()).isPresent()).isTrue();

        String playerDtoJson = objectMapper.writeValueAsString(playerDto);
        String gameLobbyDtoJson = objectMapper.writeValueAsString(gameLobbyDto);

        String payload = gameLobbyDtoJson + "|" + playerDtoJson;
        session.send("/app/lobby-create", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_1001.getCode());
    }

    @Test
    void testThatCreateLobbyWithDuplicateLobbyNameFails() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        PlayerEntity playerEntity = TestDataUtil.createTestPlayerEntityA(null);
        PlayerDto playerDto = playerMapper.mapToDto(playerEntity);

        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        gameLobbyEntityService.createLobby(gameLobbyMapper.mapToEntity(gameLobbyDtoA));
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId()).isPresent()).isTrue();

        GameLobbyDto gameLobbyDtoB = TestDataUtil.createTestGameLobbyDtoB();
        gameLobbyDtoB.setName(gameLobbyDtoA.getName());

        String playerDtoJson = objectMapper.writeValueAsString(playerDto);
        String gameLobbyDtoJson = objectMapper.writeValueAsString(gameLobbyDtoB);

        String payload = gameLobbyDtoJson + "|" + playerDtoJson;
        session.send("/app/lobby-create", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_1002.getCode());
    }

    @Test
    void testThatCreateLobbyWithInvalidPlayerIdFails() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        PlayerEntity playerEntity = TestDataUtil.createTestPlayerEntityA(null);
        PlayerDto playerDto = playerMapper.mapToDto(playerEntity);

        GameLobbyDto gameLobbyDtoA = TestDataUtil.createTestGameLobbyDtoA();
        gameLobbyDtoA.setLobbyAdminId(playerEntity.getId());
        assertThat(gameLobbyEntityService.findById(gameLobbyDtoA.getId()).isEmpty()).isTrue();

        String playerDtoJson = objectMapper.writeValueAsString(playerDto);
        String gameLobbyDtoJson = objectMapper.writeValueAsString(gameLobbyDtoA);

        String payload = gameLobbyDtoJson + "|" + playerDtoJson;
        session.send("/app/lobby-create", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2001.getCode());
    }

    @Test
    void testThatCreateLobbyWithFaultyGameLobbyDtoFails() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        String invalidGameLobbyDto = "not valid JSON";

        PlayerDto playerDto = TestDataUtil.createTestPlayerDtoA(null);
        String payload = invalidGameLobbyDto + "|" +  objectMapper.writeValueAsString(playerDto);

        session.send("/app/lobby-create", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_1006.getCode());
    }

    @Test
    void testThatCreateLobbyWithFaultyPlayerDtoFails() throws Exception {
        StompSession session = initStompSession("/user/queue/errors", messages);

        GameLobbyDto gameLobbyDto = TestDataUtil.createTestGameLobbyDtoA();

        String invalidPlayerDto = "not valid JSON";
        String payload = objectMapper.writeValueAsString(gameLobbyDto) + "|" +  invalidPlayerDto;

        session.send("/app/lobby-create", payload);

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_2004.getCode());
    }

    @Test
    void testThatUpdateLobbyNameReturnsUpdatedGameLobbyDto() throws Exception {
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
    void testThatUpdateLobbyNameFails() throws Exception {
        StompSession session = initStompSessionWithSecondTopic("/topic/game-lobby-response", "/user/queue/errors", messages);

        GameLobbyDto gameLobbyDto = TestDataUtil.createTestGameLobbyDtoA();
        gameLobbyEntityService.createLobby(gameLobbyMapper.mapToEntity(gameLobbyDto));

        gameLobbyDto.setId(10L);
        gameLobbyDto.setName("lobbyB");
        session.send("/app/lobby-name-update", objectMapper.writeValueAsString(gameLobbyDto));

        String actualResponse = messages.poll(1, TimeUnit.SECONDS);
        assertThat(actualResponse).isEqualTo("ERROR: " + ErrorCode.ERROR_1003.getCode());
    }

    @Test
    void testThatGetListOfLobbiesReturnsListOfGameLobbyDtos() throws Exception {
        StompSession session = initStompSession("/user/queue/lobby-list-response", messages);

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
    void testThatGetListOfLobbiesReturnsEmptyListOfGameLobbyDtos() throws Exception {
        StompSession session = initStompSession("/user/queue/lobby-list-response", messages);

        List<GameLobbyDto> gameLobbyDtoList = new ArrayList<>();

        session.send("/app/lobby-list", "");
        String expectedResponse = objectMapper.writeValueAsString(gameLobbyDtoList);
        String actualResponse = messages.poll(1, TimeUnit.SECONDS);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void testThatDeleteLobbyReturnsSuccessMessage() throws Exception {
        StompSession session = initStompSession("/user/queue/lobby-response", messages);

        GameLobbyDto gameLobbyDto = TestDataUtil.createTestGameLobbyDtoA();

        gameLobbyEntityService.createLobby(gameLobbyMapper.mapToEntity(gameLobbyDto));
        assertThat(gameLobbyEntityService.findById(gameLobbyDto.getId()).isPresent()).isTrue();

        session.send("/app/lobby-delete", gameLobbyDto.getId() + "");

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

    public StompSession initStompSessionWithSecondTopic(String topic, String errorTopic, BlockingQueue<String> messages) throws Exception {
        StompSession session = initStompSession(topic, messages);
        session.subscribe(errorTopic, new StompFrameHandlerClientImpl(messages));

        return session;
    }

}
