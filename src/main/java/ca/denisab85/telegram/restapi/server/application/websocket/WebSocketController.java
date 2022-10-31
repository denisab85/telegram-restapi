package ca.denisab85.telegram.restapi.server.application.websocket;

import ca.denisab85.telegram.restapi.server.domain.model.stomp.Greeting;
import ca.denisab85.telegram.restapi.server.domain.model.stomp.HelloMessage;
import ca.denisab85.telegram.restapi.server.domain.model.stomp.MessageReceivedEvent;
import ca.denisab85.telegram.restapi.server.domain.service.TelegramClientService;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

@Controller
public class WebSocketController {

  @Autowired
  private SimpMessagingTemplate simpMessagingTemplate;

  @Autowired
  private TelegramClientService telegramClientService;

  @MessageMapping("/hello")
  @SendTo("/topic/greetings")
  public Greeting send(HelloMessage message) {
    return new Greeting("Hello, " + HtmlUtils.htmlEscape(message.getName()) + "!");
  }

  public void sendSpecific() throws Exception {
    MessageReceivedEvent out = new MessageReceivedEvent(
        "msg.getText()",
        "date:" + new SimpleDateFormat("HH:mm").format(new Date()));
    simpMessagingTemplate.send("/topic/greetings", out);
  }

}
