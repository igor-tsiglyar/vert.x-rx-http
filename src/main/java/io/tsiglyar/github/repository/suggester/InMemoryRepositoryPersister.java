package io.tsiglyar.github.repository.suggester;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.tsiglyar.github.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyList;

public class InMemoryRepositoryPersister implements RepositoryPersister {

  private Map<String, List<Repository>> repositories = new ConcurrentHashMap<>();

  public Flowable<Repository> load(String language) {
    return Flowable.fromIterable(repositories.getOrDefault(language, emptyList()));
  }

  public Completable save(String language, List<Repository> repositories) {
    return Completable.fromAction(() -> this.repositories.put(language, repositories));
  }

}
