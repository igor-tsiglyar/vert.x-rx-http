package io.tsiglyar.github.repository.suggester;

import com.datastax.driver.core.RegularStatement;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.tsiglyar.github.Repository;
import io.vertx.reactivex.cassandra.CassandraClient;
import io.vertx.reactivex.cassandra.CassandraRowStream;
import io.vertx.reactivex.core.Vertx;

import java.util.List;

import static com.datastax.driver.core.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static io.tsiglyar.github.repository.suggester.Repositories.toJson;

public class CassandraRepositoryPersister implements RepositoryPersister {

  private final CassandraClient client;

  public CassandraRepositoryPersister(Vertx vertx) {
    client = CassandraClient.createNonShared(vertx);
  }

  @Override
  public Flowable<Repository> load(String language) {
    return client.rxQueryStream(select()
      .all()
      .from("repositories")
      .where(eq("language", language))
    )
      .flatMapPublisher(CassandraRowStream::toFlowable)
      .map(row -> new Repository(row.getString("name"),
        row.getString("description"), row.getString("url")));
  }

  @Override
  public Completable save(String language, List<Repository> repositories) {
    return client.rxExecute(batch(repositories.stream()
      .map(repo -> insertInto("repositories")
        .json(toJson(repo).put("language", language).encode()))
      .toArray(RegularStatement[]::new))
    )
      .ignoreElement();
  }

}
