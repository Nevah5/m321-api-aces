package dev.geeler.apiaces.gameservice.repository;

import dev.geeler.apiaces.gameservice.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {
    Game findByRoomId(String roomId);
}