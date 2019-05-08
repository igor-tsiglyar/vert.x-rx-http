package io.tsiglyar.github.repository.suggester;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Publisher;

import java.util.function.Supplier;

public final class RxHelpers {

  private RxHelpers() {
    throw new UnsupportedOperationException();
  }

  public static <T> Publisher<T> load(Supplier<? extends Publisher<T>> loader) {
    return Single.just(loader)
      .observeOn(Schedulers.io())
      .flatMapPublisher(Supplier::get);
  }

  public static Completable save(Supplier<Completable> saver) {
    return Single.just(saver)
      .observeOn(Schedulers.io())
      .flatMapCompletable(Supplier::get);
  }

}
