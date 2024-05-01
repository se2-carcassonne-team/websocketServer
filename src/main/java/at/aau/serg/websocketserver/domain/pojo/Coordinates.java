package at.aau.serg.websocketserver.domain.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Plain old java object to hold the coordinates of an objects
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Coordinates {
    private int xPosition;
    private int yPosition;
}
