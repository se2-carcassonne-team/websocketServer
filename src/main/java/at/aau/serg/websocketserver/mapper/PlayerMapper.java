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

    public PlayerMapper(ModelMapper modelMapper, GameLobbyEntityRepository gameLobbyEntityRepository, GameSessionEntityRepository gameSessionEntityRepository) {
        this.modelMapper = modelMapper;
        this.gameLobbyEntityRepository = gameLobbyEntityRepository;
        this.gameSessionEntityRepository = gameSessionEntityRepository;
    }

    public PlayerDto mapToDto(PlayerEntity playerEntity){
        return modelMapper.map(playerEntity, PlayerDto.class);
    }

    public PlayerEntity mapToEntity(PlayerDto playerDto) {


        PlayerEntity playerEntity = modelMapper.map(playerDto, PlayerEntity.class);

        // GameLobbyEntity Zuweisung
        if (playerDto.getGameLobbyId() != null) {
            GameLobbyEntity gameLobbyEntity = gameLobbyEntityRepository.findById(playerDto.getGameLobbyId())
                    .orElseThrow(() -> new EntityNotFoundException("GameLobby with id " + playerDto.getGameLobbyId() + " does not exist"));
            playerEntity.setGameLobbyEntity(gameLobbyEntity);
        } else {
            playerEntity.setGameLobbyEntity(null);
        }

        // GameSessionEntity Zuweisung
        if (playerDto.getGameSessionId() != null) {
            GameSessionEntity gameSessionEntity = gameSessionEntityRepository.findById(playerDto.getGameSessionId())
                    .orElseThrow(() -> new EntityNotFoundException("GameSession with id " + playerDto.getGameSessionId() + " does not exist"));
            playerEntity.setGameSessionEntity(gameSessionEntity);
        } else {
            playerEntity.setGameSessionEntity(null);
        }

        return playerEntity;

    }
}
