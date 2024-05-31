package at.aau.serg.websocketserver.domain.dto;

import at.aau.serg.websocketserver.domain.pojo.GameState;
import at.aau.serg.websocketserver.domain.pojo.PlayerColour;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameLobbyDto {

    private Long id;

    private String name;

    // time stamp of game start
    private Timestamp gameStartTimestamp;

    // game states: LOBBY, IN_GAME, FINISHED
    private GameState gameState;

    // counter for the number of players
    private Integer numPlayers;

    private Long lobbyAdminId;

    private List<PlayerColour> availableColours;
}

