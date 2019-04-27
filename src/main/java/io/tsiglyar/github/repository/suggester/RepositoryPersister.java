package io.tsiglyar.github.repository.suggester;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.tsiglyar.github.Repository;

import java.util.List;

public interface RepositoryPersister {

  Flowable<Repository> load(String language);

  Completable save(String language, List<Repository> repositories);

}
