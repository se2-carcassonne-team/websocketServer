package at.aau.serg.websocketserver.mapper;

import at.aau.serg.websocketserver.domain.dto.TileDto;
import at.aau.serg.websocketserver.domain.entity.TileEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class TileMapper {
    private final ModelMapper modelMapper;


    public TileMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

//    Amadeo
    public TileDto mapToDto(TileEntity tileEntity){
        return modelMapper.map(tileEntity, TileDto.class);
    }

    public TileEntity mapToEntity(TileDto tileDto){
        return modelMapper.map(tileDto, TileEntity.class);
    }


}
