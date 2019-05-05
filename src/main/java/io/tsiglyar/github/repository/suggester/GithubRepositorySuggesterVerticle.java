package io.tsiglyar.github.repository.suggester;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableConverter;
import io.reactivex.FlowableTransformer;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.tsiglyar.github.Repository;
import io.tsiglyar.github.adapter.GithubAdapter;
import io.vertx.core.Context;
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

import static io.vertx.ext.web.api.validation.ParameterType.BOOL;
import static io.vertx.ext.web.api.validation.ParameterType.GENERIC_STRING;

public class GithubRepositorySuggesterVerticle extends AbstractVerticle {

  private GithubAdapter adapter;
  private RepositoryPersister persister;

  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    this.adapter = new GithubGraphQLApiAdapter(this.vertx);
    this.persister = new MongoDbRepositoryPersister(this.vertx);
  }

  @Override
  public Completable rxStart() {
    Router router = Router.router(vertx);
    router.route("/projects_to_contribute")
      .handler(HTTPRequestValidationHandler.create()
        .addQueryParam("language", GENERIC_STRING, true)
        .addQueryParam("cache", BOOL, false))
      .handler(context -> Single.just(context.request().getParam("language"))
        .flatMapPublisher(language -> persister.load(language)
          .switchIfEmpty(Flowable.fromPublisher(adapter.getRepositoriesToContribute(language))))
        .compose(cache(context))
        .as(asJsonArray())
        .doOnError(Throwable::printStackTrace)
        .subscribe(
          repos -> respond(HttpResponseStatus.OK, repos.encodePrettily()).accept(context.request()),
          error -> respond(error).accept(context.request())
        )
      );

    return Single.fromCallable(vertx::createHttpServer)
      .flatMapCompletable(server -> server
        .requestHandler(router)
        .requestStream()
        .toFlowable()
        .onBackpressureDrop(respond(new RateLimitExceededException()))
        .ignoreElements()
        .andThen(server.rxListen(8080))
        .ignoreElement()
      );
  }

  private Consumer<HttpServerRequest> respond(HttpResponseStatus status, String content) {
    return request -> request.response()
      .setStatusCode(status.code())
      .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
      .end(content);
  }

  private Consumer<HttpServerRequest> respond(Throwable error) {
    String content = new JsonObject()
      .put("error", error.getMessage())
      .encodePrettily();

    if (error instanceof RateLimitExceededException) {
      return respond(HttpResponseStatus.TOO_MANY_REQUESTS, content);
    }

    return respond(HttpResponseStatus.INTERNAL_SERVER_ERROR, content);
  }

  private FlowableTransformer<Repository, Repository> cache(RoutingContext context) {
    MultiMap params = context.queryParams();

    return flow -> {
      if ("false".equals(params.get("cache"))) {
        return flow;
      }

      return flow.toList()
        .flatMapCompletable(repos -> persister.save(params.get("language"), repos))
        .andThen(flow);
    };
  }

  private static FlowableConverter<Repository, Single<JsonArray>> asJsonArray() {
    return flow -> flow.map(Repositories::toJson)
      .collect(JsonArray::new, JsonArray::add);
  }
}
