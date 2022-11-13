package ca.denisab85.telegram.restapi.server.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.TdApi.UpdateBasicGroup;
import org.drinkless.tdlib.TdApi.UpdateBasicGroupFullInfo;
import org.drinkless.tdlib.TdApi.UpdateSecretChat;
import org.drinkless.tdlib.TdApi.UpdateSupergroup;
import org.drinkless.tdlib.TdApi.UpdateSupergroupFullInfo;
import org.drinkless.tdlib.TdApi.UpdateUserFullInfo;

@Slf4j
public abstract class UpdateHandler implements Client.ResultHandler {

  @Override
  public void onResult(TdApi.Object object) {
    switch (object.getConstructor()) {
      case UpdateBasicGroup.CONSTRUCTOR:
        TdApi.UpdateBasicGroup updateBasicGroup = (TdApi.UpdateBasicGroup) object;
        break;
      case UpdateSupergroup.CONSTRUCTOR:
        TdApi.UpdateSupergroup updateSupergroup = (TdApi.UpdateSupergroup) object;
        break;
      case UpdateSecretChat.CONSTRUCTOR:
        TdApi.UpdateSecretChat updateSecretChat = (TdApi.UpdateSecretChat) object;
        break;
      case UpdateUserFullInfo.CONSTRUCTOR:
        TdApi.UpdateUserFullInfo updateUserFullInfo = (TdApi.UpdateUserFullInfo) object;
        break;
      case UpdateBasicGroupFullInfo.CONSTRUCTOR:
        TdApi.UpdateBasicGroupFullInfo updateBasicGroupFullInfo = (TdApi.UpdateBasicGroupFullInfo) object;
        break;
      case UpdateSupergroupFullInfo.CONSTRUCTOR:
        TdApi.UpdateSupergroupFullInfo updateSupergroupFullInfo = (TdApi.UpdateSupergroupFullInfo) object;
        break;
    }
  }
}
