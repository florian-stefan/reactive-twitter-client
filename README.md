# Usage

```
TwitterClient twitterClient = new TwitterClient.Builder()
  .withApiKey("...")
  .withApiSecret("...")
  .withToken("...")
  .withTokenSecret("...")
  .withTrackPhrase("...")
  .withTrackPhrase("...")
  .build();

Flux<Tweet> tweets = new TwitterStreamer(twitterClient, new ObjectMapper()).tweets();
```
# Observer

* By default, the subscriber is called in a thread provided by the NioEventLoopGroup that is also used for reacting on incoming data. Therefore, this thread should not be blocked. If the subscriber needs to perform a blocking operation, you should use another thread pool for this.
* The stream obtained by `TwitterStreamer.tweets()` is shared between all subscribers, i.e. only the first subscriber will create a new connection to the Twitter API. The connection is closed after the last observer unsubscribes.
