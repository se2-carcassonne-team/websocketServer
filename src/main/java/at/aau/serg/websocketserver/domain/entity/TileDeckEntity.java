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
@Table(name = "tiledeck")
public class TileDeckEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tiledeck_id_seq")
    private Long id;

    //DOCU: https://www.baeldung.com/java-jpa-persist-string-list
//    @ElementCollection(targetClass = Long.class)
    private List<Long> tileId;

    // One-to-one relationship with GameSessionEntity
    @OneToOne
    @JoinColumn(name = "game_session_id", unique = true)
    private GameSessionEntity gameSession;
}
