package de.appfabrik.naehplan.ui;

import javafx.animation.PauseTransition;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.scene.control.ColorPicker;
import javafx.scene.layout.Region;
import javafx.stage.PopupWindow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/** Sizes the native color dialog after CSS and window decorations are available. */
final class CustomColorDialogSizing {
  private final ColorPicker picker;
  private final ListChangeListener<Window> windows = this::windowsChanged;
  private final WeakListChangeListener<Window> observer = new WeakListChangeListener<>(windows);
  private boolean observing;

  CustomColorDialogSizing(ColorPicker picker) {
    this.picker = picker;
    picker.showingProperty().addListener((observable, previous, showing) -> observe(showing));
    picker
        .sceneProperty()
        .addListener(
            (observable, previous, scene) -> {
              if (scene == null) {
                observe(false);
              }
            });
  }

  private void observe(boolean value) {
    if (observing == value) {
      return;
    }
    observing = value;
    if (value) {
      Window.getWindows().addListener(observer);
    } else {
      Window.getWindows().removeListener(observer);
    }
  }

  private void windowsChanged(ListChangeListener.Change<? extends Window> change) {
    while (change.next()) {
      for (Window window : change.getAddedSubList()) {
        if (window instanceof Stage dialog
            && dialog.getOwner() instanceof PopupWindow popup
            && picker.getScene() != null
            && popup.getOwnerWindow() == picker.getScene().getWindow()
            && dialog.getScene() != null
            && dialog.getScene().getRoot().getStyleClass().contains("custom-color-dialog")) {
          prepare(dialog);
        }
      }
    }
  }

  private static void prepare(Stage dialog) {
    if (dialog.getProperties().putIfAbsent(CustomColorDialogSizing.class, Boolean.TRUE) != null) {
      return;
    }
    dialog.setResizable(true);
    var size = new PauseTransition(Duration.millis(150));
    var reveal = new PauseTransition(Duration.millis(100));
    size.setOnFinished(
        event -> {
          if (!dialog.isShowing()) {
            return;
          }
          Region root = (Region) dialog.getScene().getRoot();
          root.applyCss();
          root.layout();
          double borderWidth = Math.max(0, dialog.getWidth() - dialog.getScene().getWidth());
          double borderHeight = Math.max(0, dialog.getHeight() - dialog.getScene().getHeight());
          dialog.setMinWidth(root.minWidth(-1) + borderWidth);
          dialog.setMinHeight(root.minHeight(-1) + borderHeight);
          dialog.sizeToScene();
          reveal.playFromStart();
        });
    reveal.setOnFinished(
        event -> {
          if (!dialog.isShowing()) {
            return;
          }
          Window owner = ((PopupWindow) dialog.getOwner()).getOwnerWindow();
          var screens =
              Screen.getScreensForRectangle(
                  owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight());
          var screen =
              (screens.isEmpty() ? Screen.getPrimary() : screens.getFirst()).getVisualBounds();
          double x = owner.getX() + (owner.getWidth() - dialog.getWidth()) / 2;
          double y = owner.getY() + (owner.getHeight() - dialog.getHeight()) / 2;
          dialog.setX(
              Math.max(screen.getMinX(), Math.min(x, screen.getMaxX() - dialog.getWidth())));
          dialog.setY(
              Math.max(screen.getMinY(), Math.min(y, screen.getMaxY() - dialog.getHeight())));
          dialog.setOpacity(1);
        });
    dialog.addEventHandler(WindowEvent.WINDOW_SHOWING, event -> dialog.setOpacity(0));
    dialog.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> size.playFromStart());
    dialog.addEventHandler(
        WindowEvent.WINDOW_HIDDEN,
        event -> {
          size.stop();
          reveal.stop();
        });
    dialog.setOpacity(0);
    size.playFromStart();
  }
}
