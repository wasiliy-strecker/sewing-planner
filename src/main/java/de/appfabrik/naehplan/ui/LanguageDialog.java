package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.i18n.AppLanguage;
import de.appfabrik.naehplan.i18n.Messages;
import de.appfabrik.naehplan.persistence.LanguagePreferences;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

final class LanguageDialog {
  private final Stage stage = new Stage();
  private boolean saving;

  LanguageDialog(
      Window owner,
      Messages messages,
      AsyncTasks tasks,
      LanguagePreferences preferences,
      String current,
      Consumer<String> saved) {
    stage.initOwner(owner);
    stage.initModality(Modality.WINDOW_MODAL);
    stage.setTitle(messages.text("language.title"));
    UiSupport.icon(stage);
    Label heading = new Label(messages.text("language.title"));
    heading.getStyleClass().add("section-heading");
    Label hint = new Label(messages.text("language.hint"));
    hint.setWrapText(true);
    hint.setMinHeight(Region.USE_PREF_SIZE);
    ComboBox<Choice> languages = new ComboBox<>();
    languages.setId("language-choice");
    languages.setAccessibleText(messages.text("language.title"));
    languages
        .getItems()
        .add(new Choice(AppLanguage.AUTOMATIC, messages.text("language.automatic")));
    for (AppLanguage language : AppLanguage.values()) {
      languages.getItems().add(new Choice(language.tag(), language.nativeName()));
    }
    languages.getItems().stream()
        .filter(choice -> choice.id().equals(current))
        .findFirst()
        .ifPresent(languages::setValue);
    if (languages.getValue() == null) {
      languages.getSelectionModel().selectFirst();
    }
    languages.setMaxWidth(Double.MAX_VALUE);
    Label error = new Label();
    error.setId("language-error");
    error.setWrapText(true);
    error.setMinHeight(Region.USE_PREF_SIZE);
    error.getStyleClass().add("error-text");
    error.setVisible(false);
    error.managedProperty().bind(error.visibleProperty());
    Button cancel = new Button(messages.text("action.cancel"));
    cancel.setId("language-cancel");
    cancel.setCancelButton(true);
    cancel.setOnAction(
        event -> {
          if (!saving) {
            stage.hide();
          }
        });
    Button save = new Button(messages.text("action.save"));
    save.setId("language-save");
    save.getStyleClass().add("primary");
    save.setDefaultButton(true);
    save.setOnAction(
        event -> {
          if (saving) {
            return;
          }
          String choice = languages.getValue().id();
          saving = true;
          languages.setDisable(true);
          save.setDisable(true);
          cancel.setDisable(true);
          error.setVisible(false);
          tasks.submit(
              () -> {
                preferences.save(choice);
                return choice;
              },
              result -> {
                saving = false;
                saved.accept(result);
                stage.hide();
              },
              failure -> {
                saving = false;
                languages.setDisable(false);
                save.setDisable(false);
                cancel.setDisable(false);
                error.setText(messages.text("language.saveError"));
                error.setVisible(true);
                stage.sizeToScene();
              });
        });
    HBox actions = new HBox(12, cancel, save);
    actions.setAlignment(Pos.CENTER_RIGHT);
    VBox root = new VBox(20, heading, hint, languages, error, actions);
    root.setPadding(new Insets(28));
    Scene scene = new Scene(root, 560, 300);
    UiSupport.style(scene);
    stage.setScene(scene);
    stage.setMinWidth(500);
    stage.setMinHeight(330);
    stage.setOnCloseRequest(
        event -> {
          if (saving) {
            event.consume();
          }
        });
  }

  void show() {
    stage.show();
  }

  private record Choice(String id, String name) {
    @Override
    public String toString() {
      return name;
    }
  }
}
