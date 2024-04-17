package at.aau.serg.websocketserver.mapper;

import at.aau.serg.websocketserver.TestDataUtil;
import at.aau.serg.websocketserver.domain.dto.PlayerDto;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.service.GameLobbyEntityService;
import at.aau.serg.websocketserver.service.PlayerEntityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import at.aau.serg.websocketserver.TestDataUtil;
import at.aau.serg.websocketserver.demo.websocket.StompFrameHandlerClientImpl;
import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketserver.domain.dto.PlayerDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
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
public class PlayerMapperTest {

    private final ObjectMapper objectMapper;
    private final PlayerEntityService playerEntityService;
    private final GameLobbyEntityService gameLobbyEntityService;
    private final PlayerMapper playerMapper;
    private final GameLobbyMapper gameLobbyMapper;

    @Autowired
    public PlayerMapperTest(ObjectMapper objectMapper, PlayerEntityService playerEntityService, GameLobbyEntityService gameLobbyEntityService, PlayerMapper playerMapper, GameLobbyMapper gameLobbyMapper) {
        this.objectMapper = objectMapper;
        this.playerEntityService = playerEntityService;
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.playerMapper = playerMapper;
        this.gameLobbyMapper = gameLobbyMapper;
    }

    @Test
    void testEntityToDtoMapper() {
        PlayerEntity testPlayerEntityA = TestDataUtil.createTestPlayerEntityA(TestDataUtil.createTestGameLobbyEntityA());
        PlayerDto testPlayerDtoA = playerMapper.mapToDto(testPlayerEntityA);
        assertThat(testPlayerDtoA.getGameLobbyId()).isEqualTo(testPlayerEntityA.getGameLobbyEntity().getId());

        PlayerEntity testPlayerEntityB = TestDataUtil.createTestPlayerEntityB(TestDataUtil.createTestGameLobbyEntityB());
        PlayerDto testPlayerDtoB = playerMapper.mapToDto(testPlayerEntityB);
        assertThat(testPlayerDtoB.getGameLobbyId()).isEqualTo(testPlayerEntityB.getGameLobbyEntity().getId());
    }

    @Test
    void testDtoToEntityMapper() {
        GameLobbyEntity gameLobbyEntity = TestDataUtil.createTestGameLobbyEntityA();
        gameLobbyEntityService.createLobby(gameLobbyEntity);

        PlayerDto testPlayerDtoA = TestDataUtil.createTestPlayerDtoA(gameLobbyEntity.getId());

        PlayerEntity playerEntityA = playerMapper.mapToEntity(testPlayerDtoA);
        assertThat(playerEntityA).isEqualTo(TestDataUtil.createTestPlayerEntityA(gameLobbyEntity));
    }

}
