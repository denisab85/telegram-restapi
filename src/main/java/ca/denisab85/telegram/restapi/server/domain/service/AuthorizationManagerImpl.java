package ca.denisab85.telegram.restapi.server.domain.service;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.TdApi;

@Slf4j
public class AuthorizationManagerImpl implements AuthorizationManager {

  private final Lock authorizationLock = new ReentrantLock();
  private final Condition gotAuthorization = authorizationLock.newCondition();
  @Getter
  @Setter
  private TdApi.AuthorizationState authorizationState = null;
  @Getter
  private boolean haveAuthorization = false;

  @Override
  public void awaitAuthorization() throws InterruptedException {
    this.authorizationLock.lock();
    try {
      while (!this.haveAuthorization) {
        this.gotAuthorization.await();
      }
    } finally {
      this.authorizationLock.unlock();
    }
  }

  @Override
  public boolean isAuthenticated() {
    return haveAuthorization;
  }

  @Override
  public void setHaveAuthorization(boolean haveAuthorization) {
    this.haveAuthorization = haveAuthorization;
    if (haveAuthorization) {
      authorizationLock.lock();
      try {
        gotAuthorization.signal();
      } finally {
        authorizationLock.unlock();
      }
    }
  }

}
