package ca.denisab85.telegram.restapi.server.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.TdApi.UpdateUser;
import org.drinkless.tdlib.TdApi.UpdateUserStatus;

@Slf4j
public class UsersUpdateHandler implements Client.ResultHandler {

  private final UsersManager usersManager;

  public UsersUpdateHandler(UsersManager usersManager) {
    this.usersManager = usersManager;
  }

  @Override
  public void onResult(TdApi.Object object) {
    switch (object.getConstructor()) {
      case UpdateUser.CONSTRUCTOR:
        this.usersManager.updateUser((UpdateUser) object);
        break;
      case UpdateUserStatus.CONSTRUCTOR: {
        this.usersManager.updateUserStatus((UpdateUserStatus) object);
        break;
      }
    }
  }
}
