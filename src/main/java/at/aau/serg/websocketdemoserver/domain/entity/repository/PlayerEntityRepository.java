package at.aau.serg.websocketdemoserver.domain.entity.repository;

import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerEntityRepository extends JpaRepository<PlayerEntity, Long> {
}
