package reactivetwitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
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
    when(twitterClientMock.stream()).thenReturn(Observable
      .just(format("{\"id\":%d,\"text\":\"Example\",\"favorited\":true}\r\n", 1L))
      .map(tweet -> tweet.getBytes(StandardCharsets.UTF_8))
    );

    Observable<Tweet> tweets = twitterStreamer.tweets();

    TestSubscriber<Tweet> subscriber = new TestSubscriber<>();
    tweets.subscribe(subscriber);
    subscriber.assertCompleted();
    subscriber.assertValues(new Tweet("1", "Example"));
  }

  @Test
  public void shouldStreamChunkedTweets() {
    when(twitterClientMock.stream()).thenReturn(Observable
      .from(createTweetsAsChunks("Example A", "Example B", "Example C"))
      .map(chunk -> chunk.getBytes(StandardCharsets.UTF_8))
    );

    Observable<Tweet> tweets = twitterStreamer.tweets();

    TestSubscriber<Tweet> subscriber = new TestSubscriber<>();
    tweets.subscribe(subscriber);
    subscriber.assertCompleted();
    subscriber.assertValues(
      new Tweet("1", "Example A"),
      new Tweet("2", "Example B"),
      new Tweet("3", "Example C")
    );
  }

  @Test
  public void shouldPropagateError() {
    when(twitterClientMock.stream()).thenReturn(Observable
      .just(format("{\"id\":%d,\"text\":Example,\"favorited\":true}\r\n", 1L))
      .map(tweet -> tweet.getBytes(StandardCharsets.UTF_8))
    );

    Observable<Tweet> tweets = twitterStreamer.tweets();

    TestSubscriber<Tweet> subscriber = new TestSubscriber<>();
    tweets.subscribe(subscriber);
    subscriber.assertError(IOException.class);
  }

  @Test
  public void shouldShareStreamBetweenSubscribers() {
    TickMapper tickMapper = spy(new TickMapper());
    when(twitterClientMock.stream()).thenReturn(Observable
      .interval(10, TimeUnit.MILLISECONDS, Schedulers.newThread())
      .map(tickMapper::newTweet)
      .take(3)
    );

    Observable<Tweet> tweets = twitterStreamer.tweets();
    TestSubscriber<Tweet> firstSubscriber = new TestSubscriber<>();
    tweets.subscribe(firstSubscriber);
    TestSubscriber<Tweet> secondSubscriber = new TestSubscriber<>();
    tweets.subscribe(secondSubscriber);

    firstSubscriber.awaitTerminalEvent();
    secondSubscriber.awaitTerminalEvent();
    verify(tickMapper, times(3)).newTweet(anyLong());
  }

  private String[] createTweetsAsChunks(String... texts) {
    AtomicLong counter = new AtomicLong(1);

    return stream(texts)
      .map(text -> format("{\"id\":%d,#\"text\":\"%s\",#\"favorited\":true}", counter.getAndIncrement(), text))
      .collect(joining("\r\n"))
      .split("#");
  }

  static class TickMapper {

    byte[] newTweet(long tick) {
      return format("{\"id\":%d,\"text\":\"Example\",\"favorited\":true}\r\n", 1L).getBytes(StandardCharsets.UTF_8);
    }

  }

}
