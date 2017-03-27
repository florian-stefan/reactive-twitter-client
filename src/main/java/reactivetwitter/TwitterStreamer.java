package reactivetwitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TwitterStreamer {

  private static final String SEPARATOR = "\r\n";

  private final TwitterClient twitterClient;
  private final ObjectMapper objectMapper;

  public TwitterStreamer(TwitterClient twitterClient,
                         ObjectMapper objectMapper) {
    this.twitterClient = twitterClient;
    this.objectMapper = objectMapper;
  }

  public Observable<Tweet> tweets() {
    return twitterClient
      .stream()
      .map(this::decodeBytes)
      .flatMap(this::splitChunks)
      .publish(this::collectFrames)
      .filter(StringUtils::isNotEmpty)
      .flatMap(this::parseTweet)
      .share();
  }

  private String decodeBytes(byte[] bytes) {
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private Observable<String> splitChunks(String chunksAsString) {
    String[] chunks = StringUtils.split(chunksAsString, SEPARATOR);
    boolean endsWithSeparator = chunksAsString.endsWith(SEPARATOR);

    return Observable
      .from(chunks)
      .zipWith(Observable.range(1, chunks.length), (chunk, index) -> {
        if (index < chunks.length || endsWithSeparator) {
          return Observable.just(chunk, SEPARATOR);
        } else {
          return Observable.just(chunk);
        }
      })
      .flatMap(chunk -> chunk);
  }

  private Observable<String> collectFrames(Observable<String> chunks) {
    return chunks
      .filter(chunk -> !SEPARATOR.equals(chunk))
      .buffer(() -> chunks.filter(SEPARATOR::equals))
      .map(bufferedChunks -> StringUtils.join(bufferedChunks, ""));
  }

  private Observable<Tweet> parseTweet(String tweetAsString) {
    try {
      return Observable.just(objectMapper.readValue(tweetAsString, Tweet.class));
    } catch (IOException e) {
      return Observable.error(e);
    }
  }

}
