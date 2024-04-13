package at.aau.serg.websocketserver.service.impl;

import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketserver.domain.entity.repository.PlayerEntityRepository;
import at.aau.serg.websocketserver.errorcode.ErrorCode;
import at.aau.serg.websocketserver.service.GameLobbyEntityService;
import jakarta.persistence.EntityExistsException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GameLobbyEntityServiceImpl implements GameLobbyEntityService {

    private GameLobbyEntityRepository gameLobbyEntityRepository;
    private final PlayerEntityRepository playerEntityRepository;

    public GameLobbyEntityServiceImpl(GameLobbyEntityRepository gameLobbyEntityRepository,
                                      PlayerEntityRepository playerEntityRepository) {
        this.gameLobbyEntityRepository = gameLobbyEntityRepository;
        this.playerEntityRepository = playerEntityRepository;
    }

    @Override
    public GameLobbyEntity createLobby(GameLobbyEntity gameLobbyEntity) {

        if(gameLobbyEntity.getId() != null && gameLobbyEntityRepository.findById(gameLobbyEntity.getId()).isPresent()) {
            throw new EntityExistsException(ErrorCode.ERROR_1001.getErrorCode());
        }

        if(gameLobbyEntity.getName() != null && gameLobbyEntityRepository.findByName(gameLobbyEntity.getName()).isPresent()) {
            throw new EntityExistsException(ErrorCode.ERROR_1002.getErrorCode());
        }

        if(gameLobbyEntity.getLobbyCreatorId() != null && playerEntityRepository.findById(gameLobbyEntity.getLobbyCreatorId()).isEmpty()) {
            throw new RuntimeException(ErrorCode.ERROR_2001.getErrorCode());
        }

        gameLobbyEntity.setNumPlayers(0);
        return gameLobbyEntityRepository.save(gameLobbyEntity);
    }

    @Override
    public GameLobbyEntity updateLobbyName(GameLobbyEntity gameLobbyEntity) {
        Optional<GameLobbyEntity> gameLobbyEntityOptional = gameLobbyEntityRepository.findById(gameLobbyEntity.getId());

        if(gameLobbyEntityOptional.isPresent()) {
            GameLobbyEntity gameLobbyToUpdate = gameLobbyEntityOptional.get();
            gameLobbyToUpdate.setName(gameLobbyEntity.getName());
            return gameLobbyEntityRepository.save(gameLobbyToUpdate);
        } else {
            throw new RuntimeException(ErrorCode.ERROR_1003.getErrorCode());
        }
    }

    @Override
    public List<GameLobbyEntity> getListOfLobbies() {
        return gameLobbyEntityRepository.findAll();
    }

    @Override
    public void deleteLobby(Long id) {
        gameLobbyEntityRepository.deleteById(id);
    }

    @Override
    public Optional<GameLobbyEntity> findById(Long id) {
        return gameLobbyEntityRepository.findById(id);
    }
}
