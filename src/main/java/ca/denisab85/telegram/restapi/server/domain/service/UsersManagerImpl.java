package ca.denisab85.telegram.restapi.server.domain.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.TdApi.UpdateUser;
import org.drinkless.tdlib.TdApi.UpdateUserStatus;
import org.drinkless.tdlib.TdApi.User;

public class UsersManagerImpl implements UsersManager {

  private final ConcurrentMap<Long, User> users = new ConcurrentHashMap<>();

  @Override
  public void updateUser(UpdateUser updateUser) {
    users.put(updateUser.user.id, updateUser.user);
  }

  @Override
  public void updateUserStatus(UpdateUserStatus updateUserStatus) {
    TdApi.User user = users.get(updateUserStatus.userId);
    user.status = updateUserStatus.status;
  }

}
