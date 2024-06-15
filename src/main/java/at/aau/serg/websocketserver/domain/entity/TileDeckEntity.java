package at.aau.serg.websocketserver.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

//https://stackoverflow.com/questions/34972895/lombok-hashcode-issue-with-java-lang-stackoverflowerror-null
// might help! if there is a recursive StackOverflow error change Lombok @Data to @Getter and @Setter
//weird Lombok circular dependency issue
//@Data
@Getter
@Setter
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
    //    @OneToOne(mappedBy = "tileDeck")
    @OneToOne
    @JoinColumn(name = "game_session_id", unique = true)
    private GameSessionEntity gameSession;
}
