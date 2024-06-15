package at.aau.serg.websocketserver.repository;

import at.aau.serg.websocketserver.TestDataUtil;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.domain.entity.repository.PlayerEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

@SpringBootTest // starts up a test version of our app when the test runs
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)    // clean context changes after each test to avoid test pollution in database
class PlayerEntityRepositoryTest {

    private final PlayerEntityRepository underTest;
   @Autowired
    public PlayerEntityRepositoryTest(PlayerEntityRepository underTest) {
        this.underTest = underTest;
    }

    @Test
    void testThatPlayerCanBeCreatedAndRecalled() {
        GameLobbyEntity gameLobbyEntity = TestDataUtil.createTestGameLobbyEntityA();
        PlayerEntity playerEntity = TestDataUtil.createTestPlayerEntityA(gameLobbyEntity);

        underTest.save(playerEntity);
        // a GameLobby Entity is also  created in the database in addition to the Player Entity due to cascading: @ManyToOne(cascade = CascadeType.ALL)

        Optional<PlayerEntity> result = underTest.findById(playerEntity.getId());

        assertThat(result).isPresent();

        assertThat(result).contains(playerEntity);
    }

    @Test
    void testThatMultiplePlayersCanBeCreatedAndRecalled() {
        GameLobbyEntity gameLobbyEntity = TestDataUtil.createTestGameLobbyEntityA();

        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(gameLobbyEntity);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(gameLobbyEntity);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(gameLobbyEntity);

        underTest.save(playerEntityA);
        underTest.save(playerEntityB);
        underTest.save(playerEntityC);

        List<PlayerEntity> result = underTest.findAll();

        assertThat(result)
                .hasSize(3)
                .containsExactly(playerEntityA, playerEntityB, playerEntityC);
    }

    @Test
    void testThatPlayerCanBeUpdated() {
        GameLobbyEntity gameLobbyEntity = TestDataUtil.createTestGameLobbyEntityA();

        PlayerEntity playerEntity = TestDataUtil.createTestPlayerEntityA(gameLobbyEntity);

        underTest.save(playerEntity);

        playerEntity.setUsername("UPDATED");
        underTest.save(playerEntity);

        Optional<PlayerEntity> result = underTest.findById(playerEntity.getId());

        assertThat(result).isPresent();

        assertThat(result).contains(playerEntity);

    }

    @Test
    void testThatPlayerCanBeDeleted() {
        GameLobbyEntity gameLobbyEntity = TestDataUtil.createTestGameLobbyEntityA();

        PlayerEntity playerEntity = TestDataUtil.createTestPlayerEntityA(gameLobbyEntity);

        underTest.save(playerEntity);

        underTest.delete(playerEntity);

        Optional<PlayerEntity> result = underTest.findById(playerEntity.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void testThatPlayersCanBeFoundByGameLobbyId() {
        GameLobbyEntity gameLobbyEntityA = TestDataUtil.createTestGameLobbyEntityA();
        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(gameLobbyEntityA);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(gameLobbyEntityA);

        GameLobbyEntity gameLobbyEntityB = TestDataUtil.createTestGameLobbyEntityB();
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(gameLobbyEntityB);

        GameLobbyEntity gameLobbyEntityC = TestDataUtil.createTestGameLobbyEntityC();


        underTest.save(playerEntityA);
        underTest.save(playerEntityB);
        underTest.save(playerEntityC);

        List<PlayerEntity> resultA = underTest.findPlayerEntitiesByGameLobbyEntity_Id(gameLobbyEntityA.getId());
        assertThat(resultA)
                .hasSize(2)
                .containsExactly(playerEntityA, playerEntityB);

        List<PlayerEntity> resultB = underTest.findPlayerEntitiesByGameLobbyEntity_Id(gameLobbyEntityB.getId());
        assertThat(resultB)
                .hasSize(1)
                .containsExactly(playerEntityC);

        List<PlayerEntity> resultC = underTest.findPlayerEntitiesByGameLobbyEntity_Id(gameLobbyEntityC.getId());
        assertThat(resultC).isEmpty();
    }

    @Test
    void testThatPlayersCanBeFoundByUsername() {
        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        PlayerEntity playerEntityB = TestDataUtil.createTestPlayerEntityB(null);
        PlayerEntity playerEntityC = TestDataUtil.createTestPlayerEntityC(null);

        underTest.save(playerEntityA);
        underTest.save(playerEntityB);
        underTest.save(playerEntityC);

        List<PlayerEntity> result = underTest.findPlayerEntitiesByUsername(playerEntityA.getUsername());
        assertThat(result)
                .hasSize(1)
                .containsExactly(playerEntityA);
    }

    @Test
    void testPlayerExistsById() {
        PlayerEntity playerEntityA = TestDataUtil.createTestPlayerEntityA(null);
        underTest.save(playerEntityA);

        assertThat(underTest.existsById(playerEntityA.getId())).isTrue();

        assertThat(underTest.existsById(playerEntityA.getId()+1)).isFalse();
    }
}
