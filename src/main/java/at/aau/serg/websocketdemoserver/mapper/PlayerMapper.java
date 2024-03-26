package at.aau.serg.websocketdemoserver.mapper;

import at.aau.serg.websocketdemoserver.domain.dto.PlayerDto;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlayerMapper {

    private ModelMapper modelMapper;
    public PlayerMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public PlayerDto mapToDto(PlayerEntity playerEntity){
        return modelMapper.map(playerEntity, PlayerDto.class);
    }

    public PlayerEntity mapToEntity(PlayerDto playerDto) {
        return modelMapper.map(playerDto, PlayerEntity.class);
    }
}
