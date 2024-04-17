package at.aau.serg.websocketserver.mapper;

import at.aau.serg.websocketserver.domain.dto.PlayerDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.domain.entity.repository.GameLobbyEntityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PlayerMapper {

    private final ModelMapper modelMapper;
    GameLobbyEntityRepository gameLobbyEntityRepository;

    public PlayerMapper(ModelMapper modelMapper, GameLobbyEntityRepository gameLobbyEntityRepository) {
        this.modelMapper = modelMapper;
        this.gameLobbyEntityRepository = gameLobbyEntityRepository;
    }

    public PlayerDto mapToDto(PlayerEntity playerEntity){
        return modelMapper.map(playerEntity, PlayerDto.class);
    }

    public PlayerEntity mapToEntity(PlayerDto playerDto) {

        PlayerEntity playerEntity = modelMapper.map(playerDto, PlayerEntity.class);

        if(playerDto.getGameLobbyId() != null) {
            Optional<GameLobbyEntity> gameLobbyEntityOptional = gameLobbyEntityRepository.findById(playerDto.getGameLobbyId());

            if(gameLobbyEntityOptional.isPresent()){
                GameLobbyEntity gameLobbyEntity = gameLobbyEntityOptional.get();
                playerEntity.setGameLobbyEntity(gameLobbyEntity);

                return playerEntity;
            }

            throw new EntityNotFoundException("GameLobby with id " + playerDto.getGameLobbyId() + " does not exist");
        } else {
            playerEntity.setGameLobbyEntity(null);
            return playerEntity;
        }

    }
}
