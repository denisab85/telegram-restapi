package ca.denisab85.telegram.restapi.server.infrastructure.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic");
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/gs-guide-websocket").setAllowedOriginPatterns("*").withSockJS();
  }

//	@Override
//	public void configureWebSocketTransport(WebSocketTransportRegistration registry)
//	{
//		registry.setMessageSizeLimit(2048000);
//		WebSocketMessageBrokerConfigurer.super.configureWebSocketTransport(registry);
//	}

}