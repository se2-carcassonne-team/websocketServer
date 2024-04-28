package at.aau.serg.websocketserver.mapper;

import at.aau.serg.websocketserver.domain.dto.TileDeckDto;
import at.aau.serg.websocketserver.domain.dto.TileDto;
import at.aau.serg.websocketserver.domain.entity.TileDeckEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class TileDeckMapper {
    private final ModelMapper modelMapper;


    public TileDeckMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    //    Amadeo
    public TileDeckDto mapToDto(TileDeckEntity tileDeckEntity) {
        return modelMapper.map(tileDeckEntity, TileDeckDto.class);
    }

    public TileDeckEntity mapToEntity(TileDto tileDto) {
        return modelMapper.map(tileDto, TileDeckEntity.class);
    }
}
