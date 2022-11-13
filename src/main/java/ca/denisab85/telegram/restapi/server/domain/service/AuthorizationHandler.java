package ca.denisab85.telegram.restapi.server.domain.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Supplier;
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
import org.drinkless.tdlib.TdApi.UpdateAuthorizationState;

@Slf4j
public class AuthorizationHandler implements Client.ResultHandler {


  private final AuthorizationManager authorizationManager;
  private final Supplier<Client> clientSupplier;

  public AuthorizationHandler(AuthorizationManager authorizationManager,
      Supplier<Client> clientSupplier) {
    this.clientSupplier = clientSupplier;
    this.authorizationManager = authorizationManager;
  }

  @Override
  public void onResult(TdApi.Object object) {
    switch (object.getConstructor()) {
      case UpdateAuthorizationState.CONSTRUCTOR:
        updateAuthorizationState((TdApi.UpdateAuthorizationState) object);
        break;
      case TdApi.Error.CONSTRUCTOR:
        log.error("Received an error:" + System.lineSeparator() + object);
        updateAuthorizationState(null); // repeat last action
        break;
      case TdApi.Ok.CONSTRUCTOR:
        // result is already received through UpdateAuthorizationState, nothing to do
        break;
    }
  }

  private void updateAuthorizationState(UpdateAuthorizationState updateAuthorizationState) {
    TdApi.AuthorizationState authorizationState = updateAuthorizationState.authorizationState;
    if (authorizationState != null) {
      authorizationManager.setAuthorizationState(authorizationState);
    }
    switch (authorizationManager.getAuthorizationState().getConstructor()) {
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

        clientSupplier.get().send(request, this);
        break;
      case AuthorizationStateWaitPhoneNumber.CONSTRUCTOR: {
        String phoneNumber = promptString("Please enter phone number: ");
        clientSupplier.get().send(new SetAuthenticationPhoneNumber(phoneNumber, null), this);
        break;
      }
      case AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR: {
        String link = ((AuthorizationStateWaitOtherDeviceConfirmation) authorizationManager.getAuthorizationState()).link;
        log.info("Please confirm this login link on another device: " + link);
        break;
      }
      case AuthorizationStateWaitEmailAddress.CONSTRUCTOR: {
        String emailAddress = promptString("Please enter email address: ");
        clientSupplier.get().send(new TdApi.SetAuthenticationEmailAddress(emailAddress), this);
        break;
      }
      case AuthorizationStateWaitEmailCode.CONSTRUCTOR: {
        String code = promptString("Please enter email authentication code: ");
        clientSupplier.get().send(
            new TdApi.CheckAuthenticationEmailCode(new TdApi.EmailAddressAuthenticationCode(code)),
            this);
        break;
      }
      case AuthorizationStateWaitCode.CONSTRUCTOR: {
        String code = promptString("Please enter authentication code: ");
        clientSupplier.get().send(new TdApi.CheckAuthenticationCode(code), this);
        break;
      }
      case AuthorizationStateWaitRegistration.CONSTRUCTOR: {
        String firstName = promptString("Please enter your first name: ");
        String lastName = promptString("Please enter your last name: ");
        clientSupplier.get().send(new TdApi.RegisterUser(firstName, lastName), this);
        break;
      }
      case AuthorizationStateWaitPassword.CONSTRUCTOR: {
        String password = promptString("Please enter password: ");
        clientSupplier.get().send(new TdApi.CheckAuthenticationPassword(password), this);
        break;
      }
      case AuthorizationStateReady.CONSTRUCTOR:
        authorizationManager.setHaveAuthorization(true);
        break;
      case AuthorizationStateLoggingOut.CONSTRUCTOR:
        authorizationManager.setHaveAuthorization(false);
        log.info("Logging out");
        break;
      case AuthorizationStateClosing.CONSTRUCTOR:
        authorizationManager.setHaveAuthorization(false);
        log.info("Closing");
        break;
      case AuthorizationStateClosed.CONSTRUCTOR:
        log.info("Closed");
        break;
      default:
        log.error("Unsupported authorization state:" + System.lineSeparator()
            + authorizationManager.getAuthorizationState());
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
