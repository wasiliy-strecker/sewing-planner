package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.application.CollectionLimitException;
import de.appfabrik.naehplan.application.PhotoException;
import de.appfabrik.naehplan.application.StorageException;
import de.appfabrik.naehplan.i18n.Messages;
import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

final class UiSupport {
  private UiSupport() {}

  static void style(Scene scene) {
    UiFonts.load();
    scene
        .getStylesheets()
        .add(
            Objects.requireNonNull(
                    UiSupport.class.getResource("/de/appfabrik/naehplan/projects.css"))
                .toExternalForm());
  }

  static String message(Throwable failure, Messages messages) {
    if (failure instanceof CollectionLimitException limit) {
      return messages.text("limit." + limit.code().name());
    }
    return failure instanceof StorageException
        ? messages.text("storage." + ((StorageException) failure).code().name())
        : failure instanceof PhotoException photo
            ? messages.text("photoError." + photo.code().name())
            : messages.text("error.generic");
  }

  static void icon(Stage stage) {
    for (int size : new int[] {16, 32, 48, 64, 128, 256}) {
      stage
          .getIcons()
          .add(
              new Image(
                  Objects.requireNonNull(
                          UiSupport.class.getResource(
                              "/de/appfabrik/naehplan/icon-" + size + ".png"))
                      .toExternalForm()));
    }
  }

  static void centerAfterLayout(Stage stage) {
    PauseTransition placement = new PauseTransition(Duration.millis(150));
    PauseTransition reveal = new PauseTransition(Duration.millis(100));
    // Native window decorations can change the size after the shown event.
    Runnable schedulePlacement =
        () -> {
          reveal.stop();
          placement.playFromStart();
        };
    placement.setOnFinished(
        event -> {
          if (!stage.isShowing()) {
            return;
          }
          Window owner = stage.getOwner();
          var screens =
              Screen.getScreensForRectangle(
                  owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight());
          var bounds =
              (screens.isEmpty() ? Screen.getPrimary() : screens.getFirst()).getVisualBounds();
          double x = owner.getX() + (owner.getWidth() - stage.getWidth()) / 2;
          double y = owner.getY() + (owner.getHeight() - stage.getHeight()) / 2;
          stage.setX(Math.max(bounds.getMinX(), Math.min(x, bounds.getMaxX() - stage.getWidth())));
          stage.setY(Math.max(bounds.getMinY(), Math.min(y, bounds.getMaxY() - stage.getHeight())));
          if (stage.getOpacity() == 0) {
            reveal.playFromStart();
          }
        });
    reveal.setOnFinished(event -> stage.setOpacity(1));
    stage.widthProperty().addListener(observable -> schedulePlacement.run());
    stage.heightProperty().addListener(observable -> schedulePlacement.run());
    stage.setOnShown(
        event -> {
          stage.sizeToScene();
          schedulePlacement.run();
        });
    stage.setOnHidden(
        event -> {
          placement.stop();
          reveal.stop();
        });
  }

  static void textMenu(TextInputControl control, Messages messages) {
    MenuItem undo = item(messages, "context.undo", control::undo);
    MenuItem redo = item(messages, "context.redo", control::redo);
    MenuItem cut = item(messages, "context.cut", control::cut);
    MenuItem copy = item(messages, "context.copy", control::copy);
    MenuItem paste = item(messages, "context.paste", control::paste);
    MenuItem select = item(messages, "context.selectAll", control::selectAll);
    undo.disableProperty()
        .bind(control.undoableProperty().not().or(control.editableProperty().not()));
    redo.disableProperty()
        .bind(control.redoableProperty().not().or(control.editableProperty().not()));
    cut.disableProperty()
        .bind(control.selectedTextProperty().isEmpty().or(control.editableProperty().not()));
    copy.disableProperty().bind(control.selectedTextProperty().isEmpty());
    paste.disableProperty().bind(control.editableProperty().not());
    select.disableProperty().bind(control.textProperty().isEmpty());
    control.setContextMenu(new ContextMenu(undo, redo, cut, copy, paste, select));
  }

  private static MenuItem item(Messages messages, String key, Runnable action) {
    MenuItem item = new MenuItem(messages.text(key));
    item.setOnAction(event -> action.run());
    return item;
  }

  static boolean confirm(
      Window owner, String title, String detail, String action, String cancelText) {
    ButtonType cancel = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);
    ButtonType accept = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, detail, cancel, accept);
    alert.initOwner(owner);
    alert.setTitle(title);
    alert.setHeaderText(title);
    alert.getDialogPane().setPrefWidth(520);
    style(alert.getDialogPane().getScene());
    icon((Stage) alert.getDialogPane().getScene().getWindow());
    alert.getDialogPane().lookupButton(accept).setStyle("-fx-base: #f3e6e0");
    ((javafx.scene.control.Button) alert.getDialogPane().lookupButton(accept))
        .setDefaultButton(false);
    ((javafx.scene.control.Button) alert.getDialogPane().lookupButton(cancel))
        .setDefaultButton(true);
    return alert.showAndWait().filter(accept::equals).isPresent();
  }
}
