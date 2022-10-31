package ca.denisab85.telegram.restapi.server.application.rest;

import ca.denisab85.telegram.restapi.server.domain.service.ChatService;
import ca.denisab85.telegram.restapi.server.domain.service.TelegramClientService;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.TdApi.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ChatController {

  @Autowired
  TelegramClientService telegramClientService;

  @Autowired
  ChatService chatService;

  @GetMapping(value = "/chats")
  public ResponseEntity<List<Long>> getChatIds(@RequestParam int limit)
      throws ExecutionException, InterruptedException {
    return ResponseEntity.ok().body(chatService.getChatIds(limit).get());
  }

  @GetMapping(value = "/chat/{chatId}/history")
  public ResponseEntity<List<Message>> getChatHistory(@PathVariable long chatId,
      @RequestParam long fromMessageId, @RequestParam int offset, @RequestParam int limit)
      throws ExecutionException, InterruptedException {
    return ResponseEntity.ok()
        .body(chatService.getChatHistory(chatId, fromMessageId, offset, limit).get());
  }

}
