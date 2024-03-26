package at.aau.serg.websocketdemoserver.mapper;

import at.aau.serg.websocketdemoserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketdemoserver.domain.dto.GameState;
import at.aau.serg.websocketdemoserver.domain.dto.PlayerDto;
import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class GameLobbyMapper {

    /*@Autowired
    private ModelMapper mapper;*/

    GameLobbyEntity gameLobbyEntity;
    List<PlayerEntity> playerEntities;

    public GameLobbyEntity mapEntityForDB(GameLobbyDto dto) {
        //return this.mapper.map(dto, GameLobbyEntity.class);
        return null;
    }

    public GameLobbyDto mapDtoFromDB() {

        GameLobbyDto gameLobbyDto = new GameLobbyDto();

        gameLobbyDto.setId(gameLobbyEntity.getId());
        gameLobbyDto.setName(gameLobbyEntity.getName());
        gameLobbyDto.setGameStartTimestamp(gameLobbyEntity.getGameStartTimestamp());
        gameLobbyDto.setGameState(GameState.valueOf(gameLobbyEntity.getGameState()));

        List<PlayerDto> playerDtos = new ArrayList<>();

        for(PlayerEntity p : playerEntities) {
            PlayerDto playerDto = new PlayerDto();
            playerDto.setId(p.getId());
            playerDto.setUsername(p.getUsername());
            playerDtos.add(playerDto);
        }

        return gameLobbyDto;

        //return this.mapper.map(entity, GameLobbyDto.class);
    }

}
