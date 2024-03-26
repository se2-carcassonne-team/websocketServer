package at.aau.serg.websocketdemoserver.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="player")
public class PlayerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_id_seq")
    private Long id;

    private String username;

    // 0..1 : n relationship betw. lobby and player
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="gamelobby_id")
    private GameLobbyEntity gameLobbyEntity;
}
