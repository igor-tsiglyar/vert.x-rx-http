package io.tsiglyar.github.repository.suggester;

import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GithubAdapterTest {

  private Vertx vertx;

  @BeforeEach
  void prepare() {
    vertx = Vertx.vertx();
  }

  @AfterEach
  void cleanup() {
    vertx.close();
  }

  @Test
  void getRepositoriesNeedHelp() {
    Flowable.fromPublisher(new VertxGithubAdapter(vertx)
      .getRepositoriesToContribute("Java"))
      .blockingIterable()
      .forEach(System.out::println);
  }

}
