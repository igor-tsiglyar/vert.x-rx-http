package io.tsiglyar.github.repository.suggester;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.tsiglyar.github.Repository;
import io.tsiglyar.github.adapter.GithubAdapter;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.circuitbreaker.CircuitBreaker;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.validation.HTTPRequestValidationHandler;

import java.util.List;

import static io.vertx.ext.web.api.validation.ParameterType.BOOL;
import static io.vertx.ext.web.api.validation.ParameterType.GENERIC_STRING;
import static java.lang.Boolean.parseBoolean;
import static java.util.stream.Collectors.toList;

public class GithubRepositorySuggesterVerticle extends AbstractVerticle {

  private GithubAdapter adapter;
  private RepositoryPersister persister;
  private CircuitBreaker breaker;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    this.adapter = new GithubGraphQLApiAdapter(this.vertx);
    this.persister = new MongoDbRepositoryPersister(this.vertx);
    this.breaker = CircuitBreaker.create(this.getClass().getSimpleName() + "circuit-breaker", this.vertx,
      new CircuitBreakerOptions()
        .setMaxFailures(1)
        .setFallbackOnFailure(true)
        .setResetTimeout(1000));
  }

  @Override
  public void start() {
    Router router = Router.router(vertx);
    router.route("/projects_to_contribute")
      .handler(HTTPRequestValidationHandler.create()
        .addQueryParam("language", GENERIC_STRING, true)
        .addQueryParam("fallback", BOOL, false))
      .handler(context -> load(context)
        .flatMap(repositories -> cache(context, repositories)
          .andThen(Single.just(repositories))
          .map(toJsonArray()))
        .subscribe(
          result -> respond(HttpResponseStatus.OK, result.encodePrettily()).accept(context.request()),
          error -> respond(error).accept(context.request())
        )
      );

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080);
  }

  private Consumer<HttpServerRequest> respond(HttpResponseStatus status, String content) {
    return request -> request.response()
      .setStatusCode(status.code())
      .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
      .end(content);
  }

  private Consumer<HttpServerRequest> respond(Throwable error) {
    return respond(HttpResponseStatus.INTERNAL_SERVER_ERROR, new JsonObject()
      .put("error", error.getMessage())
      .encodePrettily());
  }

  private Single<List<Repository>> load(RoutingContext context) {
    MultiMap params = context.queryParams();

    return persister.load(params.get("language"))
      .toList()
      .flatMap(repositories -> breaker.rxExecuteCommandWithFallback(fut ->
          Flowable.fromPublisher(adapter.getRepositoriesToContribute(params.get("language")))
            .toList()
            .subscribe(SingleHelper.toObserver(fut.completer())), error -> {
        if (!params.contains("fallback") || parseBoolean(params.get("fallback"))) {
          return repositories;
        }

        throw new RuntimeException(error);
      }));
  }

  private Completable cache(RoutingContext context, List<Repository> repositories) {
    return persister.save(context.queryParams().get("language"), repositories);
  }

  private static Function<List<Repository>, JsonArray> toJsonArray() {
    return list -> new JsonArray(list.stream()
      .map(Repositories::toJson)
      .collect(toList()));
  }
}
