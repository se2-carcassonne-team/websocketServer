package at.aau.serg.websocketserver.util;

import at.aau.serg.websocketserver.domain.dto.TileType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeckManager {

    private List<TileType> tiles;

    public DeckManager() {
        tiles = new ArrayList<>();
        generateDeck();
    }

    // Method to generate and populate the deck with all tile types
    private void generateDeck() {
        for (TileType tileType : TileType.values()) {
            for (int i = 0; i < tileType.getAmount(); i++) {
                tiles.add(tileType);
            }
        }
        shuffle();
    }

    // Method to shuffle the deck
    public void shuffle() {
        Collections.shuffle(tiles);
    }

    // Method to draw a tile from the deck (optional)
    public TileType drawTile() {
        if (tiles.isEmpty()) {
            throw new IllegalStateException("Deck is empty");
        }
        return tiles.remove(0);
    }

    // Method to get the number of tiles remaining in the deck (optional)
    public int size() {
        return tiles.size();
    }
}
