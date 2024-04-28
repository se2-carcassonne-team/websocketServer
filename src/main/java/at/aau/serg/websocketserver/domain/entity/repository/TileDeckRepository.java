package at.aau.serg.websocketserver.domain.entity.repository;

import at.aau.serg.websocketserver.domain.entity.TileDeckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TileDeckRepository extends JpaRepository<TileDeckEntity, Long> {
    TileDeckEntity findByGameSessionId(Long gameSessionId);

}
