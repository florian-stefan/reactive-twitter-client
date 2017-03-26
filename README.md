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

Observable<Tweet> tweets = new TwitterStreamer(twitterClient, new ObjectMapper()).tweets();
```
# Observer

By default, the observer is called in a thread provided by the NioEventLoopGroup that is also used for reacting on incoming data. Therefore, this thread should not be blocked. If the observer needs to perform a blocking operation, you should use another thread pool for this.
