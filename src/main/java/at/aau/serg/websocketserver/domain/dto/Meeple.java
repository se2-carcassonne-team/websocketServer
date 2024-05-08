package at.aau.serg.websocketserver.domain.dto;

import at.aau.serg.websocketserver.domain.pojo.Coordinates;
import at.aau.serg.websocketserver.domain.pojo.PlayerColour;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Meeple {
    Long id;
    PlayerColour color;
    Long playerId;
    boolean placed;
    Coordinates coordinates;
}
