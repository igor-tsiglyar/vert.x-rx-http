package io.tsiglyar.github.repository.suggester;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.tsiglyar.github.Repository;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.validation.HTTPRequestValidationHandler;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.vertx.ext.web.api.validation.ParameterType.BOOL;
import static io.vertx.ext.web.api.validation.ParameterType.GENERIC_STRING;
import static java.lang.Boolean.parseBoolean;
import static java.util.stream.Collectors.toList;

public class GithubRepositorySuggesterVerticle extends AbstractVerticle {

  private GithubGraphQLApiAdapter adapter;
  private RepositoryPersister persister;
  private CircuitBreaker breaker;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    this.adapter = new GithubGraphQLApiAdapter(this.vertx);
    this.persister = new MongoDbRepositoryPersister(this.vertx);
    this.breaker = CircuitBreaker.create(this.getClass().getSimpleName() + "circuit-breaker", vertx,
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
        .addQueryParam("latest", BOOL, false))
      .handler(context -> load(context)
        .compose(repositories -> cache(context, repositories)
          .map(repositories)
          .map(toJsonArray()))
        .setHandler(result -> {
          if (result.failed()) {
            respond(result.cause()).accept(context.request());
          } else {
            respond(HttpResponseStatus.OK, result.result().encodePrettily()).accept(context.request());
          }
        })
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

  private Future<List<Repository>> load(RoutingContext context) {
    MultiMap params = context.queryParams();

    return Future.<List<Repository>>future(future -> persister.load(params.get("language"), future))
      .compose(repositories -> parseBoolean(params.get("latest"))
        ? Future.future(future -> breaker.executeCommandWithFallback(githubResultFuture ->
          adapter.getRepositoriesToContribute(params.get("language"), githubResultFuture),
          anyError -> repositories, future))
        : Future.succeededFuture(repositories));
  }

  private Future<Void> cache(RoutingContext context, List<Repository> repositories) {
    return Future.future(future -> persister.save(context.queryParams().get("language"),
      repositories, future));
  }

  private static Function<List<Repository>, JsonArray> toJsonArray() {
    return list -> new JsonArray(list.stream()
      .map(Repositories::toJson)
      .collect(toList()));
  }
}
