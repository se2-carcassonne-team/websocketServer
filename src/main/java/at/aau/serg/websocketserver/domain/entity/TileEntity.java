package at.aau.serg.websocketserver.domain.entity;

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
@Table(name = "tile")
public class TileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tile_id_seq")
    private Long id;
}
