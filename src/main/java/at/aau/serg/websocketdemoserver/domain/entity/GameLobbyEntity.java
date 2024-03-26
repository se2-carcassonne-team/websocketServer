package at.aau.serg.websocketdemoserver.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="gamelobby")
public class GameLobbyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gamelobby_id_seq")
    private Long id;

    private String name;

    // time stamp of game start
    @Temporal(TemporalType.TIMESTAMP)
    private Date gameStartTimestamp;

    // game states: lobby, game, gameFinished
    private String gameState;

}
