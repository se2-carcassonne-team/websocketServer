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
    public void updatePlayerPoints(Long playerId, FinishedTurnDto finishedTurnDto, int cheatPoints, int penaltyPoints) {
        Optional<PlayerEntity> playerEntityOptional = playerEntityRepository.findById(playerId);

        if (playerEntityOptional.isPresent()) {
            PlayerEntity playerEntity = playerEntityOptional.get();

            if (playerEntity.isCanCheat()) {
                // cheater: add points to his points in the finishedTurnDto
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
                // if not cheater, reward the player and deduct points from the cheater
                for (Long turnPlayerId : finishedTurnDto.getPoints().keySet()) {
                    if (checkIsPlayerCheater(turnPlayerId)) {
                        // cheater: deduct points in finishedTurnDto
                        finishedTurnDto.getPoints().put(turnPlayerId, penaltyPoints);
                        // assert that the points in the finishedTurnDto are negative
                        assert finishedTurnDto.getPoints().get(turnPlayerId) < 0;
                    } else if (turnPlayerId.equals(playerId)){
                        // non-cheater: reward by adding the cheatpoints to his points in the finishedTurnDto
                        finishedTurnDto.getPoints().put(turnPlayerId, cheatPoints);
                    }
                }
            }

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
//        return Math.round(cheatPoints * 1.25f) * -1;
        return cheatPoints * -1;
    }

    @Override
    public int generateCheatPoints() {
        int maxPoints = 10;
        int minPoints = 4;
        return random.nextInt(maxPoints - minPoints + 1) + minPoints;
    }

    @Override
    public void penalizeForWrongAccusation(Long playerId, FinishedTurnDto finishedTurnDto, Integer penaltyPoints) {
        // update the points map of the finishedturnDto by deducting the penaltyPoints from the players points
        for (Long turnPlayerId : finishedTurnDto.getPoints().keySet()) {
            if (turnPlayerId.equals(playerId)) {
                finishedTurnDto.getPoints().put(turnPlayerId, penaltyPoints);
            }
        }
    }
}
