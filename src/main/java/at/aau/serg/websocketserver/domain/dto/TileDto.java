package at.aau.serg.websocketserver.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TileDto {

    private Long id;

    private String tileName;

    private String northEdgeType;
    private String southEdgeType;
    private String eastEdgeType;
    private String westEdgeType;

    private int rotation;
}
