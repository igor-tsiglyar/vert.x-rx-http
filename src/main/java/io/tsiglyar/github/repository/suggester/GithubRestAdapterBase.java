package io.tsiglyar.github.repository.suggester;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.WebClient;


public abstract class GithubRestAdapterBase {

  private static final String GITHUB_API_HOST = "api.github.com";
  private static final int GITHUB_API_PORT = 443;
  private static final String BEARER_TOKEN = "dfc88a1ee5a023f2884403f808ae4c7b2583544d";

  protected final WebClient client;

  protected GithubRestAdapterBase(Vertx vertx) {
    client = WebClient.create(vertx, new WebClientOptions()
      .setDefaultHost(GITHUB_API_HOST)
      .setDefaultPort(GITHUB_API_PORT)
      .setSsl(true));
  }

  protected static HttpRequest<Buffer> authenticate(HttpRequest<Buffer> request) {
    return request.bearerTokenAuthentication(BEARER_TOKEN);
  }

}
