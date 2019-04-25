package io.vertx.starter;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

public class MainVerticle extends AbstractVerticle {

  private GithubAdapter adapter;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    this.adapter = new GithubAdapter(this.vertx);
  }

  @Override
  public void start() {
    vertx.createHttpServer()
        .requestHandler(req -> adapter.getRepositoriesToContribute(req.getParam("language"))
          .map(Repository::toJson)
          .to(toJsonArray())
          .doOnError(Throwable::printStackTrace)
          .subscribe(
            repos -> req.response()
              .putHeader("Content-Type", "application/json")
              .end(repos.encodePrettily()),
            error -> req.response()
              .setStatusCode(INTERNAL_SERVER_ERROR.code())
              .putHeader("Content-Type", "application/json")
              .end(new JsonObject()
                .put("unexpected_error", error.getMessage())
                .encodePrettily())
          ))
        .listen(8080);
  }

  private static <T> Function<Flowable<T>, Single<JsonArray>> toJsonArray() {
    return flow -> flow.collect(JsonArray::new, JsonArray::add);
  }
}
