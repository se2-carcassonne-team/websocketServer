package at.aau.serg.websocketdemoserver.repository;

import at.aau.serg.websocketdemoserver.TestDataUtil;
import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketdemoserver.domain.entity.repository.PlayerEntityRepository;
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
public class PlayerEntityRepositoryTest {

    private PlayerEntityRepository underTest;
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

        assertThat(result.get()).isEqualTo(playerEntity);
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

        assertThat(result.get()).isEqualTo(playerEntity);

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
}
