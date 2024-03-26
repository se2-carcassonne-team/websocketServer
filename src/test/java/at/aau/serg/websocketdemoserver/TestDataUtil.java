package at.aau.serg.websocketdemoserver;

import at.aau.serg.websocketdemoserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketdemoserver.domain.dto.GameState;
import at.aau.serg.websocketdemoserver.domain.dto.PlayerDto;
import at.aau.serg.websocketdemoserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketdemoserver.domain.entity.PlayerEntity;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.sql.Date;


public class TestDataUtil {
    public static PlayerEntity createTestPlayerEntityA(final GameLobbyEntity gameLobbyEntity){
        return PlayerEntity.builder()
                .id(1L)
                .username("usernameA")
                .gameLobbyEntity(gameLobbyEntity)
                .build();
    }
    public static PlayerEntity createTestPlayerEntityB(final GameLobbyEntity gameLobbyEntity){
        return PlayerEntity.builder()
                .id(2L)
                .username("usernameB")
                .gameLobbyEntity(gameLobbyEntity)
                .build();
    }
    public static PlayerEntity createTestPlayerEntityC(final GameLobbyEntity gameLobbyEntity){
        return PlayerEntity.builder()
                .id(3L)
                .username("usernameC")
                .gameLobbyEntity(gameLobbyEntity)
                .build();
    }

    public static GameLobbyEntity createTestGameLobbyEntityA() {
        Timestamp timeStamp = Timestamp.valueOf("2024-03-26 15:00:00.000");
        return GameLobbyEntity.builder()
                .id(1L)
                .name("lobbyA")
                .gameStartTimestamp(timeStamp)
                .gameState(GameState.LOBBY.toString())
                .build();
    }

    public static GameLobbyEntity createTestGameLobbyEntityB() {
        Timestamp timeStamp = Timestamp.valueOf("2024-03-26 15:01:00.000");
        return GameLobbyEntity.builder()
                .id(2L)
                .name("lobbyB")
                .gameStartTimestamp(timeStamp)
                .gameState(GameState.LOBBY.toString())
                .build();
    }

    public static GameLobbyEntity createTestGameLobbyEntityC() {
        Timestamp timeStamp = Timestamp.valueOf("2024-03-26 15:02:00.000");
        return GameLobbyEntity.builder()
                .id(3L)
                .name("lobbyC")
                .gameStartTimestamp(timeStamp)
                .gameState(GameState.LOBBY.toString())
                .build();
    }

    public static PlayerDto createTestPlayerDtoA(GameLobbyDto gameLobbyDto) {

        return PlayerDto.builder()
                .id(1L)
                .username("usernameA")
                .gameLobbyDto(gameLobbyDto)
                .build();
    }
}
