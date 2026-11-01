package de.appfabrik.naehplan.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javafx.application.Platform;

final class FxTestSupport {
  private static boolean started;

  private FxTestSupport() {}

  static synchronized void start() throws Exception {
    if (started) {
      return;
    }
    var ready = new CountDownLatch(1);
    Platform.startup(
        () -> {
          Platform.setImplicitExit(false);
          ready.countDown();
        });
    assertTrue(ready.await(10, TimeUnit.SECONDS));
    started = true;
  }

  static <T> T fx(Callable<T> action) throws Exception {
    var task = new FutureTask<>(action);
    Platform.runLater(task);
    return task.get(10, TimeUnit.SECONDS);
  }

  static <T> T uncheckedFx(Callable<T> action) {
    try {
      return fx(action);
    } catch (Exception exception) {
      throw new AssertionError(exception);
    }
  }

  static void await(BooleanSupplier condition) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) {
        fail("Timed out waiting for JavaFX");
      }
      Thread.sleep(20);
    }
  }
}
