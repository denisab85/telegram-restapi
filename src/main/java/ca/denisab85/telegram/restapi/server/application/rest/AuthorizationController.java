package ca.denisab85.telegram.restapi.server.application.rest;

import ca.denisab85.telegram.restapi.server.domain.model.rest.GetStateResponse;
import ca.denisab85.telegram.restapi.server.domain.service.TelegramClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthorizationController {

  @Autowired
  TelegramClientService telegramClientService;

  @GetMapping(value = "/state")
  public ResponseEntity<GetStateResponse> getState() {
    return ResponseEntity.ok().body(new GetStateResponse(telegramClientService.isRunning(),
        telegramClientService.isAuthenticated()));
  }

  @PostMapping(value = "/start")
  public ResponseEntity<GetStateResponse> start() throws InterruptedException {
    telegramClientService.start();
    return ResponseEntity.ok().body(new GetStateResponse(telegramClientService.isRunning(),
        telegramClientService.isAuthenticated()));
  }

  @GetMapping(value = "/getSmsCode")
  public ResponseEntity<String> getSmsCode(@RequestParam String phoneNumber) {
    return ResponseEntity.ok().body("TEST");
  }

}
