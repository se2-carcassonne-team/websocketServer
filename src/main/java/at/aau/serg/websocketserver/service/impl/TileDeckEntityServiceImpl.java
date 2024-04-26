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

    @Override
    public List<Long> generateTileIds() {
        List<Long> tileIds = new ArrayList<>();
        for (Long i = 0L; i <= 71L; i++) {
            tileIds.add(i);
        }
        Collections.shuffle(tileIds);
        return tileIds;
    }

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

    @Override
    public boolean isTileDeckEmpty(TileDeckEntity tileDeck) {
        return tileDeck.getTileId().isEmpty();
    }

    @Override
//    TODO: Implement the resetTileDeck method if needed
    public void resetTileDeck(Long gameSessionId) {
//        // Retrieve the tile deck entity for the given game session ID
//        TileDeckEntity tileDeckEntity = tileDeckRepository.findByGameSessionId(gameSessionId);
//
//        if (tileDeckEntity != null) {
//            // Retrieve all tiles from the database
//            List<TileDeckEntity> allTiles = tileDeckRepository.findAll();
//
//            // Extract the IDs of all tiles
//            List<Long> tileIds = new ArrayList<>();
//            for (TileEntity tile : allTiles) {
//                tileIds.add(tile.getId());
//            }
//
//            // Update the tile deck entity with the new list of tile IDs
//            tileDeckEntity.setTileId(tileIds);
//
//            // Save the updated tile deck entity
//            tileDeckRepository.save(tileDeckEntity);
//        } else {
//            // Handle case where tile deck entity does not exist for the given game session ID
//            // (Optional: you can throw an exception or log a message)
//        }
//    }
    }
//TODO implement this method
    @Override
    public List<Long> getAllTilesInDeck(Long gameSessionId) {
        return null;
    }

}

