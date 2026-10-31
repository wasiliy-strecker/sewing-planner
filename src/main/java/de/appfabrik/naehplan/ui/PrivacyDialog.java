package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.i18n.Messages;
import de.appfabrik.naehplan.i18n.PrivacyNotice;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

final class PrivacyDialog {
  private final Stage stage = new Stage();
  private final PauseTransition placement = new PauseTransition(Duration.millis(150));
  private final PauseTransition reveal = new PauseTransition(Duration.millis(100));

  PrivacyDialog(Window owner, Messages messages) {
    stage.initOwner(owner);
    stage.initModality(Modality.WINDOW_MODAL);
    stage.setTitle(messages.text("privacy.title"));
    UiSupport.icon(stage);
    TextArea text = new TextArea(PrivacyNotice.text(messages));
    text.setId("privacy-text");
    text.setAccessibleText(messages.text("privacy.title"));
    text.setEditable(false);
    text.setWrapText(true);
    UiSupport.textMenu(text, messages);
    Button close = new Button(messages.text("action.close"));
    close.setId("privacy-close");
    close.setCancelButton(true);
    close.setOnAction(event -> stage.close());
    HBox actions = new HBox(close);
    actions.setAlignment(Pos.CENTER_RIGHT);
    VBox root = new VBox(16, text, actions);
    root.setPadding(new Insets(24));
    root.setPrefSize(780, 660);
    VBox.setVgrow(text, Priority.ALWAYS);
    Scene scene = new Scene(root);
    UiSupport.style(scene);
    stage.setScene(scene);
    stage.setMinWidth(560);
    stage.setMinHeight(420);
    // Native window decorations can change the size after the shown event.
    placement.setOnFinished(
        event -> {
          centerOnOwner();
          if (stage.isShowing() && stage.getOpacity() == 0) {
            reveal.playFromStart();
          }
        });
    reveal.setOnFinished(event -> stage.setOpacity(1));
    stage.widthProperty().addListener(observable -> schedulePlacement());
    stage.heightProperty().addListener(observable -> schedulePlacement());
    stage.setOnShown(
        event -> {
          stage.sizeToScene();
          schedulePlacement();
        });
    stage.setOnHidden(
        event -> {
          placement.stop();
          reveal.stop();
        });
  }

  private void schedulePlacement() {
    reveal.stop();
    placement.playFromStart();
  }

  private void centerOnOwner() {
    if (!stage.isShowing()) {
      return;
    }
    Window owner = stage.getOwner();
    var screens =
        Screen.getScreensForRectangle(
            owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight());
    var bounds = (screens.isEmpty() ? Screen.getPrimary() : screens.getFirst()).getVisualBounds();
    double x = owner.getX() + (owner.getWidth() - stage.getWidth()) / 2;
    double y = owner.getY() + (owner.getHeight() - stage.getHeight()) / 2;
    stage.setX(Math.max(bounds.getMinX(), Math.min(x, bounds.getMaxX() - stage.getWidth())));
    stage.setY(Math.max(bounds.getMinY(), Math.min(y, bounds.getMaxY() - stage.getHeight())));
  }

  void show() {
    stage.setOpacity(0);
    stage.show();
  }
}
