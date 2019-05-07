package io.tsiglyar.github.repository.suggester;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.tsiglyar.github.Repository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.List;

public interface RepositoryPersister {

  Flowable<Repository> load(String language);

  void load(String language, Handler<AsyncResult<List<Repository>>> handler);

  Completable save(String language, List<Repository> repositories);

  void save(String language, List<Repository> repositories, Handler<AsyncResult<Void>> handler);

}
