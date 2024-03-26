package at.aau.serg.websocketdemoserver.service.impl;

import at.aau.serg.websocketdemoserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketdemoserver.domain.dto.PlayerDto;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketdemoserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketdemoserver.domain.entity.repository.PlayerEntityRepository;
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

        playerEntityRepository.findById(player.getId()).map(playerEntity -> {
           Optional.ofNullable(playerEntity.getGameLobbyEntity().getId()).ifPresent();
        });


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
