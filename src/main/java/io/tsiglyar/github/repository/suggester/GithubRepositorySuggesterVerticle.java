package io.tsiglyar.github.repository.suggester;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Flowable;
import io.reactivex.FlowableConverter;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleConverter;
import io.reactivex.functions.Consumer;
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
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.validation.HTTPRequestValidationHandler;

import java.util.List;

import static io.vertx.ext.web.api.validation.ParameterType.BOOL;
import static io.vertx.ext.web.api.validation.ParameterType.GENERIC_STRING;
import static java.lang.Boolean.parseBoolean;

public class GithubRepositorySuggesterVerticle extends AbstractVerticle {

  private GithubAdapter adapter;
  private RepositoryPersister persister;
  private CircuitBreaker breaker;
  private Scheduler scheduler;

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
    this.scheduler = RxHelper.blockingScheduler(this.vertx);
  }

  @Override
  public void start() {
    Router router = Router.router(vertx);
    router.route("/projects_to_contribute")
      .handler(HTTPRequestValidationHandler.create()
        .addQueryParam("language", GENERIC_STRING, true)
        .addQueryParam("latest", BOOL, false))
      .handler(context -> load(context)
        .as(asPublisher(context))
        .as(asJsonArray())
        .doOnError(Throwable::printStackTrace)
        .subscribeOn(scheduler)
        .subscribe(
          repos -> respond(HttpResponseStatus.OK, repos.encodePrettily()).accept(context.request()),
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
      .flatMap(repositories -> parseBoolean(params.get("latest"))
        ? breaker.rxExecuteCommandWithFallback(future ->
        Flowable.fromPublisher(adapter.getRepositoriesToContribute(params.get("language")))
          .toList()
          .subscribeOn(scheduler)
          .subscribe(SingleHelper.toObserver(future.getDelegate())), anyError -> repositories)
        : Single.just(repositories));
  }

  private SingleConverter<List<Repository>, Flowable<Repository>> asPublisher(RoutingContext context) {
    MultiMap params = context.queryParams();

    return repositoryListSingle -> repositoryListSingle
      .flatMapPublisher(repos -> persister.save(params.get("language"), repos)
        .andThen(Flowable.fromIterable(repos)));
  }

  private static FlowableConverter<Repository, Single<JsonArray>> asJsonArray() {
    return flow -> flow.map(Repositories::toJson)
      .collect(JsonArray::new, JsonArray::add);
  }
}
