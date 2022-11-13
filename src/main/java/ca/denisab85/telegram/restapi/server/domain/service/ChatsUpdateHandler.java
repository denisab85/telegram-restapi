package ca.denisab85.telegram.restapi.server.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.TdApi.UpdateChatDefaultDisableNotification;
import org.drinkless.tdlib.TdApi.UpdateChatDraftMessage;
import org.drinkless.tdlib.TdApi.UpdateChatHasScheduledMessages;
import org.drinkless.tdlib.TdApi.UpdateChatIsBlocked;
import org.drinkless.tdlib.TdApi.UpdateChatIsMarkedAsUnread;
import org.drinkless.tdlib.TdApi.UpdateChatLastMessage;
import org.drinkless.tdlib.TdApi.UpdateChatNotificationSettings;
import org.drinkless.tdlib.TdApi.UpdateChatPermissions;
import org.drinkless.tdlib.TdApi.UpdateChatPhoto;
import org.drinkless.tdlib.TdApi.UpdateChatPosition;
import org.drinkless.tdlib.TdApi.UpdateChatReadInbox;
import org.drinkless.tdlib.TdApi.UpdateChatReadOutbox;
import org.drinkless.tdlib.TdApi.UpdateChatReplyMarkup;
import org.drinkless.tdlib.TdApi.UpdateChatTitle;
import org.drinkless.tdlib.TdApi.UpdateChatUnreadMentionCount;
import org.drinkless.tdlib.TdApi.UpdateMessageMentionRead;
import org.drinkless.tdlib.TdApi.UpdateNewChat;

@Slf4j
public class ChatsUpdateHandler implements Client.ResultHandler {

  private final ChatsManager chatsManager;

  public ChatsUpdateHandler(ChatsManager chatsManager) {
    this.chatsManager = chatsManager;
  }

  @Override
  public void onResult(TdApi.Object object) {
    switch (object.getConstructor()) {
      case UpdateNewChat.CONSTRUCTOR: {
        chatsManager.updateNewChat((UpdateNewChat) object);
        break;
      }
      case UpdateChatTitle.CONSTRUCTOR: {
        chatsManager.updateChatTitle((UpdateChatTitle) object);
        break;
      }
      case UpdateChatPhoto.CONSTRUCTOR: {
        chatsManager.updateChatPhoto((UpdateChatPhoto) object);
        break;
      }
      case UpdateChatLastMessage.CONSTRUCTOR: {
        chatsManager.updateChatLastMessage((UpdateChatLastMessage) object);
        break;
      }
      case UpdateChatPosition.CONSTRUCTOR: {
        chatsManager.updateChatPosition((UpdateChatPosition) object);
        break;
      }
      case UpdateChatReadInbox.CONSTRUCTOR: {
        chatsManager.updateChatReadInbox((UpdateChatReadInbox) object);
        break;
      }
      case UpdateChatReadOutbox.CONSTRUCTOR: {
        chatsManager.updateChatReadOutbox((UpdateChatReadOutbox) object);
        break;
      }
      case UpdateChatUnreadMentionCount.CONSTRUCTOR: {
        chatsManager.updateChatUnreadMentionCount((UpdateChatUnreadMentionCount) object);
        break;
      }
      case UpdateMessageMentionRead.CONSTRUCTOR: {
        chatsManager.updateMessageMentionRead((UpdateMessageMentionRead) object);
        break;
      }
      case UpdateChatReplyMarkup.CONSTRUCTOR: {
        chatsManager.updateChatReplyMarkup((UpdateChatReplyMarkup) object);
        break;
      }
      case UpdateChatDraftMessage.CONSTRUCTOR: {
        chatsManager.updateChatDraftMessage((UpdateChatDraftMessage) object);
        break;
      }
      case UpdateChatPermissions.CONSTRUCTOR: {
        chatsManager.updateChatPermissions((UpdateChatPermissions) object);
        break;
      }
      case UpdateChatNotificationSettings.CONSTRUCTOR: {
        chatsManager.updateChatNotificationSettings((UpdateChatNotificationSettings) object);
        break;
      }
      case UpdateChatDefaultDisableNotification.CONSTRUCTOR: {
        chatsManager.updateChatDefaultDisableNotification(
            (UpdateChatDefaultDisableNotification) object);
        break;
      }
      case UpdateChatIsMarkedAsUnread.CONSTRUCTOR: {
        chatsManager.updateChatIsMarkedAsUnread((UpdateChatIsMarkedAsUnread) object);
        break;
      }
      case UpdateChatIsBlocked.CONSTRUCTOR: {
        chatsManager.updateChatIsBlocked((UpdateChatIsBlocked) object);
        break;
      }
      case UpdateChatHasScheduledMessages.CONSTRUCTOR: {
        chatsManager.updateChatHasScheduledMessages((UpdateChatHasScheduledMessages) object);
        break;
      }
    }

  }

}
