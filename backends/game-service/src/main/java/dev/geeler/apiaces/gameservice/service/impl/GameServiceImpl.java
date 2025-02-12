package dev.geeler.apiaces.gameservice.service.impl;

import dev.geeler.apiaces.gameservice.dto.GamePlayerDto;
import dev.geeler.apiaces.gameservice.exception.MaxGameSizeException;
import dev.geeler.apiaces.gameservice.exception.NotFoundException;
import dev.geeler.apiaces.gameservice.model.game.ChatMessage;
import dev.geeler.apiaces.gameservice.model.game.ChatType;
import dev.geeler.apiaces.gameservice.model.game.Game;
import dev.geeler.apiaces.gameservice.model.game.GameActivity;
import dev.geeler.apiaces.gameservice.model.game.GameActivityType;
import dev.geeler.apiaces.gameservice.model.game.GamePlayer;
import dev.geeler.apiaces.gameservice.model.game.GameStatus;
import dev.geeler.apiaces.gameservice.model.game.GameTurn;
import dev.geeler.apiaces.gameservice.model.http.ErrorResponse;
import dev.geeler.apiaces.gameservice.model.security.UserPrincipal;
import dev.geeler.apiaces.gameservice.repository.GamePlayerRepository;
import dev.geeler.apiaces.gameservice.repository.GameRepository;
import dev.geeler.apiaces.gameservice.repository.GameTurnRepository;
import dev.geeler.apiaces.gameservice.service.CardService;
import dev.geeler.apiaces.gameservice.service.ChatService;
import dev.geeler.apiaces.gameservice.service.GameService;
import dev.geeler.apiaces.gameservice.service.KafkaService;
import dev.geeler.apiaces.gameservice.service.PlayerService;
import dev.geeler.apiaces.gameservice.service.WebsocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class GameServiceImpl implements GameService {
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final GameTurnRepository gameTurnRepository;
    private final ChatService chatService;
    private final PlayerService playerService;
    private final KafkaService kafkaService;
    private final WebsocketService websocketService;
    private final CardService cardService;

    public GameServiceImpl(
            GameRepository gameRepository,
            GamePlayerRepository gamePlayerRepository,
            ChatService chatService,
            @Lazy PlayerService playerService,
            KafkaService kafkaService,
            @Lazy WebsocketService websocketService,
            GameTurnRepository gameTurnRepository,
            CardService cardService) {
        this.gameRepository = gameRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.chatService = chatService;
        this.playerService = playerService;
        this.kafkaService = kafkaService;
        this.websocketService = websocketService;
        this.gameTurnRepository = gameTurnRepository;
        this.cardService = cardService;
    }

    @Override
    public Optional<Game> getGame(Long roomId) {
        return Optional.ofNullable(gameRepository.findByRoomId(roomId.toString())); // TODO: FIX DUPLICATE ROOM ID CASE
    }

    @Override
    public Optional<Game> getGame(UUID gameId) {
        return gameRepository.findById(gameId);
    }

    @Override
    public Game createGame(UUID ownerId) {
        Optional.ofNullable(gamePlayerRepository.findByPlayerIdAndLeftAtIsNull(ownerId))
                .ifPresent(game -> {
                    throw new IllegalStateException("You are already in a game! gameId: " + game.getGameId().toString());
                });
        final Game game = new Game.Builder()
                .setOwnerId(ownerId)
                .setCreatedAt()
                .build();
        gameRepository.save(game);
        return game;
    }

    @Override
    public void joinGame(UUID gameId, Principal principal) {
        final UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        final UserPrincipal userPrincipal = (UserPrincipal) token.getPrincipal();
        final Game game = this.getGame(gameId).orElseThrow(() -> new NotFoundException("The game was not found"));

        if (game.getStatus() != GameStatus.WAITING_FOR_PLAYERS) {
            if (game.getOwnerId().equals(userPrincipal.getId()) && game.getStatus() == GameStatus.INITIALIZING) {
                this.updateGameStatus(game, GameStatus.WAITING_FOR_PLAYERS);
            } else {
                throw new IllegalStateException("This game is possibly not accessible anymore.");
            }
        }

        gamePlayerRepository.findByPlayerIdAndGameId(userPrincipal.getId(), game.getId())
                .ifPresent(gamePlayer -> {
                    if (game.getStatus() != GameStatus.WAITING_FOR_PLAYERS)
                        throw new IllegalStateException("Player already in game.");
                });

        if (getCurrentGamePlayerCount(gameId) >= game.getMaxPlayers()) {
            throw new MaxGameSizeException("Max game size reached!");
        }

        gamePlayerRepository.save(new GamePlayer.Builder()
                .setGameId(game.getId())
                .setPlayerId(userPrincipal.getId())
                .setUsername(userPrincipal.getUsername())
                .build());
    }

    @Override
    public void leaveGame(UUID gameId, UUID playerId, String username) {
        final Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalStateException("Game not found"));
        final GamePlayer gamePlayer = gamePlayerRepository.findByPlayerIdAndGameId(playerId, gameId)
                .orElseThrow(() -> new IllegalStateException("Player not found"));

        if (game.getStatus() != GameStatus.FINISHED) {
            gamePlayerRepository.save(gamePlayer.toBuilder()
                    .leave()
                    .build());
        }

        chatService.sendChatMessage(ChatMessage.builder()
                .gameId(gameId)
                .senderId(playerId)
                .senderUsername(username)
                .isHost(playerService.isOwnerOfGame(playerId, gameId))
                .isJoined(false)
                .type(ChatType.ACTIVITY)
                .build());


        if (game.getOwnerId().equals(playerId) && game.getStatus() != GameStatus.FINISHED) {
            updateGameStatus(game, GameStatus.OWNER_LEFT);
            kafkaService.sendMessage(
                    "games.activity",
                    GameActivity.builder()
                            .gameId(gameId)
                            .playerId(playerId)
                            .type(GameActivityType.GAME_TERMINATE)
                            .message("Game terminated because the owner left")
                            .build()
            );
        }
    }

    @Override
    public void startGame(UUID gameId, UUID playerId) {
        final Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            websocketService.sendError(playerId, new ErrorResponse(HttpStatus.BAD_REQUEST, "Game provided not found"));
            return;
        }
        if (!game.getOwnerId().equals(playerId)) {
            websocketService.sendError(playerId, new ErrorResponse(HttpStatus.FORBIDDEN, "You are not allowed to start the game"));
            return;
        }
        if (this.getConnectedPlayers(gameId).size() < 2) {
            websocketService.sendError(playerId, new ErrorResponse(HttpStatus.BAD_REQUEST, "Not enough players to start the game"));
            return;
        }
        game.builder()
                .setStatus(GameStatus.IN_PROGRESS)
                .setStartedAt()
                .build();
        gameRepository.save(game);
        kafkaService.sendMessage(
                "games.activity",
                GameActivity.builder()
                        .gameId(gameId)
                        .type(GameActivityType.GAME_START)
                        .turnOrder(prepareGameTurnOrder(gameId))
                        .build()
        );

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<UUID> deck = cardService.getShuffledDeck();
        List<GamePlayer> players = getConnectedPlayers(gameId).stream().filter(player -> player.getLeftAt() == null).toList();
        log.info("DECK SIZE: " + deck.size());
        Map<UUID, List<UUID>> playerDecks = new HashMap<>();
        for (GamePlayer player : players) {
            playerDecks.putIfAbsent(player.getPlayerId(), new ArrayList<>());
            for (int i = 0; i < 5; i++) {
                playerDecks.get(player.getPlayerId()).add(deck.remove(0));
            }
            kafkaService.sendMessage(
                    "games.activity",
                    GameActivity.builder()
                            .gameId(gameId)
                            .playerId(player.getPlayerId())
                            .type(GameActivityType.CURRENT_CARDS)
                            .cards(playerDecks.get(player.getPlayerId()))
                            .build()
            );
        }
    }

    private List<GamePlayerDto> prepareGameTurnOrder(UUID gameId) {
        List<GamePlayer> players = new ArrayList<>(getConnectedPlayers(gameId).stream()
                .filter(player -> player.getLeftAt() == null)
                .toList());
        Collections.shuffle(players);
        List<UUID> turnUUIDs = new ArrayList<>();
        players.forEach(p -> turnUUIDs.add(UUID.randomUUID()));
        for (int i = 0; i < players.size(); i++) {
            GamePlayer player = players.get(i);
            gameTurnRepository.save(new GameTurn.Builder()
                    .setId(turnUUIDs.get(i))
                    .setPlayerId(player.getPlayerId())
                    .setGameId(gameId)
                    .setNextTurnId(i < players.size() - 1 ? turnUUIDs.get(i + 1) : null)
                    .build());
        }
        return players.stream()
                .map(GamePlayerDto::new)
                .toList();
    }

    @Override
    public List<GamePlayer> getConnectedPlayers(UUID gameId) {
        return gamePlayerRepository.findGamePlayersByGameId(gameId);
    }

    @Override
    public int getCurrentGamePlayerCount(UUID gameId) {
        return gamePlayerRepository.findGamePlayersByGameId(gameId).size();
    }

    private void updateGameStatus(Game game, GameStatus status) {
        gameRepository.save(game.builder()
                .setStatus(status)
                .build());
    }
}
