package at.aau.serg.websocketserver.domain.dto;

import at.aau.serg.websocketserver.domain.pojo.Coordinates;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Tile Object we get from a player at the end of his turn.
 * No need to save this in the database, we just have to pass it on to the other players in the gameSession
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlacedTileDto {
    // Game-Session id
    private Long gameSessionId;

    // Tile id
    private Long tileId;

    // tile coordinates
    private Coordinates coordinates;

    // tile rotation
    private Integer rotation;
}