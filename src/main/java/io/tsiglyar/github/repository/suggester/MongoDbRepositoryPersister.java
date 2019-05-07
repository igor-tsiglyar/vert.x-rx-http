package io.tsiglyar.github.repository.suggester;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.internal.functions.Functions;
import io.tsiglyar.github.Repository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mongo.MongoClient;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class MongoDbRepositoryPersister implements RepositoryPersister {

  private final MongoClient client;

  public MongoDbRepositoryPersister(Vertx vertx) {
    client = MongoClient.createNonShared(vertx, new JsonObject()
      .put("host", "localhost")
      .put("waitQueueMultiple", 1000));
  }

  @Override
  public Flowable<Repository> load(String language) {
    return client.rxFind(language, new JsonObject())
      .flattenAsFlowable(Functions.identity())
      .map(Repositories::fromJson);
  }

  @Override
  public void load(String language, Handler<AsyncResult<List<Repository>>> handler) {
    client.find(language, new JsonObject(), result -> handler.handle(result.map(list -> list.stream()
      .map(Repositories::fromJson)
      .collect(toList()))));
  }

  @Override
  public Completable save(String language, List<Repository> repositories) {
    return client.rxBulkWrite(language, repositories.stream()
      .map(repo -> BulkOperation.createInsert(Repositories.toJson(repo)))
        .collect(toList()))
      .ignoreElement();
  }

  @Override
  public void save(String language, List<Repository> repositories, Handler<AsyncResult<Void>> handler) {
    client.bulkWrite(language, repositories.stream()
      .map(repo -> BulkOperation.createInsert(Repositories.toJson(repo)))
      .collect(toList()), ignored -> handler.handle(Future.succeededFuture()));
  }

}
