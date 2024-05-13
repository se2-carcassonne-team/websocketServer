package at.aau.serg.websocketserver.statuscode;

public enum ResponseCode {
    RESPONSE_101("received updated playerDto", "101"),
    RESPONSE_102("placeholder", "102"),
    RESPONSE_103("player successfully deleted", "103"),
    RESPONSE_201("received updated list of players in the current lobby", "201"),
    RESPONSE_202("received updated name of current lobby", "202"),
    RESPONSE_203("lobby was deleted", "203"),
    RESPONSE_301("received list of lobbies currently in database", "301"),
    ;

    private final String responseDescription;
    private final String code;

    /**
     * Enum of standardized custom response codes.
     * @param responseDescription short description of the response code meaning
     * @param code response code: 10X for player updates, 20X for lobby updates, 30X for list of lobbies
     */
    ResponseCode(String responseDescription, String code) {
        this.responseDescription = responseDescription;
        this.code = code;
    }

    public String getResponseDescription() {
        return responseDescription;
    }

    public String getCode() {
        return code;
    }
}
