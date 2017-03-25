#Usage

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
