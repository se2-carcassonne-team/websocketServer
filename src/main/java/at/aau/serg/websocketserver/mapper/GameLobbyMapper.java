package at.aau.serg.websocketserver.mapper;

import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.repository.PlayerEntityRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class GameLobbyMapper {

    private ModelMapper modelMapper;
    private PlayerEntityRepository playerEntityRepository;
    private PlayerMapper playerMapper;

    public GameLobbyMapper(ModelMapper modelMapper, PlayerEntityRepository playerEntityRepository, PlayerMapper playerMapper) {
        this.modelMapper = modelMapper;
        this.playerEntityRepository = playerEntityRepository;
        this.playerMapper = playerMapper;
    }


    //////// Albert ////////
    public GameLobbyDto mapToDto(GameLobbyEntity gameLobbyEntity) {
        return modelMapper.map(gameLobbyEntity, GameLobbyDto.class);
    }
    public GameLobbyEntity mapToEntity(GameLobbyDto gameLobbyDto) {
        return modelMapper.map(gameLobbyDto, GameLobbyEntity.class);
    }




//    //////// Dominik ////////
//    List<PlayerEntity> playerEntities;
//
//    public GameLobbyEntity mapToDB(GameLobbyDto dto) {
//        return this.modelMapper.map(dto, GameLobbyEntity.class);
//        //return null;
//    }
//
//    public GameLobbyDto mapFromDB(GameLobbyEntity gameLobbyEntity) {
//
//        GameLobbyDto gameLobbyDto = new GameLobbyDto();
//
//        gameLobbyDto.setId(gameLobbyEntity.getId());
//        gameLobbyDto.setName(gameLobbyEntity.getName());
//        gameLobbyDto.setGameStartTimestamp(gameLobbyEntity.getGameStartTimestamp());
//        gameLobbyDto.setGameState(GameState.valueOf(gameLobbyEntity.getGameState()));
//
//        List<PlayerDto> playerDtos = new ArrayList<>();
//
//        for(PlayerEntity p : playerEntities) {
//            PlayerDto playerDto = new PlayerDto();
//            playerDto.setId(p.getId());
//            playerDto.setUsername(p.getUsername());
//            playerDtos.add(playerDto);
//        }
//
//        return gameLobbyDto;
//
//        //return this.mapper.map(entity, GameLobbyDto.class);
//    }

}
