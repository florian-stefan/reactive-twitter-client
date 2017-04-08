package reactivetwitter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Tweet {

  public final String id;
  public final String text;

  @JsonCreator
  public Tweet(@JsonProperty("id_str") String id,
               @JsonProperty("text") String text) {
    this.id = id;
    this.text = text;
  }

  public boolean hasText() {
    return StringUtils.isNotBlank(text);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append(id)
      .append(text)
      .build();
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }

    if (that == null || getClass() != that.getClass()) {
      return false;
    }

    Tweet tweet = (Tweet) that;

    return new EqualsBuilder()
      .append(id, tweet.id)
      .append(text, tweet.text)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(id)
      .append(text)
      .toHashCode();
  }

}
