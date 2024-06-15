package at.aau.serg.websocketserver.domain.entity.repository;


import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GameSessionEntityRepository extends JpaRepository<GameSessionEntity, Long> {
    @Query(value = "SELECT * FROM GAME_SESSION g WHERE :playerId = ANY (g.player_ids) LIMIT 1", nativeQuery = true)
    Optional<GameSessionEntity> findByPlayerId(@Param("playerId") Long playerId);
}
