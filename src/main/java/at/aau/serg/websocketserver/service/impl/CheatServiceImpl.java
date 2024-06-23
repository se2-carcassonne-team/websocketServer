package at.aau.serg.websocketserver.service.impl;

import at.aau.serg.websocketserver.domain.dto.FinishedTurnDto;
import at.aau.serg.websocketserver.domain.entity.PlayerEntity;
import at.aau.serg.websocketserver.domain.entity.repository.PlayerEntityRepository;
import at.aau.serg.websocketserver.service.CheatService;
import at.aau.serg.websocketserver.statuscode.ErrorCode;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class CheatServiceImpl implements CheatService {

    SecureRandom random;
    PlayerEntityRepository playerEntityRepository;

    public CheatServiceImpl(PlayerEntityRepository playerEntityRepository) {
        this.random = new SecureRandom();
        this.playerEntityRepository = playerEntityRepository;
    }

    @Override
    public void assignCheatFunctionality(List<PlayerEntity> playerEntityList) {
        if (playerEntityList == null || playerEntityList.isEmpty()) {
            throw new IllegalArgumentException(ErrorCode.ERROR_3004.getCode());
        }
        int randomIndex = random.nextInt(playerEntityList.size());

        PlayerEntity cheatPlayer = playerEntityList.get(randomIndex);
        cheatPlayer.setCanCheat(true);
        playerEntityRepository.save(cheatPlayer);
    }

    @Override
    public void updatePlayerPoints(Long playerId, FinishedTurnDto finishedTurnDto, int cheatPoints) {
        Optional<PlayerEntity> playerEntityOptional = playerEntityRepository.findById(playerId);

        if (playerEntityOptional.isPresent()) {
            PlayerEntity playerEntity = playerEntityOptional.get();
            if (!playerEntity.isCanCheat()) {
                throw new IllegalArgumentException(ErrorCode.ERROR_3005.getCode());
            }

            for (Long turnPlayerId : finishedTurnDto.getPoints().keySet()) {
                if (turnPlayerId.equals(playerId)) {
                    int currentPoints = finishedTurnDto.getPoints().get(turnPlayerId);
                    int updatedPoints = currentPoints + cheatPoints;
                    finishedTurnDto.getPoints().put(turnPlayerId, Math.max(updatedPoints, 0));
                }
            }

            playerEntity.setCanCheat(false);
            playerEntity.setCheatPoints(cheatPoints);
            playerEntityRepository.save(playerEntity);
        } else {
            throw new IllegalArgumentException(ErrorCode.ERROR_2001.getCode());
        }
    }

    @Override
    public Integer getCheatPoints(Long playerId) {
        Optional<PlayerEntity> playerEntityOptional = playerEntityRepository.findById(playerId);

        if (playerEntityOptional.isPresent()) {
            PlayerEntity playerEntity = playerEntityOptional.get();
            return playerEntity.getCheatPoints();
        } else {
            throw new IllegalArgumentException(ErrorCode.ERROR_2001.getCode());
        }
    }

    @Override
    public Boolean checkIsPlayerCheater(Long playerId) {
        Optional<PlayerEntity> playerEntityOptional = playerEntityRepository.findById(playerId);

        if (playerEntityOptional.isPresent()) {
            PlayerEntity playerEntity = playerEntityOptional.get();
            return playerEntity.getCheatPoints() != 0;
        } else {
            throw new IllegalArgumentException(ErrorCode.ERROR_2001.getCode());
        }
    }

    @Override
    public Integer generatePenaltyPoints(Integer cheatPoints) {
        return Math.round(cheatPoints * 1.25f) * -1;
    }

    @Override
    public int generateCheatPoints() {
        int maxPoints = 10;
        int minPoints = 4;
        return random.nextInt(maxPoints - minPoints + 1) + minPoints;
    }
}
