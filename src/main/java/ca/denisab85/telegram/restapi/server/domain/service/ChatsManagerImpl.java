package ca.denisab85.telegram.restapi.server.domain.service;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.TdApi.Chat;
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

public class ChatsManagerImpl implements ChatsManager {

  private final ConcurrentMap<Long, Chat> chats = new ConcurrentHashMap<>();
  private final NavigableSet<OrderedChat> mainChatList = new TreeSet<>();

  private void setChatPositions(TdApi.Chat chat, TdApi.ChatPosition[] positions) {
    synchronized (mainChatList) {
      synchronized (chat) {
        for (TdApi.ChatPosition position : chat.positions) {
          if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
            boolean isRemoved = mainChatList.remove(new OrderedChat(chat.id, position));
            assert isRemoved;
          }
        }

        chat.positions = positions;

        for (TdApi.ChatPosition position : chat.positions) {
          if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
            boolean isAdded = mainChatList.add(new OrderedChat(chat.id, position));
            assert isAdded;
          }
        }
      }
    }
  }

  @Override
  public void updateNewChat(UpdateNewChat updateNewChat) {
    TdApi.Chat chat = updateNewChat.chat;
    chats.put(chat.id, chat);
    TdApi.ChatPosition[] positions = chat.positions;
    chat.positions = new TdApi.ChatPosition[0];
    setChatPositions(chat, positions);
  }

  @Override
  public void updateChatTitle(UpdateChatTitle updateChatTitle) {
    TdApi.Chat chat = chats.get(updateChatTitle.chatId);
    chat.title = updateChatTitle.title;
  }

  @Override
  public void updateChatPhoto(UpdateChatPhoto updateChatPhoto) {
    TdApi.Chat chat = chats.get(updateChatPhoto.chatId);
    chat.photo = updateChatPhoto.photo;
  }

  @Override
  public void updateChatLastMessage(UpdateChatLastMessage updateChatLastMessage) {
    TdApi.Chat chat = chats.get(updateChatLastMessage.chatId);
    chat.lastMessage = updateChatLastMessage.lastMessage;
    setChatPositions(chat, updateChatLastMessage.positions);
  }

  @Override
  public void updateChatPosition(UpdateChatPosition updateChatPosition) {
    if (updateChatPosition.position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
      TdApi.Chat chat = chats.get(updateChatPosition.chatId);
      int i;
      for (i = 0; i < chat.positions.length; i++) {
        if (chat.positions[i].list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
          break;
        }
      }
      TdApi.ChatPosition[] new_positions = new TdApi.ChatPosition[
          chat.positions.length + (updateChatPosition.position.order == 0 ? 0 : 1) - (
              i < chat.positions.length ? 1 : 0)];
      int pos = 0;
      if (updateChatPosition.position.order != 0) {
        new_positions[pos++] = updateChatPosition.position;
      }
      for (int j = 0; j < chat.positions.length; j++) {
        if (j != i) {
          new_positions[pos++] = chat.positions[j];
        }
      }
      assert pos == new_positions.length;

      setChatPositions(chat, new_positions);
    }
  }

  @Override
  public void updateChatReadInbox(UpdateChatReadInbox updateChatReadInbox) {
    TdApi.Chat chat = chats.get(updateChatReadInbox.chatId);
    chat.lastReadInboxMessageId = updateChatReadInbox.lastReadInboxMessageId;
    chat.unreadCount = updateChatReadInbox.unreadCount;
  }

  @Override
  public void updateChatReadOutbox(UpdateChatReadOutbox updateChatReadOutbox) {
    TdApi.Chat chat = chats.get(updateChatReadOutbox.chatId);
    chat.lastReadOutboxMessageId = updateChatReadOutbox.lastReadOutboxMessageId;
  }

  @Override
  public void updateChatUnreadMentionCount(
      UpdateChatUnreadMentionCount updateChatUnreadMentionCount) {
    TdApi.Chat chat = chats.get(updateChatUnreadMentionCount.chatId);
    chat.unreadMentionCount = updateChatUnreadMentionCount.unreadMentionCount;
  }

  @Override
  public void updateMessageMentionRead(UpdateMessageMentionRead updateMessageMentionRead) {
    TdApi.Chat chat = chats.get(updateMessageMentionRead.chatId);
    chat.unreadMentionCount = updateMessageMentionRead.unreadMentionCount;
  }

  @Override
  public void updateChatReplyMarkup(UpdateChatReplyMarkup updateChatReplyMarkup) {
    TdApi.Chat chat = chats.get(updateChatReplyMarkup.chatId);
    chat.replyMarkupMessageId = updateChatReplyMarkup.replyMarkupMessageId;
  }

  @Override
  public void updateChatDraftMessage(UpdateChatDraftMessage updateChatDraftMessage) {
    TdApi.Chat chat = chats.get(updateChatDraftMessage.chatId);
    chat.draftMessage = updateChatDraftMessage.draftMessage;
    setChatPositions(chat, updateChatDraftMessage.positions);
  }

  @Override
  public void updateChatPermissions(UpdateChatPermissions updateChatPermissions) {
    TdApi.Chat chat = chats.get(updateChatPermissions.chatId);
    chat.permissions = updateChatPermissions.permissions;
  }

  @Override
  public void updateChatNotificationSettings(
      UpdateChatNotificationSettings updateChatNotificationSettings) {
    TdApi.Chat chat = chats.get(updateChatNotificationSettings.chatId);
    chat.notificationSettings = updateChatNotificationSettings.notificationSettings;
  }

  @Override
  public void updateChatDefaultDisableNotification(
      UpdateChatDefaultDisableNotification updateChatDefaultDisableNotification) {
    TdApi.Chat chat = chats.get(updateChatDefaultDisableNotification.chatId);
    chat.defaultDisableNotification = updateChatDefaultDisableNotification.defaultDisableNotification;
  }

  @Override
  public void updateChatIsMarkedAsUnread(UpdateChatIsMarkedAsUnread updateChatIsMarkedAsUnread) {
    TdApi.Chat chat = chats.get(updateChatIsMarkedAsUnread.chatId);
    chat.isMarkedAsUnread = updateChatIsMarkedAsUnread.isMarkedAsUnread;
  }

  @Override
  public void updateChatIsBlocked(UpdateChatIsBlocked updateChatIsBlocked) {
    TdApi.Chat chat = chats.get(updateChatIsBlocked.chatId);
    chat.isBlocked = updateChatIsBlocked.isBlocked;
  }

  @Override
  public void updateChatHasScheduledMessages(
      UpdateChatHasScheduledMessages updateChatHasScheduledMessages) {
    TdApi.Chat chat = chats.get(updateChatHasScheduledMessages.chatId);
    chat.hasScheduledMessages = updateChatHasScheduledMessages.hasScheduledMessages;
  }

  private static class OrderedChat implements Comparable<OrderedChat> {

    final long chatId;
    final TdApi.ChatPosition position;

    OrderedChat(long chatId, TdApi.ChatPosition position) {
      this.chatId = chatId;
      this.position = position;
    }

    @Override
    public int compareTo(OrderedChat o) {
      if (this.position.order != o.position.order) {
        return o.position.order < this.position.order ? -1 : 1;
      }
      if (this.chatId != o.chatId) {
        return o.chatId < this.chatId ? -1 : 1;
      }
      return 0;
    }

    @Override
    public boolean equals(Object obj) {
      OrderedChat o = (OrderedChat) obj;
      return this.chatId == o.chatId && this.position.order == o.position.order;
    }
  }
}
