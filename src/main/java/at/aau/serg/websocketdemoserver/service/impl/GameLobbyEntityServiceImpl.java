package at.aau.serg.websocketdemoserver.service.impl;

import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.repository.GameLobbyEntityRepository;
import at.aau.serg.websocketdemoserver.service.GameLobbyEntityService;
import jakarta.persistence.EntityExistsException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GameLobbyEntityServiceImpl implements GameLobbyEntityService {

    private GameLobbyEntityRepository gameLobbyEntityRepository;

    public GameLobbyEntityServiceImpl(GameLobbyEntityRepository gameLobbyEntityRepository) {
        this.gameLobbyEntityRepository = gameLobbyEntityRepository;
    }

    @Override
    public GameLobbyEntity createLobby(GameLobbyEntity gameLobbyEntity) {

        if(gameLobbyEntity.getId() != null && gameLobbyEntityRepository.findById(gameLobbyEntity.getId()).isPresent()) {
            throw new EntityExistsException("gameLobby with id " + gameLobbyEntity.getId() + " already exists");
        }

        if(gameLobbyEntity.getName() != null && gameLobbyEntityRepository.findByName(gameLobbyEntity.getName()).isPresent()) {
            throw new EntityExistsException("gameLobby with name " + gameLobbyEntity.getName() + " already exists");
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
            throw new RuntimeException("gameLobby does not exist");
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
