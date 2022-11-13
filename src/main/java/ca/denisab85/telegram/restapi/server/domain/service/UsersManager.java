package ca.denisab85.telegram.restapi.server.domain.service;

import org.drinkless.tdlib.TdApi.UpdateUser;
import org.drinkless.tdlib.TdApi.UpdateUserStatus;

public interface UsersManager {

  void updateUser(UpdateUser updateUser);

  void updateUserStatus(UpdateUserStatus updateUserStatus);

}
