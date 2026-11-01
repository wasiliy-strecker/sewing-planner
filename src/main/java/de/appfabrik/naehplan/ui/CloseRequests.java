package de.appfabrik.naehplan.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import javafx.application.Platform;

/** Coalesces a native quit's owner and editor requests before asking about one draft. */
final class CloseRequests {
  private final BooleanSupplier closeEditor;
  private final List<Runnable> afterClose = new ArrayList<>();
  private boolean pending;

  CloseRequests(BooleanSupplier closeEditor) {
    this.closeEditor = closeEditor;
  }

  void request(Runnable completed) {
    afterClose.add(completed);
    if (pending) {
      return;
    }
    pending = true;
    // macOS sends a close request to every window in one native event.
    // Defer the confirmation until all those requests have been collected.
    Platform.runLater(this::finish);
  }

  private void finish() {
    try {
      if (closeEditor.getAsBoolean()) {
        List.copyOf(afterClose).forEach(Runnable::run);
      }
    } finally {
      afterClose.clear();
      pending = false;
    }
  }
}
