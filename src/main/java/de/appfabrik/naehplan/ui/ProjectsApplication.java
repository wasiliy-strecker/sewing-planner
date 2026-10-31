package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.application.ProjectService;
import de.appfabrik.naehplan.i18n.AppLanguage;
import de.appfabrik.naehplan.i18n.Messages;
import de.appfabrik.naehplan.persistence.AppDataDirectory;
import de.appfabrik.naehplan.persistence.LanguagePreferences;
import de.appfabrik.naehplan.persistence.SqliteProjectRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public final class ProjectsApplication extends Application {
  private final AsyncTasks tasks = new AsyncTasks();
  private Messages messages;
  private Path directory;
  private LanguagePreferences preferences;
  private String choice = AppLanguage.AUTOMATIC;
  private boolean languageReadFailed;
  private boolean invalidDirectory;

  @Override
  public void init() {
    Locale systemLocale = Locale.getDefault();
    messages = new Messages(AppLanguage.detect(systemLocale));
    try {
      String configured = getParameters().getNamed().get("data-dir");
      if (configured != null && configured.isBlank()) {
        throw new IllegalArgumentException("Empty data directory");
      }
      directory = configured == null ? AppDataDirectory.current() : Path.of(configured);
      preferences = new LanguagePreferences(directory);
      try {
        choice = preferences.load();
      } catch (IOException exception) {
        languageReadFailed = true;
      }
      messages = new Messages(AppLanguage.resolve(choice, systemLocale));
      Locale.setDefault(messages.locale());
    } catch (RuntimeException exception) {
      invalidDirectory = true;
    }
  }

  @Override
  public void start(Stage stage) {
    if (invalidDirectory) {
      Alert alert =
          new Alert(
              Alert.AlertType.ERROR,
              messages.text("start.error"),
              new ButtonType(messages.text("action.close")));
      alert.setTitle("NähPlan");
      alert.setHeaderText(messages.text("start.title"));
      UiSupport.style(alert.getDialogPane().getScene());
      UiSupport.icon((Stage) alert.getDialogPane().getScene().getWindow());
      alert.showAndWait();
      javafx.application.Platform.exit();
      return;
    }
    ProjectService service =
        new ProjectService(new SqliteProjectRepository(directory.resolve("naehplan.db")));
    new MainWindow(stage, service, tasks, messages, preferences, choice, languageReadFailed).show();
  }

  @Override
  public void stop() {
    tasks.close();
  }
}
