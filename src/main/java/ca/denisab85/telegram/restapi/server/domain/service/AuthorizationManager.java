package ca.denisab85.telegram.restapi.server.domain.service;

import org.drinkless.tdlib.TdApi.AuthorizationState;

public interface AuthorizationManager {

  void awaitAuthorization() throws InterruptedException;

  boolean isAuthenticated();

  AuthorizationState getAuthorizationState();

  void setAuthorizationState(AuthorizationState authorizationState);

  void setHaveAuthorization(boolean haveAuthorization);
}
