package dev.geeler.apiaces.gameservice.repository;

import dev.geeler.apiaces.gameservice.model.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GamePlayerRepository extends JpaRepository<GamePlayer, UUID> {
    GamePlayer findByPlayerIdAndGameId(UUID playerId, UUID gameId);
    List<GamePlayer> findByGameId(UUID gameId);
}
