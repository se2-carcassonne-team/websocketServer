package at.aau.serg.websocketserver.controller.helper;

import at.aau.serg.websocketserver.domain.dto.GameLobbyDto;
import at.aau.serg.websocketserver.domain.dto.PlayerDto;
import at.aau.serg.websocketserver.domain.entity.GameLobbyEntity;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.mapper.GameLobbyMapper;
import at.aau.serg.websocketserver.mapper.PlayerMapper;
import at.aau.serg.websocketserver.service.GameLobbyEntityService;
import at.aau.serg.websocketserver.service.PlayerEntityService;

import java.util.ArrayList;
import java.util.List;

public class HelperMethods {


    public static List<PlayerDto> getPlayerDtosInLobbyList(Long gameLobbyId, GameLobbyEntityService gameLobbyEntityService, PlayerEntityService playerEntityService, PlayerMapper playerMapper) {
        List<PlayerDto> playerDtos = new ArrayList<>();
        if (gameLobbyEntityService.findById(gameLobbyId).isEmpty()){
            return playerDtos;
        }

        List<PlayerEntity> playerEntityList = playerEntityService.getAllPlayersForLobby(gameLobbyId);

        for (PlayerEntity playerEntity : playerEntityList) {
            playerDtos.add(playerMapper.mapToDto(playerEntity));
        }
        return playerDtos;
    }

    public static List<GameLobbyDto> getGameLobbyDtoList(GameLobbyEntityService gameLobbyEntityService, GameLobbyMapper gameLobbyMapper) {
        List<GameLobbyEntity> gameLobbyEntities = gameLobbyEntityService.getListOfLobbies();
        List<GameLobbyDto> gameLobbyDtos = new ArrayList<>();
        if(gameLobbyEntities.isEmpty()){
            return gameLobbyDtos;
        }

        for (GameLobbyEntity gameLobbyEntity : gameLobbyEntities) {
            gameLobbyDtos.add(gameLobbyMapper.mapToDto(gameLobbyEntity));
        }
        return gameLobbyDtos;
    }


}
