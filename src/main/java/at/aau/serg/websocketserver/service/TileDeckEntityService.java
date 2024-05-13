package at.aau.serg.websocketserver.service;

import at.aau.serg.websocketserver.domain.entity.TileDeckEntity;

import java.util.List;

public interface TileDeckEntityService {
    TileDeckEntity createTileDeck(Long gameSessionId);

    List<Long> generateTileIds();

    Long drawNextTile(TileDeckEntity tileDeck);

    boolean isTileDeckEmpty(TileDeckEntity tileDeck);

    List<Long> getAllTilesInDeck(Long gameSessionId);
}