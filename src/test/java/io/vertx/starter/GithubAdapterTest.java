package io.vertx.starter;

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
    new GithubAdapter(vertx)
      .getRepositoriesToContribute("Java")
      .blockingIterable()
      .forEach(System.out::println);
  }

}
