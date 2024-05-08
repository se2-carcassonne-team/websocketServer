package at.aau.serg.websocketserver.domain.pojo;

import java.util.Arrays;
import java.util.List;

public enum PlayerColour {
    BLACK,
    BLUE,
    GREEN,
    RED,
    YELLOW;

    public static List<String> getColoursAsList() {
        return Arrays.stream(PlayerColour.values())
                .map(Enum::toString)
                .toList();
    }
}