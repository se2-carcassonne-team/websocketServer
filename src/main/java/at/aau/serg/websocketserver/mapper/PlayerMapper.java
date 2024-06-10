package at.aau.serg.websocketserver.mapper;

import at.aau.serg.websocketserver.domain.dto.PlayerDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketserver.domain.entity.repository.GameSessionEntityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PlayerMapper {

    private final ModelMapper modelMapper;
    GameLobbyEntityRepository gameLobbyEntityRepository;
    GameSessionEntityRepository gameSessionEntityRepository;

    public PlayerMapper(ModelMapper modelMapper, GameLobbyEntityRepository gameLobbyEntityRepository) {
        this.modelMapper = modelMapper;
        this.gameLobbyEntityRepository = gameLobbyEntityRepository;
    }

    public PlayerDto mapToDto(PlayerEntity playerEntity){
        PlayerDto dto =  modelMapper.map(playerEntity, PlayerDto.class);
        dto.setGameSessionId(playerEntity.getGameSessionEntity().getId());
        return dto;
    }

    public PlayerEntity mapToEntity(PlayerDto playerDto) {
        PlayerEntity playerEntity = modelMapper.map(playerDto, PlayerEntity.class);

        // find and assign the GameLobbyEntity based on the ID from the DTO
        if(playerDto.getGameLobbyId() != null) {
            Optional<GameLobbyEntity> gameLobbyEntityOptional = gameLobbyEntityRepository.findById(playerDto.getGameLobbyId());

            if(gameLobbyEntityOptional.isPresent()){
                GameLobbyEntity gameLobbyEntity = gameLobbyEntityOptional.get();
                playerEntity.setGameLobbyEntity(gameLobbyEntity);
            } else {
                throw new EntityNotFoundException("GameLobby with id " + playerDto.getGameLobbyId() + " does not exist");
            }
        } else {
            playerEntity.setGameLobbyEntity(null);
        }

        // find and assign the GameSessionEntity based on the ID from the DTO (same procedure as above)
        if (playerDto.getGameSessionId() != null) {
            Optional<GameSessionEntity> gameSessionEntityOptional = gameSessionEntityRepository.findById(playerDto.getGameSessionId());

            if (gameSessionEntityOptional.isPresent()) {
                GameSessionEntity gameSessionEntity = gameSessionEntityOptional.get();
                playerEntity.setGameSessionEntity(gameSessionEntity);
            } else {
                throw new EntityNotFoundException("GameSession with id " + playerDto.getGameSessionId() + " does not exist");
            }
        } else {
            playerEntity.setGameSessionEntity(null);
        }

        return playerEntity;
    }
}
