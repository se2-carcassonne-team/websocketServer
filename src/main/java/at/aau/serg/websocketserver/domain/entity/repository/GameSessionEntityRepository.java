package at.aau.serg.websocketserver.domain.entity.repository;

<<<<<<< HEAD
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

=======
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

>>>>>>> amadeo_implement_card_tile_deck_backend
public interface GameSessionEntityRepository extends JpaRepository<GameSessionEntity, Long> {
}
