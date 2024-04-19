package at.aau.serg.websocketserver.mapper;

import at.aau.serg.websocketserver.domain.dto.GameSessionDto;
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class GameSessionMapper {
    private final ModelMapper modelMapper;

    public GameSessionMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public GameSessionDto mapToDto(GameSessionEntity gameSessionEntity) {
        return modelMapper.map(gameSessionEntity, GameSessionDto.class);
    }

    public GameSessionEntity mapToEntity(GameSessionDto gameSessionDto) {
        return modelMapper.map(gameSessionDto, GameSessionEntity.class);
    }

}
