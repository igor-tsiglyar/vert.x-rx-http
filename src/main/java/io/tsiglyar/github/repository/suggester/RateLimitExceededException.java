package io.tsiglyar.github.repository.suggester;

public class RateLimitExceededException extends RuntimeException {

  public RateLimitExceededException() {
    super("API request rate limit exceeded");
  }

}
