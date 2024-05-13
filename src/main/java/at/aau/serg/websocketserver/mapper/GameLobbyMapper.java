package at.aau.serg.websocketserver.mapper;

import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.repository.PlayerEntityRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class GameLobbyMapper {

    private final ModelMapper modelMapper;
    private final PlayerMapper playerMapper;

    public GameLobbyMapper(ModelMapper modelMapper, PlayerMapper playerMapper) {
        this.modelMapper = modelMapper;
        this.playerMapper = playerMapper;
    }

    public GameLobbyDto mapToDto(GameLobbyEntity gameLobbyEntity) {
        return modelMapper.map(gameLobbyEntity, GameLobbyDto.class);
    }
    public GameLobbyEntity mapToEntity(GameLobbyDto gameLobbyDto) {
        return modelMapper.map(gameLobbyDto, GameLobbyEntity.class);
    }

}
