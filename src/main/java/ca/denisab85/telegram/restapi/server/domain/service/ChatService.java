package ca.denisab85.telegram.restapi.server.domain.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.TdApi.ChatListMain;
import org.drinkless.tdlib.TdApi.Chats;
import org.drinkless.tdlib.TdApi.Error;
import org.drinkless.tdlib.TdApi.Message;
import org.drinkless.tdlib.TdApi.MessagePhoto;
import org.drinkless.tdlib.TdApi.Messages;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChatService {

  private final TelegramClientService telegramClientService;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public ChatService(TelegramClientService telegramClientService) {
    this.telegramClientService = telegramClientService;
  }

  public static void main(String[] args) throws Exception {
    TelegramClientService service = new TelegramClientService();
    service.start();
    ChatService chatService = new ChatService(service);
    chatService.saveImages();
  }

  public void saveImages() throws ExecutionException, InterruptedException {
    List<Message> messages;
    Message lastMessage;
    long lastMessageId = 1892679680;
    do {
      messages = getChatHistory(429638055, lastMessageId, 0, 100).get();
      if (!messages.isEmpty()) {
        for (Message message : messages) {
          if (message.content.getConstructor()
              == MessagePhoto.CONSTRUCTOR) {//              System.err.println("Photo");
          }
          System.err.printf("%d   %s   %s%n", message.id, new Date(message.date * 1000L),
              message.content.getClass().getSimpleName());
        }
        lastMessage = messages.stream().min(Comparator.comparing(m -> m.id)).get();
        lastMessageId = lastMessage.id;
      } else {
        System.err.println("DONE");
      }
    } while (!messages.isEmpty());
  }

  public Future<List<Long>> getChatIds(int limit) throws TelegramException {

//    synchronized (mainChatList) {
//      if (!haveFullMainChatList && limit > mainChatList.size()) {
//        // send LoadChats request if there are some unknown chats and have not enough known chats
//        client.send(new TdApi.LoadChats(new TdApi.ChatListMain(), limit - mainChatList.size()), new Client.ResultHandler() {
//          @Override
//          public void onResult(TdApi.Object object) {
//            switch (object.getConstructor()) {
//              case TdApi.Error.CONSTRUCTOR:
//                if (((TdApi.Error) object).code == 404) {
//                  synchronized (mainChatList) {
//                    haveFullMainChatList = true;
//                  }
//                } else {
//                  System.err.println("Receive an error for LoadChats:" + newLine + object);
//                }
//                break;
//              case TdApi.Ok.CONSTRUCTOR:
//                // chats had already been received through updates, let's retry request
//                getMainChatList(limit);
//                break;
//              default:
//                System.err.println("Receive wrong response from TDLib:" + newLine + object);
//            }
//          }
//        });
//        return;
//      }
//
//      java.util.Iterator<OrderedChat> iter = mainChatList.iterator();
//      System.out.println();
//      System.out.println("First " + limit + " chat(s) out of " + mainChatList.size() + " known chat(s):");
//      for (int i = 0; i < limit && i < mainChatList.size(); i++) {
//        long chatId = iter.next().chatId;
//        TdApi.Chat chat = chats.get(chatId);
//        synchronized (chat) {
//          System.out.println(chatId + ": " + chat.title);
//        }
//      }
//      print("");
//    }

    final CountDownLatch doneSignal = new CountDownLatch(1);
    final List<Long> chats = new ArrayList<>();
    final AtomicReference<Error> exception = new AtomicReference<>();
    return executor.submit(() -> {
      ChatListMain chatList = new ChatListMain();
      telegramClientService.getClient().send(new TdApi.GetChats(chatList, limit), object -> {
        switch (object.getConstructor()) {
          case Chats.CONSTRUCTOR:
            Arrays.stream(((Chats) object).chatIds).forEach(chats::add);
            break;
          case TdApi.Error.CONSTRUCTOR:
            exception.set(((Error) object));
        }
        doneSignal.countDown();
      });
      doneSignal.await();
      if (exception.get() != null) {
        throw new TelegramException(exception.get());
      }
      return chats;
    });
  }

  public Future<List<Message>> getChatHistory(long chatId, long fromMessageId, int offset,
      int limit) {
    final CountDownLatch doneSignal = new CountDownLatch(1);
    final List<Message> messages = new ArrayList<>();
    final AtomicReference<Error> exception = new AtomicReference<>();
    return executor.submit(() -> {
      telegramClientService.getClient()
          .send(new TdApi.GetChatHistory(chatId, fromMessageId, offset, limit, false), object -> {
            switch (object.getConstructor()) {
              case Messages.CONSTRUCTOR:
                messages.addAll(
                    Arrays.stream(((Messages) object).messages).collect(Collectors.toList()));
                break;
              case TdApi.Error.CONSTRUCTOR:
                exception.set(((Error) object));
            }
          });
      doneSignal.await();
      if (exception.get() != null) {
        throw new TelegramException(exception.get());
      }
      return messages;
    });
  }

}
