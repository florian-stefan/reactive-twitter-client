package reactivetwitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class TwitterStreamer {

  private static final String SEPARATOR = "\r\n";

  private final TwitterClient twitterClient;
  private final ObjectMapper objectMapper;

  public TwitterStreamer(TwitterClient twitterClient,
                         ObjectMapper objectMapper) {
    this.twitterClient = twitterClient;
    this.objectMapper = objectMapper;
  }

  public Flux<Tweet> tweets() {
    return twitterClient
      .stream()
      .map(this::decodeBytes)
      .flatMap(this::splitChunks)
      .publish(this::collectFrames)
      .flatMap(this::parseTweet)
      .filter(Tweet::hasText)
      .share();
  }

  private String decodeBytes(byte[] bytes) {
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private Flux<String> splitChunks(String chunksAsString) {
    String[] chunks = StringUtils.split(chunksAsString, SEPARATOR);
    boolean endsWithSeparator = chunksAsString.endsWith(SEPARATOR);

    return Flux
      .fromArray(chunks)
      .zipWith(Flux.range(1, chunks.length), (chunk, index) -> {
        if (index < chunks.length || endsWithSeparator) {
          return Flux.just(chunk, SEPARATOR);
        } else {
          return Flux.just(chunk);
        }
      })
      .flatMap(chunk -> chunk);
  }

  private Flux<String> collectFrames(Flux<String> chunks) {
    return chunks
      .filter(chunk -> !SEPARATOR.equals(chunk))
      .buffer(chunks.filter(SEPARATOR::equals))
      .map(bufferedChunks -> StringUtils.join(bufferedChunks, EMPTY));
  }

  private Mono<Tweet> parseTweet(String tweetAsString) {
    try {
      return Mono.just(objectMapper.readValue(tweetAsString, Tweet.class));
    } catch (IOException e) {
      return Mono.error(e);
    }
  }

}
