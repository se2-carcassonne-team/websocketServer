package at.aau.serg.websocketserver.service.impl;

import at.aau.serg.websocketserver.domain.entity.TileDeckEntity;
import at.aau.serg.websocketserver.domain.entity.repository.TileDeckRepository;
import at.aau.serg.websocketserver.service.TileDeckEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class TileDeckEntityServiceImpl implements TileDeckEntityService {

    private final TileDeckRepository tileDeckRepository;

    public TileDeckEntityServiceImpl(TileDeckRepository tileDeckRepository) {
        this.tileDeckRepository = tileDeckRepository;
    }
    @Override
    public TileDeckEntity createTileDeck(Long gameSessionId) {
        TileDeckEntity tileDeck = TileDeckEntity.builder()
                .tileId(generateTileIds())
                .gameSessionId(gameSessionId)
                .build();
        return tileDeckRepository.save(tileDeck);
    }

    @Override
    public List<Long> generateTileIds() {
        List<Long> tileIds = new ArrayList<>();
        for (long i = 1; i <= 72; i++) {
            tileIds.add(i);
        }
        Collections.shuffle(tileIds);
        return tileIds;
    }
    @Override
    public Long drawNextTile(TileDeckEntity tileDeck) {
        List<Long> tileIds = tileDeck.getTileId();
        if (tileIds.isEmpty()) {
            return null; // No more tiles left in the deck
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
    @Override
    public List<Long> getAllTilesInDeck(Long gameSessionId) {
        return null;
    }

}

