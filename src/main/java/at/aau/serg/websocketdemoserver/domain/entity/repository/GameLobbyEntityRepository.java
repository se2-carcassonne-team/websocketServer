package at.aau.serg.websocketdemoserver.domain.entity.repository;

import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameLobbyEntityRepository extends JpaRepository<GameLobbyEntity, Long> {
    Optional<GameLobbyEntity> findByName(String name);
}
