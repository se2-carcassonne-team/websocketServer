package at.aau.serg.websocketserver.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameSessionDto {
    private Long id;
    private Long turnPlayerId;
    private GameState gameState;
    private List<Long> playerIds;
}
