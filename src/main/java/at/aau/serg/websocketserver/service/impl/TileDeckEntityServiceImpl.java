package at.aau.serg.websocketserver.service.impl;

import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import at.aau.serg.websocketserver.domain.entity.TileDeckEntity;
import at.aau.serg.websocketserver.domain.entity.repository.GameSessionEntityRepository;
import at.aau.serg.websocketserver.domain.entity.repository.TileDeckRepository;
import at.aau.serg.websocketserver.service.GameSessionEntityService;
import at.aau.serg.websocketserver.service.TileDeckEntityService;
import at.aau.serg.websocketserver.statuscode.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class TileDeckEntityServiceImpl implements TileDeckEntityService {
    TileDeckRepository tileDeckRepository;
    GameSessionEntityRepository gameSessionEntityRepository;


    public TileDeckEntityServiceImpl(TileDeckRepository tileDeckRepository,
                                     GameSessionEntityRepository gameSessionEntityRepository) {
        this.tileDeckRepository = tileDeckRepository;
        this.gameSessionEntityRepository = gameSessionEntityRepository;
    }

    /**
     * Create a new tile deck entity for the given game session ID.
     *
     * @param gameSessionId The ID of the game session for which the tile deck entity should be created.
     * @return The created tile deck entity.
     * @throws EntityNotFoundException If the game session entity does not exist for the given game session ID.
     */

    @Override
    public TileDeckEntity createTileDeck(Long gameSessionId) {
        Optional<GameSessionEntity> gameSessionOptional = gameSessionEntityRepository.findById(gameSessionId);

        if (gameSessionOptional.isPresent()) {
            GameSessionEntity gameSession = gameSessionOptional.get();

//            Create a new tile deck entity
            TileDeckEntity tileDeck = new TileDeckEntity();
            tileDeck.setTileId(generateTileIds());
            tileDeck.setGameSession(gameSession);

//            Return the created tile deck entity
            return tileDeckRepository.save(tileDeck);
        } else {
            throw new EntityNotFoundException(ErrorCode.ERROR_3003.getErrorCode());
        }
    }

    /**
     * Generate a list of tile IDs for a new tile deck.
     *
     * @return A list of tile IDs.
     */

    @Override
    public List<Long> generateTileIds() {
        List<Long> tileIds = new ArrayList<>();
        for (Long i = 1L; i <= 71L; i++) {
            tileIds.add(i);
        }
        Collections.shuffle(tileIds);
        return tileIds;
    }

/**
     * Draw the next tile from the tile deck.
     *
     * @param tileDeck The tile deck from which the next tile should be drawn.
     * @return The ID of the drawn tile.
     * @throws IllegalStateException If the tile deck is empty.
     */

    @Override
    public Long drawNextTile(TileDeckEntity tileDeck) {
        List<Long> tileIds = tileDeck.getTileId();
        if (tileIds.isEmpty()) {
            throw new IllegalStateException("No more tile left in the deck."); // No more tiles left in the deck
        }
        Long drawnTileId = tileIds.remove(0); // Remove and return the first tile from the deck
        tileDeck.setTileId(tileIds); // Update the tile deck in the database
        tileDeckRepository.save(tileDeck);
        return drawnTileId;
    }

    /**
     * Check if the tile deck is empty.
     *
     * @param tileDeck The tile deck to check.
     * @return True if the tile deck is empty, false otherwise.
     */

    @Override
    public boolean isTileDeckEmpty(TileDeckEntity tileDeck) {
        return tileDeck.getTileId().isEmpty();
    }

    @Override
    public List<Long> getAllTilesInDeck(Long gameSessionId) {
        return null;
    }

}

