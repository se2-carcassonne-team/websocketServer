package at.aau.serg.websocketserver.domain.dto;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class FinishedTurnDto {
    private Long gameSessionId;
    private Map<Long, Integer> points;
    private Map<Long, List<Meeple>> playersWithMeeples;
}
