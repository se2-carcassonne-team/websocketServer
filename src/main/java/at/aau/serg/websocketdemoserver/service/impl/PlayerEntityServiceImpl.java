package at.aau.serg.websocketdemoserver.service.impl;

import at.aau.serg.websocketdemoserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketdemoserver.domain.dto.PlayerDto;
import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketdemoserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketdemoserver.domain.entity.repository.PlayerEntityRepository;
import at.aau.serg.websocketdemoserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketdemoserver.service.PlayerEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlayerEntityServiceImpl implements PlayerEntityService {

    PlayerEntityRepository playerEntityRepository;
    GameLobbyEntityRepository gameLobbyEntityRepository;

    public PlayerEntityServiceImpl(PlayerEntityRepository playerEntityRepository,
                                   GameLobbyEntityRepository gameLobbyEntityRepository) {
        this.playerEntityRepository = playerEntityRepository;
        this.gameLobbyEntityRepository = gameLobbyEntityRepository;
    }

    @Override
    public void createPlayer(String name) {

    }

    @Override
    public void updatePlayer(String name) {

    }

    @Override
    public GameLobbyDto joinLobby(GameLobbyDto lobby, PlayerDto player) {

        Optional<PlayerEntity> playerEntityOptional = playerEntityRepository.findById(player.getId());
        Optional<GameLobbyEntity> gameLobbyEntityOptional = gameLobbyEntityRepository.findById(lobby.getId());

        if (playerEntityOptional.isPresent()) {
            PlayerEntity playerEntity = playerEntityOptional.get();

            if (gameLobbyEntityOptional.isPresent()) {
                GameLobbyEntity gameLobbyEntity = gameLobbyEntityOptional.get();
                playerEntity.setGameLobbyEntity(gameLobbyEntity);

                playerEntityRepository.save(playerEntity);

                return new GameLobbyMapper(gameLobbyEntity, playerEntityRepository.findPlayerEntitiesByGameLobbyEntity_Id(gameLobbyEntity.getId())).mapDtoFromDB();

            } else {
                throw new RuntimeException("Lobby not found");
            }

        } else {
            throw new RuntimeException("Player not found");
        }


/*
        bookRepository.findById(isbn).map(existingBook -> {
            Optional.ofNullable(bookEntity.getTitle()).ifPresent(existingBook::setTitle);
            return bookRepository.save(existingBook);
        }).orElseThrow(() -> new RuntimeException("Book does not exist"));*/

    }

    @Override
    public void leaveLobby(Long id) {

    }

    @Override
    public void deletePlayer(Long id) {

    }
}
