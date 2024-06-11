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
    private Integer numPlayers;
    // One-to-one relationship with TileDeckEntity
//    @OneToOne(cascade = CascadeType.ALL)
//    @JoinColumn(name = "tiledeck_id", unique = true)
//    @EqualsAndHashCode.Exclude
    @OneToOne(mappedBy = "gameSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private TileDeckEntity tileDeck;
}
