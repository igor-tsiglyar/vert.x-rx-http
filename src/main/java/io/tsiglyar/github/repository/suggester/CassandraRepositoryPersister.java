package io.tsiglyar.github.repository.suggester;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.tsiglyar.github.Repository;
import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.cassandra.CassandraClient;
import io.vertx.reactivex.cassandra.CassandraRowStream;
import io.vertx.reactivex.core.Vertx;

import java.util.List;

import static com.datastax.driver.core.DataType.text;
import static com.datastax.driver.core.querybuilder.QueryBuilder.batch;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static io.tsiglyar.github.repository.suggester.Repositories.toJson;

public class CassandraRepositoryPersister implements RepositoryPersister {

  private static String KEYSPACE = "repositories";

  private final CassandraClient client;

  public CassandraRepositoryPersister(Vertx vertx) {
    client = CassandraClient.createNonShared(vertx, new CassandraClientOptions(Cluster.builder()
        .withProtocolVersion(ProtocolVersion.V4)
        .withPoolingOptions(new PoolingOptions()
          .setConnectionsPerHost(HostDistance.LOCAL, 1, 1)
          .setMaxRequestsPerConnection(HostDistance.LOCAL, 32768)
          .setNewConnectionThreshold(HostDistance.LOCAL, 30000)
          .setPoolTimeoutMillis(0)))
      );
  }

  @Override
  public Flowable<Repository> load(String language) {
    return client.rxQueryStream(select()
      .all()
      .from(KEYSPACE, language)
    )
      .flatMapPublisher(CassandraRowStream::toFlowable)
      .onErrorResumeNext(createKeyspace()
        .andThen(createTable(language))
        .andThen(Flowable.empty()))
      .map(row -> new Repository(row.getString("name"), row.getString("description"), row.getString("url")));
  }

  private Completable createKeyspace() {
    return client.rxExecute(SchemaBuilder.createKeyspace(KEYSPACE)
      .ifNotExists()
      .with()
      .replication(new JsonObject()
        .put("class", "SimpleStrategy")
        .put("replication_factor", 1)
        .getMap()))
      .ignoreElement();
  }

  private Completable createTable(String name) {
    return client.rxExecute(SchemaBuilder.createTable(KEYSPACE, name)
      .ifNotExists()
      .addPartitionKey("name", text())
      .addColumn("description", text())
      .addColumn("url", text())
    )
      .ignoreElement();
  }

  @Override
  public Completable save(String language, List<Repository> repositories) {
    return client.rxExecute(batch(repositories.stream()
      .map(repo -> insertInto(KEYSPACE, language)
        .json(toJson(repo).encode()))
      .toArray(RegularStatement[]::new))
    )
      .ignoreElement();
  }

}
