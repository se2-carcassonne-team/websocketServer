package at.aau.serg.websocketserver.service;

import at.aau.serg.websocketserver.domain.entity.TileDeckEntity;
import at.aau.serg.websocketserver.domain.entity.repository.TileDeckRepository;
import at.aau.serg.websocketserver.service.impl.TileDeckEntityServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class TileDeckEntityServiceImplTest {


    @Mock
    private TileDeckRepository tileDeckRepository;
    @Mock
    private GameSessionEntityService gameSessionEntityService;
    @InjectMocks
    private TileDeckEntityServiceImpl tileDeckEntityService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
    }


    @Test
    void drawNextTile() {
        // Arrange
        List<Long> tileIds = new ArrayList<>(Arrays.asList(1L, 2L, 3L));
        TileDeckEntity tileDeck = new TileDeckEntity();
        tileDeck.setTileId(tileIds);

        // Mock the behavior of tileDeckRepository.save method
        when(tileDeckRepository.save(any(TileDeckEntity.class))).thenReturn(tileDeck);

        // Act
        Long result = tileDeckEntityService.drawNextTile(tileDeck);

        // Assert
        assertEquals(1L, result);
        assertEquals(2, tileDeck.getTileId().size());
        verify(tileDeckRepository, times(1)).save(tileDeck);
    }

    @Test
    void drawNextTile_EmptyDeck_ThrowsException() {
        // Arrange
        TileDeckEntity tileDeck = new TileDeckEntity();
        tileDeck.setTileId(new ArrayList<>()); // Set the tileId list as an empty list

        // Act
//        Long result = tileDeckEntityService.drawNextTile(tileDeck);

        // Assert
        assertThrows(IllegalStateException.class, () -> tileDeckEntityService.drawNextTile(tileDeck));
    }

    @Test
    void testGetAllTilesInDeck() {
        Long gameSessionId = 1L; // replace with the actual gameSessionId you want to use
        List<Long> expectedTileIds = Collections.emptyList(); // The method is expected to return an empty list

        // Act
        List<Long> result = tileDeckEntityService.getAllTilesInDeck(gameSessionId);

        // Assert
        assertEquals(expectedTileIds, result);
    }

}
