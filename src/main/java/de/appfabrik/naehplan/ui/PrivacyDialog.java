package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.i18n.Messages;
import de.appfabrik.naehplan.i18n.PrivacyNotice;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

final class PrivacyDialog {
  private final Stage stage = new Stage();

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
    UiSupport.centerAfterLayout(stage);
  }

  void show() {
    stage.setOpacity(0);
    stage.show();
  }
}
