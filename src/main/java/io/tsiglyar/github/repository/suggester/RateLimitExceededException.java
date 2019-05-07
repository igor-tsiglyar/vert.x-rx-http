package io.tsiglyar.github.repository.suggester;

public class RateLimitExceededException extends RuntimeException {

  public final Cause cause;

  public RateLimitExceededException() {
    this(Cause.TOO_MUCH);
  }

  public RateLimitExceededException(Cause cause) {
    super("API request rate limit exceeded");
    this.cause = cause;
  }

  public enum Cause {
    TOO_MUCH,
    TOO_FAST
  }

}
