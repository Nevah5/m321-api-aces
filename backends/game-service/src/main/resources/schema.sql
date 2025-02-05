USE `game-svc`;

DROP TABLE IF EXISTS `games`;

CREATE TABLE `games`
(
    `id`            varchar(36) NOT NULL,
    `roomId`        varchar(6)  NOT NULL,
    `ownerId`       varchar(36) NOT NULL,
    `status`        int         NOT NULL,
    `maxPlayers`    int         NOT NULL,
    `createdAt`     long        NOT NULL,
    `startedAt`     long,
    `currentTurnId` varchar(36),
    `winnerId`      varchar(36),
    PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `game_players`;

CREATE TABLE `game_players`
(
    `gameId`   varchar(36) NOT NULL,
    `playerId` varchar(36) NOT NULL,
    `username` varchar(20) NOT NULL,
    `joinedAt` long        NOT NULL,
    `leftAt`   long,
    PRIMARY KEY (`gameId`, `playerId`)
);

DROP TABLE IF EXISTS `game_turns`;

CREATE TABLE `game_turns`
(
    `id`           varchar(36) NOT NULL,
    `gameId`       varchar(36) NOT NULL,
    `playerId`     varchar(36) NOT NULL,
    `nextTurnId`   varchar(36),
    `playedAt`     long,
    `playedCardId` varchar(36),
    PRIMARY KEY (`id`)
);
