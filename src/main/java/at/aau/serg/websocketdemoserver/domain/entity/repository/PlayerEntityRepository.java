package at.aau.serg.websocketdemoserver.domain.entity.repository;

import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerEntityRepository extends JpaRepository<PlayerEntity, Long> {

    List<PlayerEntity> findPlayerEntitiesByGameLobbyEntity_Id(Long id);

    /*@Query("SELECT p FROM player WHERE p.gameLobbyEntity")
    Iterable<PlayerEntity> findPlayersInLobby(Long id);*/
}
