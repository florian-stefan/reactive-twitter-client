package reactivetwitter;

import org.asynchttpclient.*;
import org.asynchttpclient.Response.ResponseBuilder;
import org.asynchttpclient.oauth.ConsumerKey;
import org.asynchttpclient.oauth.OAuthSignatureCalculator;
import org.asynchttpclient.oauth.RequestToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Emitter.BackpressureMode;
import rx.Observable;
import rx.subscriptions.Subscriptions;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class TwitterClient {

  private static Logger LOGGER = LoggerFactory.getLogger(TwitterClient.class);

  private final String apiKey;
  private final String apiSecret;
  private final String token;
  private final String tokenSecret;
  private final List<String> track;

  private TwitterClient(String apiKey,
                        String apiSecret,
                        String token,
                        String tokenSecret,
                        List<String> track) {
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
    this.token = token;
    this.tokenSecret = tokenSecret;
    this.track = track;
  }

  public Observable<byte[]> stream() {
    String trackAsString = track.stream().collect(joining(","));
    String url = format("https://stream.twitter.com/1.1/statuses/filter.json?track=%s", trackAsString);

    ConsumerKey consumerKey = new ConsumerKey(apiKey, apiSecret);
    RequestToken requestToken = new RequestToken(token, tokenSecret);

    Request request = new RequestBuilder()
      .setMethod("POST")
      .setUrl(url)
      .setSignatureCalculator(new OAuthSignatureCalculator(consumerKey, requestToken))
      .setRequestTimeout(Integer.MAX_VALUE)
      .build();

    return Observable.create(emitter -> {
      DefaultAsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();

      LOGGER.info("Starting to request tweets ...");
      asyncHttpClient.executeRequest(request, new AsyncHandler<Response>() {
        private final ResponseBuilder builder = new ResponseBuilder();

        @Override
        public void onThrowable(Throwable throwable) {
          emitter.onError(throwable);
        }

        @Override
        public State onBodyPartReceived(HttpResponseBodyPart httpResponseBodyPart) throws Exception {
          emitter.onNext(httpResponseBodyPart.getBodyPartBytes());

          return httpResponseBodyPart.isLast() ? State.ABORT : State.CONTINUE;
        }

        @Override
        public State onStatusReceived(HttpResponseStatus httpResponseStatus) throws Exception {
          LOGGER.info("Received HTTP response status: {}", httpResponseStatus.getStatusCode());
          builder.accumulate(httpResponseStatus);

          if (httpResponseStatus.getStatusCode() == 200) {
            return State.CONTINUE;
          } else {
            emitter.onError(new TwitterStatusCodeException(httpResponseStatus.getStatusCode()));

            return State.ABORT;
          }
        }

        @Override
        public State onHeadersReceived(HttpResponseHeaders httpResponseHeaders) throws Exception {
          LOGGER.info("Received HTTP response headers: {}", httpResponseHeaders.getHeaders());
          builder.accumulate(httpResponseHeaders);

          return State.CONTINUE;
        }

        @Override
        public Response onCompleted() throws Exception {
          emitter.onCompleted();

          return builder.build();
        }
      });

      emitter.setSubscription(Subscriptions.create(asyncHttpClient::close));
    }, BackpressureMode.BUFFER);
  }

  public static class Builder {

    private String apiKey;
    private String apiSecret;
    private String token;
    private String tokenSecret;
    private List<String> track;

    public Builder() {
      track = new ArrayList<>();
    }

    public Builder withApiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public Builder withApiSecret(String apiSecret) {
      this.apiSecret = apiSecret;
      return this;
    }

    public Builder withToken(String token) {
      this.token = token;
      return this;
    }

    public Builder withTokenSecret(String tokenSecret) {
      this.tokenSecret = tokenSecret;
      return this;
    }

    public Builder withTrackPhrase(String trackPhrase) {
      track.add(trackPhrase);
      return this;
    }

    public TwitterClient build() {
      return new TwitterClient(
        apiKey,
        apiSecret,
        token,
        tokenSecret,
        track
      );
    }

  }

}
