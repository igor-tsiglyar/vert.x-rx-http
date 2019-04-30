package io.tsiglyar.github.repository.suggester;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.tsiglyar.github.adapter.GithubAdapter;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

public class GithubRepositorySuggesterVerticle extends AbstractVerticle {

  private GithubAdapter adapter;
  private RepositoryPersister persister;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    this.adapter = new VertxGithubAdapter(this.vertx);
    this.persister = new CassandraRepositoryPersister(this.vertx);
  }

  @Override
  public void start() {
    vertx.createHttpServer()
        .requestHandler(req -> Single.just(req.getParam("language"))
            .flatMapPublisher(language -> persister.load(language)
              .switchIfEmpty(Flowable.fromPublisher(adapter.getRepositoriesToContribute(language))
                .toList()
                .flatMapPublisher(repos -> persister.save(language, repos)
                  .andThen(Flowable.fromIterable(repos)))))
          .map(Repositories::toJson)
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
          )
        )
        .listen(8080);
  }

  private static <T> Function<Flowable<T>, Single<JsonArray>> toJsonArray() {
    return flow -> flow.collect(JsonArray::new, JsonArray::add);
  }
}
