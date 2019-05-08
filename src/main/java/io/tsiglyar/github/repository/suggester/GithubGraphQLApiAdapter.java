package io.tsiglyar.github.repository.suggester;

import io.tsiglyar.github.Repository;
import io.tsiglyar.github.adapter.GithubAdapter;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import org.reactivestreams.Publisher;

import static io.tsiglyar.github.repository.suggester.RxHelpers.load;
import static java.lang.String.format;

public class GithubGraphQLApiAdapter extends GithubRestAdapterBase implements GithubAdapter {

  private static final String GRAPHQL_URI = "/graphql";

  private final Vertx vertx;

  public GithubGraphQLApiAdapter(Vertx vertx) {
    super(vertx);
    this.vertx = vertx;
  }

  @Override
  public Publisher<Repository> getRepositoriesToContribute(String language) {
    return load(() -> vertx.fileSystem().rxReadFile("src/main/resources/get-repositories-need-help.graphql")
      .map(Buffer::toString)
      .flatMap(query -> authenticate(client.post(GRAPHQL_URI))
        .rxSendJsonObject(new JsonObject()
          .put("query", format(query, language))))
      .flattenAsFlowable(response -> response.bodyAsJsonObject()
        .getJsonObject("data")
        .getJsonObject("search")
        .getJsonArray("nodes"))
      .cast(JsonObject.class)
      .map(Repositories::fromJson));
  }

}
