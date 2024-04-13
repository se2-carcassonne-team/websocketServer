package at.aau.serg.websocketserver.demo;

import static org.assertj.core.api.Assertions.assertThat;

//@ExtendWith(SpringExtension.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//class WebSocketHandlerIntegrationTest {
//
//    @LocalServerPort
//    private int port;
//
//    private final String WEBSOCKET_URI = "ws://localhost:%d/websocket-example-handler";
//
//    /**
//     * Queue of messages from the server.
//     */
//    BlockingQueue<String> messages = new LinkedBlockingDeque<>();
//
//    @Test
//    public void testWebSocketMessageBroker() throws Exception {
//        WebSocketSession session = initStompSession();
//
//        // send a message to the server
//        String message = "Test message";
//        session.sendMessage(new TextMessage(message));
//
//        var expectedResponse = "echo from handler: " + message;
//        assertThat(messages.poll(1, TimeUnit.SECONDS)).isEqualTo(expectedResponse);
//    }
//
//    /**
//     * @return The basic session for the WebSocket connection.
//     */
//    public WebSocketSession initStompSession() throws Exception {
//        WebSocketClient client = new StandardWebSocketClient();
//
//        // connect client to the websocket server
//        WebSocketSession session = client.execute(new WebSocketHandlerClientImpl(messages), // pass the message list
//                        String.format(WEBSOCKET_URI, port))
//                // wait 1 sec for the client to be connected
//                .get(1, TimeUnit.SECONDS);
//
//        return session;
//    }
//
//}

