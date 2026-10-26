package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.application.PhotoException;
import de.appfabrik.naehplan.application.StorageException;
import java.util.Objects;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

final class UiSupport {
  private UiSupport() {}

  static void style(Scene scene) {
    scene
        .getStylesheets()
        .add(
            Objects.requireNonNull(
                    UiSupport.class.getResource("/de/appfabrik/naehplan/projects.css"))
                .toExternalForm());
  }

  static String message(Throwable failure) {
    return failure instanceof StorageException || failure instanceof PhotoException
        ? failure.getMessage()
        : "Der Vorgang konnte nicht abgeschlossen werden. Bitte versuche es erneut. Deine Eingaben bleiben erhalten.";
  }

  static boolean confirm(
      Window owner, String name, String detail, String action, String cancelText) {
    ButtonType cancel = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);
    ButtonType accept = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, detail, cancel, accept);
    alert.initOwner(owner);
    alert.setTitle(name);
    alert.setHeaderText(name);
    alert.getDialogPane().setPrefWidth(460);
    alert.getDialogPane().lookupButton(accept).setStyle("-fx-base: #f3e6e0");
    ((javafx.scene.control.Button) alert.getDialogPane().lookupButton(accept))
        .setDefaultButton(false);
    ((javafx.scene.control.Button) alert.getDialogPane().lookupButton(cancel))
        .setDefaultButton(true);
    return alert.showAndWait().filter(accept::equals).isPresent();
  }
}
