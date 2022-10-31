package ca.denisab85.telegram.restapi.server.domain.service;

import org.drinkless.tdlib.TdApi.Error;

public class TelegramException extends RuntimeException {

  private final Error error;

  public TelegramException(Error error) {
    this.error = error;
  }

  @Override
  public String getMessage() {
    return error.message;
  }

  public int getCode() {
    return this.error.code;
  }

}
