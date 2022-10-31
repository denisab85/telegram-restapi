package ca.denisab85.telegram.restapi.server.domain.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.TdApi.AuthorizationStateClosed;
import org.drinkless.tdlib.TdApi.AuthorizationStateClosing;
import org.drinkless.tdlib.TdApi.AuthorizationStateLoggingOut;
import org.drinkless.tdlib.TdApi.AuthorizationStateReady;
import org.drinkless.tdlib.TdApi.AuthorizationStateWaitCode;
import org.drinkless.tdlib.TdApi.AuthorizationStateWaitEmailAddress;
import org.drinkless.tdlib.TdApi.AuthorizationStateWaitEmailCode;
import org.drinkless.tdlib.TdApi.AuthorizationStateWaitOtherDeviceConfirmation;
import org.drinkless.tdlib.TdApi.AuthorizationStateWaitPassword;
import org.drinkless.tdlib.TdApi.AuthorizationStateWaitPhoneNumber;
import org.drinkless.tdlib.TdApi.AuthorizationStateWaitRegistration;
import org.drinkless.tdlib.TdApi.AuthorizationStateWaitTdlibParameters;
import org.drinkless.tdlib.TdApi.SetAuthenticationPhoneNumber;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthorizationService implements Client.ResultHandler {

  public final Lock authorizationLock = new ReentrantLock();
  public final Condition gotAuthorization = authorizationLock.newCondition();
  private final TelegramClientService telegramClientService;
  public TdApi.AuthorizationState authorizationState = null;
  public boolean haveAuthorization = false;


  public AuthorizationService(TelegramClientService telegramClientService) {
    this.telegramClientService = telegramClientService;
  }

  @Override
  public void onResult(TdApi.Object object) {
    switch (object.getConstructor()) {
      case TdApi.Error.CONSTRUCTOR:
        log.error("Receive an error:" + System.lineSeparator() + object);
        onAuthorizationStateUpdated(null); // repeat last action
        break;
      case TdApi.Ok.CONSTRUCTOR:
        // result is already received through UpdateAuthorizationState, nothing to do
        break;
      default:
        log.error("Receive wrong response from TDLib:" + System.lineSeparator() + object);
    }
  }

  public boolean isAuthenticated() {
    return haveAuthorization;
  }

  public void onAuthorizationStateUpdated(TdApi.AuthorizationState authorizationState) {
    if (authorizationState != null) {
      this.authorizationState = authorizationState;
    }
    switch (this.authorizationState.getConstructor()) {
      case AuthorizationStateWaitTdlibParameters.CONSTRUCTOR:
        TdApi.SetTdlibParameters request = new TdApi.SetTdlibParameters();
        request.databaseDirectory = "tdlib";
        request.useMessageDatabase = true;
        request.useSecretChats = true;
        request.apiId = 94575;
        request.apiHash = "a3406de8d171bb422bb6ddf3bbd800e2";
        request.systemLanguageCode = "en";
        request.deviceModel = "Desktop";
        request.applicationVersion = "1.0";
        request.enableStorageOptimizer = true;

        telegramClientService.getClient().send(request, this);
        break;
      case AuthorizationStateWaitPhoneNumber.CONSTRUCTOR: {
        String phoneNumber = promptString("Please enter phone number: ");
        telegramClientService.getClient()
            .send(new SetAuthenticationPhoneNumber(phoneNumber, null), this);
        break;
      }
      case AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR: {
        String link = ((AuthorizationStateWaitOtherDeviceConfirmation) this.authorizationState).link;
        log.info("Please confirm this login link on another device: " + link);
        break;
      }
      case AuthorizationStateWaitEmailAddress.CONSTRUCTOR: {
        String emailAddress = promptString("Please enter email address: ");
        telegramClientService.getClient()
            .send(new TdApi.SetAuthenticationEmailAddress(emailAddress), this);
        break;
      }
      case AuthorizationStateWaitEmailCode.CONSTRUCTOR: {
        String code = promptString("Please enter email authentication code: ");
        telegramClientService.getClient().send(
            new TdApi.CheckAuthenticationEmailCode(new TdApi.EmailAddressAuthenticationCode(code)),
            this);
        break;
      }
      case AuthorizationStateWaitCode.CONSTRUCTOR: {
        String code = promptString("Please enter authentication code: ");
        telegramClientService.getClient().send(new TdApi.CheckAuthenticationCode(code), this);
        break;
      }
      case AuthorizationStateWaitRegistration.CONSTRUCTOR: {
        String firstName = promptString("Please enter your first name: ");
        String lastName = promptString("Please enter your last name: ");
        telegramClientService.getClient().send(new TdApi.RegisterUser(firstName, lastName), this);
        break;
      }
      case AuthorizationStateWaitPassword.CONSTRUCTOR: {
        String password = promptString("Please enter password: ");
        telegramClientService.getClient()
            .send(new TdApi.CheckAuthenticationPassword(password), this);
        break;
      }
      case AuthorizationStateReady.CONSTRUCTOR:
        haveAuthorization = true;
        authorizationLock.lock();
        try {
          gotAuthorization.signal();
        } finally {
          authorizationLock.unlock();
        }
        break;
      case AuthorizationStateLoggingOut.CONSTRUCTOR:
        haveAuthorization = false;
        log.info("Logging out");
        break;
      case AuthorizationStateClosing.CONSTRUCTOR:
        haveAuthorization = false;
        log.info("Closing");
        break;
      case AuthorizationStateClosed.CONSTRUCTOR:
        log.info("Closed");
        break;
      default:
        log.error(
            "Unsupported authorization state:" + System.lineSeparator() + this.authorizationState);
    }
  }

  private String promptString(String prompt) {
    log.info(prompt);
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    String str = "";
    try {
      str = reader.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return str;
  }

}
