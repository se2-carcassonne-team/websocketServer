package at.aau.serg.websocketserver.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="gamelobby")
public class GameLobbyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gamelobby_id_seq")
    private Long id;

    private String name;

    // time stamp of game start
    //@Temporal(TemporalType.TIMESTAMP)
    private java.sql.Timestamp gameStartTimestamp;

    // game states: lobby, game, gameFinished
    private String gameState;

    // counter for the number of players
    private Integer numPlayers;

    private Long lobbyAdminId;

    private List<String> availableColours;
}
