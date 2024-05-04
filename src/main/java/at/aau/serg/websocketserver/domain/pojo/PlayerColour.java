package at.aau.serg.websocketserver.domain.pojo;

import java.util.Arrays;
import java.util.List;

public enum PlayerColour {
    BLACK,
    RED,
    BLUE,
    GREEN,
    YELLOW;

    public static List<String> getColoursAsList() {
        return Arrays.stream(PlayerColour.values())
                .map(Enum::toString)
                .toList();
    }
}