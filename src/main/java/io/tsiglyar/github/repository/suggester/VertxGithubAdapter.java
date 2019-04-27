package io.tsiglyar.github.repository.suggester;

import io.tsiglyar.github.Repository;
import io.tsiglyar.github.adapter.GithubAdapter;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.reactivestreams.Publisher;

import static java.lang.String.format;

public class VertxGithubAdapter implements GithubAdapter {

  private static final String GITHUB_API_HOST = "api.github.com";
  private static final String SEARCH_REPOS_NEED_HELP_URI_TEMPLATE
    = "/search/repositories?client_id=992213e727e23340951b&client_secret=ba38391d31a735e46a3767390f37236c05b6c6d3&q=language:%s&sort=help+wanted";

  private WebClient client;

  public VertxGithubAdapter(Vertx vertx) {
    client = WebClient.create(vertx, new WebClientOptions()
      .setSsl(true));
  }

  @Override
  public Publisher<Repository> getRepositoriesToContribute(String language) {
    String uri = format(SEARCH_REPOS_NEED_HELP_URI_TEMPLATE, language);
    return client.get(443, GITHUB_API_HOST, uri)
      .putHeader("Content-Type", "application/json")
      .rxSend()
      .flattenAsFlowable(response -> response.bodyAsJsonObject().getJsonArray("items"))
      .cast(JsonObject.class)
      .map(Repositories::fromJson);
  }
}
