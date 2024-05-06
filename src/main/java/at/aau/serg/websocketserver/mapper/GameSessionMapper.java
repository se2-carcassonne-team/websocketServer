package at.aau.serg.websocketserver.mapper;

import at.aau.serg.websocketserver.domain.dto.GameSessionDto;
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import at.aau.serg.websocketserver.domain.entity.repository.PlayerEntityRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class GameSessionMapper {
    private final ModelMapper modelMapper;
    private final PlayerMapper playerMapper;
    private final PlayerEntityRepository playerEntityRepository;

    public GameSessionMapper(ModelMapper modelMapper, PlayerMapper playerMapper, PlayerEntityRepository playerEntityRepository) {
        this.modelMapper = modelMapper;
        this.playerMapper = playerMapper;
        this.playerEntityRepository = playerEntityRepository;
    }

    public GameSessionDto mapToDto(GameSessionEntity gameSessionEntity) {
        return modelMapper.map(gameSessionEntity, GameSessionDto.class);
    }

    public GameSessionEntity mapToEntity(GameSessionDto gameSessionDto) {
        return modelMapper.map(gameSessionDto, GameSessionEntity.class);
    }

}
