package at.aau.serg.websocketserver.domain.dto;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link at.aau.serg.websocketserver.domain.entity.TileDeckEntity}
 */
@Value
@Builder
public class TileDeckDto implements Serializable {
    Long id;
    List<Long> tileId;
    Long gameSessionId;
}