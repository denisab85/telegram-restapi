//
// Copyright Aliaksei Levin (levlam@telegram.org), Arseny Smirnov (arseny30@gmail.com) 2014-2022
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package ca.denisab85.telegram.restapi.server.domain.service;

import java.io.IOError;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.Client.ResultHandler;
import org.drinkless.tdlib.TdApi;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public final class TelegramClientService {

  private AuthorizationManagerImpl authorizationManager;
  private UsersManager usersManager;
  private ChatsManager chatsManager;
  @Getter
  private Client client = null;

  private static void onFatalError(String errorMessage) {
    final class ThrowError implements Runnable {

      private final String errorMessage;
      private final AtomicLong errorThrowTime;

      private ThrowError(String errorMessage, AtomicLong errorThrowTime) {
        this.errorMessage = errorMessage;
        this.errorThrowTime = errorThrowTime;
      }

      @Override
      public void run() {
        if (isDatabaseBrokenError(errorMessage) || isDiskFullError(errorMessage) || isDiskError(
            errorMessage)) {
          processExternalError();
          return;
        }

        errorThrowTime.set(System.currentTimeMillis());
        throw new ClientError("TDLib fatal error: " + errorMessage);
      }

      private void processExternalError() {
        errorThrowTime.set(System.currentTimeMillis());
        throw new ExternalClientError("Fatal error: " + errorMessage);
      }

      private boolean isDatabaseBrokenError(String message) {
        return message.contains("Wrong key or database is corrupted") || message.contains(
            "SQL logic error or missing database") || message.contains(
            "database disk image is malformed") || message.contains(
            "file is encrypted or is not a database") || message.contains("unsupported file format")
            || message.contains(
            "Database was corrupted and deleted during execution and can't be recreated");
      }

      private boolean isDiskFullError(String message) {
        return message.contains("PosixError : No space left on device") || message.contains(
            "database or disk is full");
      }

      private boolean isDiskError(String message) {
        return message.contains("I/O error") || message.contains("Structure needs cleaning");
      }

      final class ClientError extends Error {

        private ClientError(String message) {
          super(message);
        }
      }

      final class ExternalClientError extends Error {

        public ExternalClientError(String message) {
          super(message);
        }
      }
    }

    final AtomicLong errorThrowTime = new AtomicLong(Long.MAX_VALUE);
    new Thread(new ThrowError(errorMessage, errorThrowTime), "TDLib fatal error thread").start();

    // wait at least 10 seconds after the error is thrown
    while (errorThrowTime.get() >= System.currentTimeMillis() - 10000) {
      try {
        Thread.sleep(1000 /* milliseconds */);
      } catch (InterruptedException ignore) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public void start() throws InterruptedException {
    if (this.client == null) {
      // set log message handler to handle only fatal errors (0) and plain log messages (-1)
      Client.setLogMessageHandler(0, new LogMessageHandler());

      // disable TDLib log and redirect fatal errors and plain log messages to a file
      Client.execute(new TdApi.SetLogVerbosityLevel(0));
      if (Client.execute(new TdApi.SetLogStream(
          new TdApi.LogStreamFile("tdlib.log", 1 << 27, false))) instanceof TdApi.Error) {
        throw new IOError(new IOException("Write access to the current directory is required"));
      }

      // Prepare message handlers
      HandlerDispatcher handlerDispatcher = new HandlerDispatcher();
      this.authorizationManager = new AuthorizationManagerImpl();
      this.usersManager = new UsersManagerImpl();
      this.chatsManager = new ChatsManagerImpl();
      AuthorizationHandler authorizationHandler = new AuthorizationHandler(authorizationManager,
          this::getClient);
      UsersUpdateHandler usersUpdateHandler = new UsersUpdateHandler(this.usersManager);
      ChatsUpdateHandler chatsUpdateHandler = new ChatsUpdateHandler(this.chatsManager);
      handlerDispatcher.addResultHandler(authorizationHandler, usersUpdateHandler,
          chatsUpdateHandler);
      // create client
      client = Client.create(handlerDispatcher, null, null);
      authorizationManager.awaitAuthorization();
    }
  }

  public boolean isRunning() {
    return client != null;
  }

  public boolean isAuthenticated() {
    return authorizationManager != null && authorizationManager.isAuthenticated();
  }


  private static class LogMessageHandler implements Client.LogMessageHandler {

    @Override
    public void onLogMessage(int verbosityLevel, String message) {
      if (verbosityLevel == 0) {
        onFatalError(message);
        return;
      }
      log.error(message);
    }
  }

  private class HandlerDispatcher implements Client.ResultHandler {

    private final Set<ResultHandler> resultHandlers = new HashSet<>();

    public boolean addResultHandler(Client.ResultHandler... resultHandler) {
      return this.resultHandlers.addAll(Arrays.asList(resultHandler));
    }

    @Override
    public void onResult(TdApi.Object object) {
      this.resultHandlers.forEach(rh -> rh.onResult(object));
    }
  }

}
