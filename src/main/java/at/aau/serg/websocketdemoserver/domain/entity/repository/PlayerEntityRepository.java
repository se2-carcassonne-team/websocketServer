package at.aau.serg.websocketdemoserver.domain.entity.repository;

import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayerEntityRepository extends JpaRepository<PlayerEntity, Long> {

    // Alternatively, we could include this HQL query to define the exact operation.
    // However, Spring often manages to find out on its own what query to execute based on the method name.
    //@Query("SELECT p FROM PlayerEntity p WHERE p.gameLobbyEntity.id = ?1")
    List<PlayerEntity> findPlayerEntitiesByGameLobbyEntity_Id(Long id);

}
