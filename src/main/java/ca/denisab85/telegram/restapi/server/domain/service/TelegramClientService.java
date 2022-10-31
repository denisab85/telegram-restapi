//
// Copyright Aliaksei Levin (levlam@telegram.org), Arseny Smirnov (arseny30@gmail.com) 2014-2022
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package ca.denisab85.telegram.restapi.server.domain.service;

import static org.drinkless.tdlib.TdApi.UpdateAuthorizationState;
import static org.drinkless.tdlib.TdApi.UpdateBasicGroup;
import static org.drinkless.tdlib.TdApi.UpdateBasicGroupFullInfo;
import static org.drinkless.tdlib.TdApi.UpdateChatDefaultDisableNotification;
import static org.drinkless.tdlib.TdApi.UpdateChatDraftMessage;
import static org.drinkless.tdlib.TdApi.UpdateChatHasScheduledMessages;
import static org.drinkless.tdlib.TdApi.UpdateChatIsBlocked;
import static org.drinkless.tdlib.TdApi.UpdateChatIsMarkedAsUnread;
import static org.drinkless.tdlib.TdApi.UpdateChatLastMessage;
import static org.drinkless.tdlib.TdApi.UpdateChatNotificationSettings;
import static org.drinkless.tdlib.TdApi.UpdateChatPermissions;
import static org.drinkless.tdlib.TdApi.UpdateChatPhoto;
import static org.drinkless.tdlib.TdApi.UpdateChatPosition;
import static org.drinkless.tdlib.TdApi.UpdateChatReadInbox;
import static org.drinkless.tdlib.TdApi.UpdateChatReadOutbox;
import static org.drinkless.tdlib.TdApi.UpdateChatReplyMarkup;
import static org.drinkless.tdlib.TdApi.UpdateChatTitle;
import static org.drinkless.tdlib.TdApi.UpdateChatUnreadMentionCount;
import static org.drinkless.tdlib.TdApi.UpdateMessageMentionRead;
import static org.drinkless.tdlib.TdApi.UpdateNewChat;
import static org.drinkless.tdlib.TdApi.UpdateSecretChat;
import static org.drinkless.tdlib.TdApi.UpdateSupergroup;
import static org.drinkless.tdlib.TdApi.UpdateSupergroupFullInfo;
import static org.drinkless.tdlib.TdApi.UpdateUser;
import static org.drinkless.tdlib.TdApi.UpdateUserFullInfo;
import static org.drinkless.tdlib.TdApi.UpdateUserStatus;

import java.io.IOError;
import java.io.IOException;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public final class TelegramClientService {

  private final Client.ResultHandler defaultHandler = new DefaultHandler();
  private final ConcurrentMap<Long, TdApi.User> users = new ConcurrentHashMap<>();
  private final ConcurrentMap<Long, TdApi.Chat> chats = new ConcurrentHashMap<>();
  private final NavigableSet<OrderedChat> mainChatList = new TreeSet<>();
  public AuthorizationService authorizationService;
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
    // set log message handler to handle only fatal errors (0) and plain log messages (-1)
    Client.setLogMessageHandler(0, new LogMessageHandler());

    // disable TDLib log and redirect fatal errors and plain log messages to a file
    Client.execute(new TdApi.SetLogVerbosityLevel(0));
    if (Client.execute(new TdApi.SetLogStream(
        new TdApi.LogStreamFile("tdlib.log", 1 << 27, false))) instanceof TdApi.Error) {
      throw new IOError(new IOException("Write access to the current directory is required"));
    }

    // create client
    client = Client.create(new UpdateHandler(), null, null);
    authorizationService = new AuthorizationService(this);
    // test Client.execute
    defaultHandler.onResult(Client.execute(new TdApi.GetTextEntities(
        "@telegram /test_command https://telegram.org telegram.me @gif @test")));

    // main loop
//    while (true) {
    // await authorization
    this.authorizationService.authorizationLock.lock();
    try {
      while (!this.authorizationService.haveAuthorization) {
        this.authorizationService.gotAuthorization.await();
      }
    } finally {
      this.authorizationService.authorizationLock.unlock();
    }
//    }
  }

  public boolean isRunning() {
    return client != null;
  }

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

  private static class DefaultHandler implements Client.ResultHandler {

    @Override
    public void onResult(TdApi.Object object) {
      log.info(object.toString());
    }
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

  private class UpdateHandler implements Client.ResultHandler {

    @Override
    public void onResult(TdApi.Object object) {
      switch (object.getConstructor()) {
        case UpdateAuthorizationState.CONSTRUCTOR:
          authorizationService.onAuthorizationStateUpdated(
              ((TdApi.UpdateAuthorizationState) object).authorizationState);
          break;

        case UpdateUser.CONSTRUCTOR:
          TdApi.UpdateUser updateUser = (TdApi.UpdateUser) object;
          users.put(updateUser.user.id, updateUser.user);
          break;
        case UpdateUserStatus.CONSTRUCTOR: {
          TdApi.UpdateUserStatus updateUserStatus = (TdApi.UpdateUserStatus) object;
          TdApi.User user = users.get(updateUserStatus.userId);
          synchronized (user) {
            user.status = updateUserStatus.status;
          }
          break;
        }
        case UpdateBasicGroup.CONSTRUCTOR:
          TdApi.UpdateBasicGroup updateBasicGroup = (TdApi.UpdateBasicGroup) object;
          break;
        case UpdateSupergroup.CONSTRUCTOR:
          TdApi.UpdateSupergroup updateSupergroup = (TdApi.UpdateSupergroup) object;
          break;
        case UpdateSecretChat.CONSTRUCTOR:
          TdApi.UpdateSecretChat updateSecretChat = (TdApi.UpdateSecretChat) object;
          break;

        case UpdateNewChat.CONSTRUCTOR: {
          TdApi.UpdateNewChat updateNewChat = (TdApi.UpdateNewChat) object;
          TdApi.Chat chat = updateNewChat.chat;
          synchronized (chat) {
            chats.put(chat.id, chat);

            TdApi.ChatPosition[] positions = chat.positions;
            chat.positions = new TdApi.ChatPosition[0];
            setChatPositions(chat, positions);
          }
          break;
        }
        case UpdateChatTitle.CONSTRUCTOR: {
          TdApi.UpdateChatTitle updateChat = (TdApi.UpdateChatTitle) object;
          TdApi.Chat chat = chats.get(updateChat.chatId);
          synchronized (chat) {
            chat.title = updateChat.title;
          }
          break;
        }
        case UpdateChatPhoto.CONSTRUCTOR: {
          TdApi.UpdateChatPhoto updateChat = (TdApi.UpdateChatPhoto) object;
          TdApi.Chat chat = chats.get(updateChat.chatId);
          synchronized (chat) {
            chat.photo = updateChat.photo;
          }
          break;
        }
        case UpdateChatLastMessage.CONSTRUCTOR: {
          TdApi.UpdateChatLastMessage updateChat = (TdApi.UpdateChatLastMessage) object;
          TdApi.Chat chat = chats.get(updateChat.chatId);
          synchronized (chat) {
            chat.lastMessage = updateChat.lastMessage;
            setChatPositions(chat, updateChat.positions);
          }
          break;
        }
        case UpdateChatPosition.CONSTRUCTOR: {
          TdApi.UpdateChatPosition updateChat = (TdApi.UpdateChatPosition) object;
          if (updateChat.position.list.getConstructor() != TdApi.ChatListMain.CONSTRUCTOR) {
            break;
          }

          TdApi.Chat chat = chats.get(updateChat.chatId);
          synchronized (chat) {
            int i;
            for (i = 0; i < chat.positions.length; i++) {
              if (chat.positions[i].list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                break;
              }
            }
            TdApi.ChatPosition[] new_positions = new TdApi.ChatPosition[
                chat.positions.length + (updateChat.position.order == 0 ? 0 : 1) - (
                    i < chat.positions.length ? 1 : 0)];
            int pos = 0;
            if (updateChat.position.order != 0) {
              new_positions[pos++] = updateChat.position;
            }
            for (int j = 0; j < chat.positions.length; j++) {
              if (j != i) {
                new_positions[pos++] = chat.positions[j];
              }
            }
            assert pos == new_positions.length;

            setChatPositions(chat, new_positions);
          }
          break;
        }
        case UpdateChatReadInbox.CONSTRUCTOR: {
          TdApi.UpdateChatReadInbox updateChat = (TdApi.UpdateChatReadInbox) object;
          TdApi.Chat chat = chats.get(updateChat.chatId);
          synchronized (chat) {
            chat.lastReadInboxMessageId = updateChat.lastReadInboxMessageId;
            chat.unreadCount = updateChat.unreadCount;
          }
          break;
        }
        case UpdateChatReadOutbox.CONSTRUCTOR: {
          TdApi.UpdateChatReadOutbox updateChat = (TdApi.UpdateChatReadOutbox) object;
          TdApi.Chat chat = chats.get(updateChat.chatId);
          synchronized (chat) {
            chat.lastReadOutboxMessageId = updateChat.lastReadOutboxMessageId;
          }
          break;
        }
        case UpdateChatUnreadMentionCount.CONSTRUCTOR: {
          TdApi.UpdateChatUnreadMentionCount updateChat = (TdApi.UpdateChatUnreadMentionCount) object;
          TdApi.Chat chat = chats.get(updateChat.chatId);
          synchronized (chat) {
            chat.unreadMentionCount = updateChat.unreadMentionCount;
          }
          break;
        }
        case UpdateMessageMentionRead.CONSTRUCTOR: {
          TdApi.UpdateMessageMentionRead updateChat = (TdApi.UpdateMessageMentionRead) object;
          TdApi.Chat chat = chats.get(updateChat.chatId);
          synchronized (chat) {
            chat.unreadMentionCount = updateChat.unreadMentionCount;
          }
          break;
        }
        case UpdateChatReplyMarkup.CONSTRUCTOR: {
          TdApi.UpdateChatReplyMarkup updateChat = (TdApi.UpdateChatReplyMarkup) object;
          TdApi.Chat chat = chats.get(updateChat.chatId);
          synchronized (chat) {
            chat.replyMarkupMessageId = updateChat.replyMarkupMessageId;
          }
          break;
        }
        case UpdateChatDraftMessage.CONSTRUCTOR: {
          TdApi.UpdateChatDraftMessage updateChat = (TdApi.UpdateChatDraftMessage) object;
          TdApi.Chat chat = chats.get(updateChat.chatId);
          synchronized (chat) {
            chat.draftMessage = updateChat.draftMessage;
            setChatPositions(chat, updateChat.positions);
          }
          break;
        }
        case UpdateChatPermissions.CONSTRUCTOR: {
          TdApi.UpdateChatPermissions update = (TdApi.UpdateChatPermissions) object;
          TdApi.Chat chat = chats.get(update.chatId);
          synchronized (chat) {
            chat.permissions = update.permissions;
          }
          break;
        }
        case UpdateChatNotificationSettings.CONSTRUCTOR: {
          TdApi.UpdateChatNotificationSettings update = (TdApi.UpdateChatNotificationSettings) object;
          TdApi.Chat chat = chats.get(update.chatId);
          synchronized (chat) {
            chat.notificationSettings = update.notificationSettings;
          }
          break;
        }
        case UpdateChatDefaultDisableNotification.CONSTRUCTOR: {
          TdApi.UpdateChatDefaultDisableNotification update = (TdApi.UpdateChatDefaultDisableNotification) object;
          TdApi.Chat chat = chats.get(update.chatId);
          synchronized (chat) {
            chat.defaultDisableNotification = update.defaultDisableNotification;
          }
          break;
        }
        case UpdateChatIsMarkedAsUnread.CONSTRUCTOR: {
          TdApi.UpdateChatIsMarkedAsUnread update = (TdApi.UpdateChatIsMarkedAsUnread) object;
          TdApi.Chat chat = chats.get(update.chatId);
          synchronized (chat) {
            chat.isMarkedAsUnread = update.isMarkedAsUnread;
          }
          break;
        }
        case UpdateChatIsBlocked.CONSTRUCTOR: {
          TdApi.UpdateChatIsBlocked update = (TdApi.UpdateChatIsBlocked) object;
          TdApi.Chat chat = chats.get(update.chatId);
          synchronized (chat) {
            chat.isBlocked = update.isBlocked;
          }
          break;
        }
        case UpdateChatHasScheduledMessages.CONSTRUCTOR: {
          TdApi.UpdateChatHasScheduledMessages update = (TdApi.UpdateChatHasScheduledMessages) object;
          TdApi.Chat chat = chats.get(update.chatId);
          synchronized (chat) {
            chat.hasScheduledMessages = update.hasScheduledMessages;
          }
          break;
        }

        case UpdateUserFullInfo.CONSTRUCTOR:
          TdApi.UpdateUserFullInfo updateUserFullInfo = (TdApi.UpdateUserFullInfo) object;
          break;
        case UpdateBasicGroupFullInfo.CONSTRUCTOR:
          TdApi.UpdateBasicGroupFullInfo updateBasicGroupFullInfo = (TdApi.UpdateBasicGroupFullInfo) object;
          break;
        case UpdateSupergroupFullInfo.CONSTRUCTOR:
          TdApi.UpdateSupergroupFullInfo updateSupergroupFullInfo = (TdApi.UpdateSupergroupFullInfo) object;
          break;
        default:
          log.info("Unsupported update: " + object.getClass().getSimpleName());
      }
    }
  }
}
