package at.aau.serg.websocketserver.service.impl;

import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketserver.domain.entity.repository.PlayerEntityRepository;
import at.aau.serg.websocketserver.domain.pojo.PlayerColour;
import at.aau.serg.websocketserver.statuscode.ErrorCode;
import at.aau.serg.websocketserver.service.GameLobbyEntityService;
import jakarta.persistence.EntityExistsException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GameLobbyEntityServiceImpl implements GameLobbyEntityService {

    private final GameLobbyEntityRepository gameLobbyEntityRepository;
    private final PlayerEntityRepository playerEntityRepository;

    public GameLobbyEntityServiceImpl(GameLobbyEntityRepository gameLobbyEntityRepository,
                                      PlayerEntityRepository playerEntityRepository) {
        this.gameLobbyEntityRepository = gameLobbyEntityRepository;
        this.playerEntityRepository = playerEntityRepository;
    }

    @Override
    public GameLobbyEntity createLobby(GameLobbyEntity gameLobbyEntity) {

        if(gameLobbyEntity.getId() != null && gameLobbyEntityRepository.findById(gameLobbyEntity.getId()).isPresent()) {
            throw new EntityExistsException(ErrorCode.ERROR_1001.getCode());
        }

        if(gameLobbyEntity.getName() != null && gameLobbyEntityRepository.findByName(gameLobbyEntity.getName()).isPresent()) {
            throw new EntityExistsException(ErrorCode.ERROR_1002.getCode());
        }

        if(gameLobbyEntity.getLobbyAdminId() != null && playerEntityRepository.findById(gameLobbyEntity.getLobbyAdminId()).isEmpty()) {
            throw new RuntimeException(ErrorCode.ERROR_2001.getCode());
        }

        gameLobbyEntity.setNumPlayers(0);
        gameLobbyEntity.setAvailableColours(PlayerColour.getColoursAsList());
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
            throw new RuntimeException(ErrorCode.ERROR_1003.getCode());
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
