package io.tsiglyar.github.repository.suggester;

import io.tsiglyar.github.Repository;
import io.vertx.core.json.JsonObject;

public final class Repositories {

  private Repositories() {
    throw new UnsupportedOperationException();
  }

  public static Repository fromJson(JsonObject json) {
    return json.mapTo(Repository.class);
  }

  public static JsonObject toJson(Repository repository) {
    return JsonObject.mapFrom(repository);
  }

}
