package at.aau.serg.websocketserver.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Messages whose destination begins with /app are routed to the @MessageMapping methods in @Controller classes
        config.setApplicationDestinationPrefixes("/app");

        // Broker topics begin with /topic or /queue
        config.enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // HTTP URL for the endpoint
        registry.addEndpoint("/websocket-broker")
                .setAllowedOrigins("*");
    }
}


