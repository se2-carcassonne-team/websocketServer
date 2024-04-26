package at.aau.serg.websocketserver.domain.entity;

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
@Table(name = "gameSession")
public class GameSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gamesession_id_seq")
    private Long id;
    private Long turnPlayerId;
    private String gameState;
    private List<Long> playerIds;

    // One-to-one relationship with TileDeckEntity
    @OneToOne(mappedBy = "gameSession")
    private TileDeckEntity tileDeck;
}
