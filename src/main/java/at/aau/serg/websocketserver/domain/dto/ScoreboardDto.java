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
public class ScoreboardDto {
    private Long gameSessionId;
    private Long gameLobbyId;
    private List<Long> playerIds;
    private List<String> playerNames;
}
