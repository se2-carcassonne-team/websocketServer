package at.aau.serg.websocketserver.mapper;

import at.aau.serg.websocketserver.TestDataUtil;
import at.aau.serg.websocketserver.domain.dto.PlayerDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.service.GameLobbyEntityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PlayerMapperTest {

    private final GameLobbyEntityService gameLobbyEntityService;
    private final PlayerMapper playerMapper;

    @Autowired
    public PlayerMapperTest(GameLobbyEntityService gameLobbyEntityService, PlayerMapper playerMapper) {
        this.gameLobbyEntityService = gameLobbyEntityService;
        this.playerMapper = playerMapper;
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
