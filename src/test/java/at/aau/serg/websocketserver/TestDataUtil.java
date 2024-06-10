package at.aau.serg.websocketserver;

import at.aau.serg.websocketserver.domain.dto.*;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.GameSessionEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.domain.pojo.Coordinates;
import at.aau.serg.websocketserver.domain.pojo.GameState;
import at.aau.serg.websocketserver.domain.pojo.PlayerColour;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class TestDataUtil {
    /**
     * id: 1L
     * @param gameLobbyEntity
     * @return
     */
    public static PlayerEntity createTestPlayerEntityA(final GameLobbyEntity gameLobbyEntity){
        return PlayerEntity.builder()
                .id(1L)
                .username("usernameA")
                .gameLobbyEntity(gameLobbyEntity)
                .playerColour(null)
                .build();
    }

    /**
     * id: 2L
     * @param gameLobbyEntity
     * @return
     */
    public static PlayerEntity createTestPlayerEntityB(final GameLobbyEntity gameLobbyEntity){
        return PlayerEntity.builder()
                .id(2L)
                .username("usernameB")
                .gameLobbyEntity(gameLobbyEntity)
                .playerColour(null)
                .build();
    }

    /**
     * id: 3L
     * @param gameLobbyEntity
     * @return
     */
    public static PlayerEntity createTestPlayerEntityC(final GameLobbyEntity gameLobbyEntity){
        return PlayerEntity.builder()
                .id(3L)
                .username("usernameC")
                .gameLobbyEntity(gameLobbyEntity)
                .playerColour(null)
                .build();
    }
    public static PlayerEntity createTestPlayerEntityD(final GameLobbyEntity gameLobbyEntity){
        return PlayerEntity.builder()
                .id(null)
                .username("usernameD")
                .gameLobbyEntity(gameLobbyEntity)
                .playerColour(null)
                .build();
    }
    public static PlayerEntity createTestPlayerEntityE(final GameLobbyEntity gameLobbyEntity){
        return PlayerEntity.builder()
                .id(null)
                .username("usernameE")
                .gameLobbyEntity(gameLobbyEntity)
                .playerColour(null)
                .build();
    }
    public static PlayerEntity createTestPlayerEntityF(final GameLobbyEntity gameLobbyEntity){
        return PlayerEntity.builder()
                .id(null)
                .username("usernameF")
                .gameLobbyEntity(gameLobbyEntity)
                .playerColour(null)
                .build();
    }

    public static PlayerEntity createTestPlayerEntityG(final GameLobbyEntity gameLobbyEntity){
        return PlayerEntity.builder()
                .id(null)
                .username("usernameG")
                .gameLobbyEntity(gameLobbyEntity)
                .playerColour(null)
                .build();
    }


    public static GameLobbyEntity createTestGameLobbyEntityA() {
        Timestamp timeStamp = Timestamp.valueOf("2024-03-26 15:00:00.000");
        return GameLobbyEntity.builder()
                .id(1L)
                .name("lobbyA")
                .gameStartTimestamp(timeStamp)
                .gameState(GameState.LOBBY.toString())
                .numPlayers(0)
                .availableColours(getTestPlayerColoursAsStringList())
                .build();
    }

    public static GameLobbyEntity createTestGameLobbyEntityB() {
        Timestamp timeStamp = Timestamp.valueOf("2024-03-26 15:01:00.000");
        return GameLobbyEntity.builder()
                .id(2L)
                .name("lobbyB")
                .gameStartTimestamp(timeStamp)
                .gameState(GameState.LOBBY.toString())
                .numPlayers(0)
                .availableColours(getTestPlayerColoursAsStringList())
                .build();
    }

    public static GameLobbyEntity createTestGameLobbyEntityC() {
        Timestamp timeStamp = Timestamp.valueOf("2024-03-26 15:02:00.000");
        return GameLobbyEntity.builder()
                .id(3L)
                .name("lobbyC")
                .gameStartTimestamp(timeStamp)
                .gameState(GameState.LOBBY.toString())
                .numPlayers(0)
                .availableColours(getTestPlayerColoursAsStringList())
                .build();
    }

    public static PlayerDto createTestPlayerDtoA(Long gameLobbyId) {

        return PlayerDto.builder()
                .id(1L)
                .username("usernameA")
                .gameLobbyId(gameLobbyId)
                .playerColour(null)
                .build();
    }

    public static GameLobbyDto createTestGameLobbyDtoA() {
        Timestamp timeStamp = Timestamp.valueOf("2024-03-26 15:00:00.000");
        return GameLobbyDto.builder()
                .id(1L)
                .name("lobbyA")
                .gameStartTimestamp(timeStamp)
                .gameState(GameState.LOBBY)
                .numPlayers(0)
                .availableColours(getTestPlayerColoursAsEnumList())
                .build();
    }

    public static GameLobbyDto createTestGameLobbyDtoB() {
        Timestamp timeStamp = Timestamp.valueOf("2024-03-26 15:00:00.000");
        return GameLobbyDto.builder()
                .id(2L)
                .name("lobbyB")
                .gameStartTimestamp(timeStamp)
                .gameState(GameState.LOBBY)
                .numPlayers(0)
                .availableColours(getTestPlayerColoursAsEnumList())
                .build();
    }

    public static GameSessionDto createTestGameSessionDtoA(PlayerDto lobbyCreator) {
        return GameSessionDto.builder()
                .id(1L)
                .turnPlayerId(lobbyCreator.getId())
                .gameState(GameState.IN_GAME)
                .playerIds(null)
                .build();
    }

    public static GameSessionEntity createTestGameSessionEntityA(PlayerEntity lobbyCreator) {
        return GameSessionEntity.builder()
                .id(1L)
                .turnPlayerId(lobbyCreator.getId())
                .gameState(GameState.IN_GAME.toString())
                .playerIds(null)
                .build();
    }

    public static TileDeckDto createTestTileDeckDtoA(Long gameSessionId) {
        return TileDeckDto.builder()
                .id(1L)
                .tileId(null)
                .gameSessionId(gameSessionId)
                .build();
    }

    public static GameSessionEntity createTestGameSessionEntityWith3Players(){
        List<Long> playerIds = new ArrayList<>();
        playerIds.add(1L);
        playerIds.add(2L);
        playerIds.add(3L);

        return GameSessionEntity.builder()
                .id(1L)
                .turnPlayerId(1L)
                .gameState(GameState.IN_GAME.toString())
                .playerIds(playerIds)
                .tileDeck(null)
                .build();
    }
    public static GameSessionEntity createTestGameSessionEntityWith2Players(){
        List<Long> playerIds = new ArrayList<>();
        playerIds.add(1L);
        playerIds.add(2L);

        return GameSessionEntity.builder()
                .id(1L)
                .turnPlayerId(1L)
                .gameState(GameState.IN_GAME.toString())
                .playerIds(playerIds)
                .numPlayers(2)
                .tileDeck(null)
                .build();
    }
    public static PlacedTileDto createTestPlacedTileDto(Long gameSessionId) {
        return PlacedTileDto.builder()
                .gameSessionId(gameSessionId)
                .tileId(1L)
                .coordinates(new Coordinates(12,12))
                .rotation(1)
                .build();
    }

    public static List<String> getTestPlayerColoursAsStringList() {
        List<String> playerColours = new ArrayList<>();
        playerColours.add(PlayerColour.BLACK.name());
        playerColours.add(PlayerColour.BLUE.name());
        playerColours.add(PlayerColour.GREEN.name());
        playerColours.add(PlayerColour.RED.name());
        playerColours.add(PlayerColour.YELLOW.name());

        return playerColours;
    }

    public static List<PlayerColour> getTestPlayerColoursAsEnumList() {
        List<PlayerColour> playerColours = new ArrayList<>();
        playerColours.add(PlayerColour.BLACK);
        playerColours.add(PlayerColour.BLUE);
        playerColours.add(PlayerColour.GREEN);
        playerColours.add(PlayerColour.RED);
        playerColours.add(PlayerColour.YELLOW);

        return playerColours;
    }

    public static List<String> getTestPlayerColoursAsStringListRemoveValue(String playerColour) {
        List<String> playerColours = getTestPlayerColoursAsStringList();
        playerColours.remove(playerColour);
        return playerColours;
    }

    public static List<String> getTestPlayerColoursAsStringListRemoveValue(List<String> playerColours, String playerColour) {
        playerColours.remove(playerColour);
        return playerColours;
    }

    public static List<PlayerColour> getTestPlayerColoursAsEnumListRemoveValue(PlayerColour playerColour) {
        List<PlayerColour> playerColours = getTestPlayerColoursAsEnumList();
        playerColours.remove(playerColour);

        return playerColours;
    }

    public static List<PlayerColour> getTestPlayerColoursAsEnumListRemoveValue(List<PlayerColour> playerColours, PlayerColour playerColour) {
        playerColours.remove(playerColour);
        return playerColours;
    }

    public static FinishedTurnDto getTestFinishedTurnDto() {
        FinishedTurnDto finishedTurnDto = new FinishedTurnDto();
        finishedTurnDto.setGameSessionId(1L);
        finishedTurnDto.setPoints(Map.of(1L, 0));
        finishedTurnDto.setPlayersWithMeeples(Map.of(1L, new ArrayList<>()));
        return finishedTurnDto;
    }
}
