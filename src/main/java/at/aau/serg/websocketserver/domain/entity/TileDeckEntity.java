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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //DOCU: https://www.baeldung.com/java-jpa-persist-string-list
    @ElementCollection(targetClass = Long.class)
    private List<Long> tileId;

    private Long gameSessionId;
}
