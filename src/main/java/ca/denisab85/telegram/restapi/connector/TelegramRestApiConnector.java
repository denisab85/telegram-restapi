package ca.denisab85.telegram.restapi.connector;

import ca.denisab85.telegram.restapi.server.domain.model.rest.GetStateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.drinkless.tdlib.TdApi.Message;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TelegramRestApiConnector {

  /**
   * Retrieve the running/authorized state of the service.
   *
   * @return GetStateResponse
   */
  @GET("/auth/state")
  Call<GetStateResponse> getState();

  /**
   * Start the service and initialize authentication (if necessary).
   *
   * @return GetStateResponse
   */
  @POST("/auth/start")
  Call<GetStateResponse> start();

  /**
   * Retrieve SMS code for authentication.
   *
   * @return GetStateResponse
   */
  @GET("/auth/getSmsCode")
  Call<String> getSmsCode();

  /**
   * Retrieve the IDs of all chats from the main chat list.
   *
   * @return List<Long>
   */
  @GET("/chats")
  Call<List<Long>> getChatIds(@Query("limit") int limit);

  /**
   * Retrieve messages for a specific chat ID.
   *
   * @return List<Long>
   */
  @GET("/chat/{chatId}/history")
  Call<List<Message>> getChatHistory(@Path("testCaseKey") long chatId,
      @Query("fromMessageId") long fromMessageId, @Query("offset") int offset,
      @Query("limit") int limit);

  class Builder {

    public static TelegramRestApiConnector build(String serverAddress) {
      OkHttpClient.Builder clientBuilder = new OkHttpClient().newBuilder();
      try {
        ResourceBundle props = ResourceBundle.getBundle("application");
        if (props.containsKey("network.connectTimeout")) {
          clientBuilder.connectTimeout(Long.parseLong(props.getString("network.connectTimeout")),
              TimeUnit.SECONDS);
        }
        if (props.containsKey("network.readTimeout")) {
          clientBuilder.readTimeout(Long.parseLong(props.getString("network.readTimeout")),
              TimeUnit.SECONDS);
        }
        if (props.containsKey("network.writeTimeout")) {
          clientBuilder.writeTimeout(Long.parseLong(props.getString("network.writeTimeout")),
              TimeUnit.SECONDS);
        }
      } catch (Exception ignored) {
      }
      final ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      final Retrofit retrofit = new Retrofit.Builder().client(clientBuilder.build())
          .baseUrl(serverAddress).addConverterFactory(JacksonConverterFactory.create(objectMapper))
          .build();
      return retrofit.create(TelegramRestApiConnector.class);
    }
  }

}
