package at.aau.serg.websocketserver.domain.entity.repository;

import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSessionEntityRepository extends JpaRepository<GameSessionEntity, Long> {
}
