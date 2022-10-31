package ca.denisab85.telegram.restapi.server.domain.model.stomp;

import java.util.HashMap;
import lombok.Getter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@Getter
public class MessageReceivedEvent implements Message<String> {

  private final MessageHeaders headers;

  private final String payload;

  public MessageReceivedEvent(String payload, String... headers) {
    this.payload = payload;
    this.headers = new MessageHeaders(new HashMap<>());
    for (String header : headers) {
      String[] keyValue = header.split(":");
      this.headers.put(keyValue[0], keyValue[1]);
    }
  }

}
