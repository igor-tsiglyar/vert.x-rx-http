package io.tsiglyar.github.repository.suggester;

import io.tsiglyar.github.Repository;
import io.vertx.core.json.JsonObject;

public final class Repositories {

  private Repositories() {
    throw new UnsupportedOperationException();
  }

  public static Repository fromJson(JsonObject json) {
    return new Repository(json.getString("name"),
                          json.getString("description"),
                          json.getString("url"));
  }

  public static JsonObject toJson(Repository repository) {
    return new JsonObject()
      .put("name", repository.getName())
      .put("description", repository.getDescription())
      .put("url", repository.getUrl());
  }

}
