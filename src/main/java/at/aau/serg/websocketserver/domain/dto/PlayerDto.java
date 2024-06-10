package at.aau.serg.websocketserver.domain.dto;

import at.aau.serg.websocketserver.domain.pojo.PlayerColour;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlayerDto {
    private Long id;

    private String username;
    private String sessionId;
    private Long gameSessionId;
    private Long gameLobbyId;
    private PlayerColour playerColour;

}
