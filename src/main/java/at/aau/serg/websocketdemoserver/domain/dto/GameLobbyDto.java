package at.aau.serg.websocketdemoserver.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;

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

    private ArrayList<PlayerDto> players;
}

