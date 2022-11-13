package ca.denisab85.telegram.restapi.server.domain.service;

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

public interface ChatsManager {

  void updateNewChat(UpdateNewChat updateNewChat);

  void updateChatTitle(UpdateChatTitle updateChatTitle);

  void updateChatPhoto(UpdateChatPhoto updateChatPhoto);

  void updateChatLastMessage(UpdateChatLastMessage updateChatLastMessage);

  void updateChatPosition(UpdateChatPosition updateChatPosition);

  void updateChatReadInbox(UpdateChatReadInbox updateChatReadInbox);

  void updateChatReadOutbox(UpdateChatReadOutbox updateChatReadOutbox);

  void updateChatUnreadMentionCount(UpdateChatUnreadMentionCount updateChatUnreadMentionCount);

  void updateMessageMentionRead(UpdateMessageMentionRead updateMessageMentionRead);

  void updateChatReplyMarkup(UpdateChatReplyMarkup updateChatReplyMarkup);

  void updateChatDraftMessage(UpdateChatDraftMessage updateChatDraftMessage);

  void updateChatPermissions(UpdateChatPermissions updateChatPermissions);

  void updateChatNotificationSettings(
      UpdateChatNotificationSettings updateChatNotificationSettings);

  void updateChatDefaultDisableNotification(
      UpdateChatDefaultDisableNotification updateChatDefaultDisableNotification);

  void updateChatIsMarkedAsUnread(UpdateChatIsMarkedAsUnread updateChatIsMarkedAsUnread);

  void updateChatIsBlocked(UpdateChatIsBlocked updateChatIsBlocked);

  void updateChatHasScheduledMessages(
      UpdateChatHasScheduledMessages updateChatHasScheduledMessages);
}
