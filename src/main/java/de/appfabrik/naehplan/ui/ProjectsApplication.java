package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.application.CollectionExport;
import de.appfabrik.naehplan.application.CollectionImport;
import de.appfabrik.naehplan.application.ProjectService;
import de.appfabrik.naehplan.i18n.AppLanguage;
import de.appfabrik.naehplan.i18n.Messages;
import de.appfabrik.naehplan.persistence.AppDataDirectory;
import de.appfabrik.naehplan.persistence.JsonCollectionFile;
import de.appfabrik.naehplan.persistence.LanguagePreferences;
import de.appfabrik.naehplan.persistence.PhotoDirectoryPreferences;
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
  private PhotoDirectoryPreferences photoDirectories;
  private String choice = AppLanguage.ENGLISH.tag();
  private boolean languageReadFailed;
  private boolean invalidDirectory;

  @Override
  public void init() {
    Locale systemLocale = Locale.getDefault();
    messages = new Messages(AppLanguage.ENGLISH);
    try {
      String configured = getParameters().getNamed().get("data-dir");
      if (configured != null && configured.isBlank()) {
        throw new IllegalArgumentException("Empty data directory");
      }
      directory = configured == null ? AppDataDirectory.current() : Path.of(configured);
      preferences = new LanguagePreferences(directory);
      photoDirectories = new PhotoDirectoryPreferences(directory);
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
      alert.setTitle("Sewing Planner");
      alert.setHeaderText(messages.text("start.title"));
      UiSupport.style(alert.getDialogPane().getScene());
      UiSupport.icon((Stage) alert.getDialogPane().getScene().getWindow());
      alert.showAndWait();
      javafx.application.Platform.exit();
      return;
    }
    var repository = new SqliteProjectRepository(directory.resolve("naehplan.db"));
    ProjectService service = new ProjectService(repository);
    var exports = new CollectionExport(repository, new JsonCollectionFile());
    var imports = new CollectionImport(repository, new JsonCollectionFile());
    new MainWindow(
            stage,
            service,
            exports,
            imports,
            tasks,
            messages,
            preferences,
            photoDirectories,
            choice,
            languageReadFailed)
        .show();
  }

  @Override
  public void stop() {
    tasks.close();
  }
}
