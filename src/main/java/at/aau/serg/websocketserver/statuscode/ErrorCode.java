package at.aau.serg.websocketserver.statuscode;

// fixme use meaningful enum names
public enum ErrorCode {
    ERROR_1001("gameLobby with the id already exists", "1001"),
    ERROR_1002("gameLobby with the name already exists", "1002"),
    ERROR_1003("gameLobby with the id does not exist", "1003"),
    ERROR_1004("gameLobby is already full (5/5 players)", "1004"),
    ERROR_1005("gameLobby id NumberFormat is invalid", "1005"),
    ERROR_1006("invalid gameLobbyDto JSON", "1006"),
    ERROR_2001("player with the id does not exist", "2001"),
    ERROR_2002("player with the id already exists", "2002"),
    ERROR_2003("player with the username already exists", "2003"),
    ERROR_2004("invalid playerDto JSON", "2004"),
    ERROR_2005("player is not in a gameLobby", "2005"),
    ERROR_2006("player still exists", "2006"),
    ERROR_3003("gameSession with the id does not exist", "3003");

    private final String errorDescription;
    private final String code;


    /**
     * Enum of standardized custom response codes.
     * @param errorDescription short description of the error associated with the error code
     * @param code error codes: 100X for gameLobby errors, 200X for PlayerDto errors
     */
    ErrorCode(String errorDescription, String code) {
        this.errorDescription = errorDescription;
        this.code = code;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public String getCode() {
        return code;
    }
}
