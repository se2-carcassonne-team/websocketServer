package at.aau.serg.websocketdemoserver.repository;

import at.aau.serg.websocketdemoserver.TestDataUtil;
import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.repository.GameLobbyEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

@SpringBootTest // starts up a test version of our app when the test runs
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)    // clean context changes after each test to avoid test pollution in database
public class GameLobbyEntityRepositoryTest {
    private GameLobbyEntityRepository underTest;

    @Autowired
    public GameLobbyEntityRepositoryTest(GameLobbyEntityRepository underTest) {
        this.underTest = underTest;
    }

    @Test
    void testThatGameLobbyCanBeCreatedAndRecalled() {
        GameLobbyEntity gameLobbyEntity = TestDataUtil.createTestGameLobbyEntityA();

        underTest.save(gameLobbyEntity);

        Optional<GameLobbyEntity> result = underTest.findById(gameLobbyEntity.getId());

        // result should not be an empty optional
        assertThat(result).isPresent();

        // result from query should be equal to created GameLobby Entity
        assertThat(result.get()).isEqualTo(gameLobbyEntity);
    }
}
