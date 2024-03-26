package at.aau.serg.websocketdemoserver.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GambeLobbyDto {

    private Long id;

    private String name;

    // time stamp of game start
    private Date gameStartTimestamp;

    // game states: LOBBY, IN_GAME, FINISHED
    private GameState gameState;

    private ArrayList<PlayerDto> players;
}

