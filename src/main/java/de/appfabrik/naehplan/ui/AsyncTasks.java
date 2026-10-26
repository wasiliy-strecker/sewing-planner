package de.appfabrik.naehplan.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.concurrent.Task;

/** Serial database work. JavaFX Task delivers both callbacks on the UI thread. */
final class AsyncTasks implements AutoCloseable {
  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread thread = new Thread(r, "projects-database");
            thread.setDaemon(true);
            return thread;
          });

  <T> void submit(Callable<T> operation, Consumer<T> success, Consumer<Throwable> failure) {
    Task<T> task =
        new Task<>() {
          @Override
          protected T call() throws Exception {
            return operation.call();
          }
        };
    task.setOnSucceeded(event -> success.accept(task.getValue()));
    task.setOnFailed(event -> failure.accept(task.getException()));
    executor.execute(task);
  }

  @Override
  public void close() {
    executor.shutdown();
  }
}
