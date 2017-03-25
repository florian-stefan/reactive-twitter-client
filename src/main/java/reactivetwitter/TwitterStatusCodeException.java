package reactivetwitter;

import static java.lang.String.format;

public class TwitterStatusCodeException extends RuntimeException {

  public final int statusCode;

  public TwitterStatusCodeException(int statusCode) {
    super(format("Received HTTP response status: %s", statusCode));
    this.statusCode = statusCode;
  }

}
