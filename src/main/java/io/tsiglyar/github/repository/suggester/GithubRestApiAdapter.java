package io.tsiglyar.github.repository.suggester;

import io.tsiglyar.github.Repository;
import io.tsiglyar.github.adapter.GithubAdapter;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import org.reactivestreams.Publisher;

import static java.lang.String.format;

public class GithubRestApiAdapter extends GithubRestAdapterBase implements GithubAdapter {

  private static final String SEARCH_REPOS_NEED_HELP_URI_TEMPLATE
    = "/search/repositories?q=language:%s+help-wanted-issues:>0+sort:help-wanted-issues";

  public GithubRestApiAdapter(Vertx vertx) {
    super(vertx);
  }

  @Override
  public Publisher<Repository> getRepositoriesToContribute(String language) {
    return client.get(format(SEARCH_REPOS_NEED_HELP_URI_TEMPLATE, language))
      .putHeader("Content-Type", "application/json")
      .rxSend()
      .flattenAsFlowable(response -> response.bodyAsJsonObject().getJsonArray("items"))
      .cast(JsonObject.class)
      .map(Repositories::fromJson);
  }
}
