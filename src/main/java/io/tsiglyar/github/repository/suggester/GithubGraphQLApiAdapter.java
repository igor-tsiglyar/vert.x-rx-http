package io.tsiglyar.github.repository.suggester;

import io.tsiglyar.github.Repository;
import io.tsiglyar.github.adapter.GithubAdapter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import org.reactivestreams.Publisher;

import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class GithubGraphQLApiAdapter extends GithubRestAdapterBase implements GithubAdapter {

  private static final String GRAPHQL_URI = "/graphql";

  private final Vertx vertx;

  public GithubGraphQLApiAdapter(Vertx vertx) {
    super(vertx);
    this.vertx = vertx;
  }

  @Override
  public Publisher<Repository> getRepositoriesToContribute(String language) {
    return vertx.fileSystem().rxReadFile("src/main/resources/get-repositories-need-help.graphql")
      .map(Buffer::toString)
      .flatMap(query -> authenticate(client.post(GRAPHQL_URI))
        .rxSendJsonObject(new JsonObject()
          .put("query", format(query, language))))
      .flattenAsFlowable(response -> response.bodyAsJsonObject()
        .getJsonObject("data")
        .getJsonObject("search")
        .getJsonArray("nodes"))
      .cast(JsonObject.class)
      .map(Repositories::fromJson);
  }

  public void getRepositoriesToContribute(String language, Handler<AsyncResult<List<Repository>>> handler) {
    handler.handle(Future.<Buffer>future(fut -> vertx.fileSystem()
      .readFile("src/main/resources/get-repositories-need-help.graphql", fut)
    )
      .map(Buffer::toString)
      .compose(query -> Future.<HttpResponse<Buffer>>future(fut -> authenticate(client.post(GRAPHQL_URI))
        .sendJsonObject(new JsonObject()
          .put("query", format(query, language)), fut)))
      .map(response -> response.bodyAsJsonObject()
        .getJsonObject("data")
        .getJsonObject("search")
        .getJsonArray("nodes").stream()
          .map(repo -> Repositories.fromJson((JsonObject) repo))
          .collect(toList()))
    );
  }

}
