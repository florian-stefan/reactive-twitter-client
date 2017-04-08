package reactivetwitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mockito.Mockito.*;

public class TwitterStreamerTest {

  private TwitterClient twitterClientMock;

  private TwitterStreamer twitterStreamer;

  @Before
  public void setUp() {
    twitterClientMock = mock(TwitterClient.class);

    twitterStreamer = new TwitterStreamer(twitterClientMock, new ObjectMapper());
  }

  @Test
  public void shouldStreamSingleTweet() {
    Flux<byte[]> rawTweets = createRawTweets(format("{\"id\":%d,\"text\":\"Example\",\"favorited\":true}\r\n", 1L));
    when(twitterClientMock.stream()).thenReturn(rawTweets);

    Flux<Tweet> tweets = twitterStreamer.tweets();

    StepVerifier.create(tweets)
      .expectNext(new Tweet("1", "Example"))
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldStreamChunkedTweets() {
    Flux<byte[]> rawTweetsAsChunks = createRawTweetsAsChunks("Example A", "Example B", "Example C");
    when(twitterClientMock.stream()).thenReturn(rawTweetsAsChunks);

    Flux<Tweet> tweets = twitterStreamer.tweets();

    StepVerifier.create(tweets)
      .expectNext(new Tweet("1", "Example A"))
      .expectNext(new Tweet("2", "Example B"))
      .expectNext(new Tweet("3", "Example C"))
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldHandleEmptyResponses() {
    Flux<byte[]> rawTweets = createRawTweets(format("{\"id\":%d,\"text\":\"Example\"}\r\n", 1L), EMPTY);
    when(twitterClientMock.stream()).thenReturn(rawTweets);

    Flux<Tweet> tweets = twitterStreamer.tweets();

    StepVerifier.create(tweets)
      .expectNext(new Tweet("1", "Example"))
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldFilterMessagesWithoutText() {
    Flux<byte[]> rawTweets = createRawTweets(
      format("{\"id\":%d,\"text\":\"Example\"}\r\n", 1L),
      format("{\"id\":%d,\"test\":\"Example\"}\r\n", 2L)
    );
    when(twitterClientMock.stream()).thenReturn(rawTweets);

    Flux<Tweet> tweets = twitterStreamer.tweets();

    StepVerifier.create(tweets)
      .expectNext(new Tweet("1", "Example"))
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldPropagateError() {
    Flux<byte[]> rawTweets = createRawTweets(format("{\"id\":%d,\"text\":Example,\"favorited\":true}\r\n", 1L));
    when(twitterClientMock.stream()).thenReturn(rawTweets);

    Flux<Tweet> tweets = twitterStreamer.tweets();

    StepVerifier.create(tweets)
      .expectError(IOException.class)
      .verify();
  }

  @Test
  public void shouldShareStreamBetweenSubscribers() throws InterruptedException {
    CountDownLatch countDownLatch = new CountDownLatch(6);
    TickMapper tickMapper = spy(new TickMapper());
    Flux<byte[]> rawTweets = Flux
      .interval(Duration.ofMillis(10))
      .map(tickMapper::newTweet)
      .take(3);
    when(twitterClientMock.stream()).thenReturn(rawTweets);

    Flux<Tweet> tweets = twitterStreamer.tweets();
    tweets.subscribe(tweet -> countDownLatch.countDown());
    tweets.subscribe(tweet -> countDownLatch.countDown());
    countDownLatch.await();

    verify(tickMapper, times(3)).newTweet(anyLong());
  }

  private Flux<byte[]> createRawTweets(String... texts) {
    return Flux
      .fromArray(texts)
      .map(tweet -> tweet.getBytes(StandardCharsets.UTF_8));
  }

  private Flux<byte[]> createRawTweetsAsChunks(String... texts) {
    AtomicLong counter = new AtomicLong(1);

    String[] textsAsChunks = stream(texts)
      .map(text -> format("{\"id\":%d,#\"text\":\"%s\",#\"favorited\":true}", counter.getAndIncrement(), text))
      .collect(joining("\r\n"))
      .split("#");

    return createRawTweets(textsAsChunks);
  }

  static class TickMapper {

    byte[] newTweet(long tick) {
      return format("{\"id\":%d,\"text\":\"Example\",\"favorited\":true}\r\n", 1L).getBytes(StandardCharsets.UTF_8);
    }

  }

}
