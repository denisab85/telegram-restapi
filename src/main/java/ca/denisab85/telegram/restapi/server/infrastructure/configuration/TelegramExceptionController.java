package ca.denisab85.telegram.restapi.server.infrastructure.configuration;

import ca.denisab85.telegram.restapi.server.domain.service.TelegramException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class TelegramExceptionController {

  @ExceptionHandler(value = TelegramException.class)
  public ResponseEntity<Object> exception(TelegramException exception) {
    return new ResponseEntity<>(exception.getMessage(), HttpStatus.valueOf(exception.getCode()));
  }

}
