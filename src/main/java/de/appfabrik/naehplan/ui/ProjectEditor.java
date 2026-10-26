package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.application.PhotoImporter;
import de.appfabrik.naehplan.application.ProjectService;
import de.appfabrik.naehplan.domain.Photo;
import de.appfabrik.naehplan.domain.Project;
import de.appfabrik.naehplan.domain.ProjectDraft;
import de.appfabrik.naehplan.domain.ProjectStatus;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

final class ProjectEditor {
  private final Stage stage = new Stage();
  private final TextField name = new TextField();
  private final TextField material = new TextField();
  private final TextField color = new TextField();
  private final TextField location = new TextField();
  private final ComboBox<ProjectStatus> status = new ComboBox<>();
  private final TextArea note = new TextArea();
  private final Label validation = new Label();
  private final Label error = new Label();
  private final Label progress = new Label();
  private final Button save = new Button("Speichern");
  private final Button cancel = new Button("Abbrechen");
  private final Button choose = new Button("Foto auswählen …");
  private final Button remove = new Button("Foto entfernen");
  private final PhotoPreview preview = new PhotoPreview(232, 170);
  private final ProjectService service;
  private final AsyncTasks tasks;
  private final Long id;
  private final Input original;
  private final Consumer<Project> onSaved;
  private final ChecklistEditor checklist;
  private Photo photo;
  private boolean busy;

  ProjectEditor(
      Window owner,
      Project existing,
      Photo existingPhoto,
      ProjectService service,
      AsyncTasks tasks,
      Consumer<Project> onSaved) {
    this.service = service;
    this.tasks = tasks;
    this.onSaved = onSaved;
    id = existing == null ? null : existing.id();
    photo = existingPhoto;
    checklist = new ChecklistEditor(existing == null ? List.of() : existing.steps());
    status.getItems().setAll(ProjectStatus.values());
    status.setValue(existing == null ? ProjectStatus.PLANNED : existing.status());
    status.setCellFactory(list -> statusCell());
    status.setButtonCell(statusCell());
    status.setMaxWidth(Double.MAX_VALUE);
    status.setId("project-status");
    if (existing != null) {
      name.setText(existing.name());
      material.setText(existing.material());
      color.setText(existing.color());
      location.setText(existing.location());
      note.setText(existing.note());
    }
    original = input();
    stage.initOwner(owner);
    stage.initModality(Modality.WINDOW_MODAL);
    stage.setTitle(existing == null ? "Projekt hinzufügen" : "Projekt bearbeiten");
    stage.setMinWidth(660);
    stage.setMinHeight(540);
    Label heading = new Label(stage.getTitle());
    heading.getStyleClass().add("section-heading");
    Label hint = new Label("Nur der Projektname ist erforderlich.");
    hint.getStyleClass().add("muted");
    name.setId("project-name");
    material.setId("project-material");
    color.setId("project-color");
    location.setId("project-location");
    note.setId("project-note");
    name.setPromptText("Zum Beispiel Tasche für den Alltag");
    material.setPromptText("Zum Beispiel Leinen oder Baumwolle");
    color.setPromptText("Zum Beispiel Salbeigrün");
    location.setPromptText("Zum Beispiel Schrank, Box 2");
    note.setPromptText("Was möchtest du dir zu diesem Projekt merken?");
    note.setWrapText(true);
    note.setPrefRowCount(4);
    for (Label label : new Label[] {validation, error, progress}) {
      label.setWrapText(true);
      label.setMinHeight(Region.USE_PREF_SIZE);
      label.setVisible(false);
      label.managedProperty().bind(label.visibleProperty());
    }
    validation.getStyleClass().add("error-text");
    error.getStyleClass().add("error-text");
    progress.getStyleClass().add("muted");
    name.textProperty()
        .addListener((observable, previous, current) -> validation.setVisible(false));
    save.getStyleClass().add("primary");
    save.setId("save-project");
    save.setDefaultButton(true);
    save.setOnAction(event -> save());
    cancel.setCancelButton(true);
    cancel.setId("cancel-project");
    cancel.setOnAction(event -> requestClose());
    choose.setId("choose-photo");
    choose.setOnAction(event -> choosePhoto());
    remove.setId("remove-photo");
    remove.setOnAction(
        event -> {
          photo = null;
          preview.showImage(null);
          updatePhotoButtons();
          error.setVisible(false);
        });
    Label photoHint =
        new Label(
            "Ein JPEG oder PNG. Max. 10 MiB, 6000 Pixel je Seite und 16 Millionen Pixel insgesamt.");
    photoHint.setWrapText(true);
    photoHint.setMaxWidth(232);
    photoHint.getStyleClass().add("muted");
    VBox photoForm = new VBox(10, new Label("Foto · optional"), preview, choose, remove, photoHint);
    VBox fields =
        new VBox(
            12,
            field("Projektname *", name),
            field("Status", status),
            field("Stoff / Material", material),
            field("Farbe", color),
            field("Aufbewahrungsort", location),
            field("Notiz", note));
    fields.setMinWidth(280);
    HBox.setHgrow(fields, Priority.ALWAYS);
    HBox columns = new HBox(24, fields, photoForm);
    columns.setPadding(new Insets(2));
    ScrollPane scroll = new ScrollPane(new VBox(22, columns, checklist));
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    HBox actions = new HBox(10, cancel, save);
    actions.setAlignment(Pos.CENTER_RIGHT);
    VBox root = new VBox(14, heading, hint, scroll, validation, progress, error, actions);
    root.setPadding(new Insets(24));
    VBox.setVgrow(scroll, Priority.ALWAYS);
    Scene scene = new Scene(root, 800, 780);
    UiSupport.style(scene);
    scene
        .getAccelerators()
        .put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), this::save);
    note.addEventFilter(
        javafx.scene.input.KeyEvent.KEY_PRESSED,
        event -> {
          if (event.getCode() == KeyCode.ENTER && !event.isShortcutDown() && !event.isAltDown()) {
            note.replaceSelection("\n");
            event.consume();
          }
        });
    stage.setScene(scene);
    stage.setOnCloseRequest(
        event -> {
          event.consume();
          requestClose();
        });
    if (existingPhoto != null) {
      preview.load(() -> existingPhoto, tasks);
    }
    updatePhotoButtons();
  }

  private static ListCell<ProjectStatus> statusCell() {
    return new ListCell<>() {
      @Override
      protected void updateItem(ProjectStatus value, boolean empty) {
        super.updateItem(value, empty);
        setText(empty || value == null ? null : value.displayName());
      }
    };
  }

  private static VBox field(String text, javafx.scene.control.Control control) {
    Label label = new Label(text);
    label.setLabelFor(control);
    label.getStyleClass().add("field-label");
    return new VBox(6, label, control);
  }

  void show() {
    stage.show();
    Platform.runLater(name::requestFocus);
  }

  boolean isShowing() {
    return stage.isShowing();
  }

  private Input input() {
    return new Input(
        name.getText(),
        material.getText(),
        color.getText(),
        location.getText(),
        note.getText(),
        photo,
        status.getValue(),
        checklist.input());
  }

  private void choosePhoto() {
    if (busy) {
      return;
    }
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Foto auswählen");
    chooser
        .getExtensionFilters()
        .add(
            new FileChooser.ExtensionFilter(
                "JPEG- und PNG-Bilder", "*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"));
    File selected = chooser.showOpenDialog(stage);
    if (selected == null) {
      return;
    }
    setBusy(true, "Foto wird geprüft …");
    error.setVisible(false);
    tasks.submit(
        () -> {
          Photo imported = PhotoImporter.read(selected.toPath());
          return new ImportedPhoto(imported, PhotoPreview.decode(imported));
        },
        imported -> {
          photo = imported.photo();
          preview.showImage(imported.image());
          setBusy(false, "");
        },
        failure -> {
          setBusy(false, "");
          showError(failure);
        });
  }

  private void save() {
    if (busy) {
      return;
    }
    ProjectDraft draft;
    try {
      Input current = input();
      draft =
          new ProjectDraft(
              current.name(),
              current.material(),
              current.color(),
              current.location(),
              current.note(),
              current.photo(),
              current.status(),
              checklist.validatedSteps());
    } catch (IllegalArgumentException exception) {
      validation.setText(exception.getMessage());
      validation.setVisible(true);
      return;
    }
    setBusy(true, "Wird gespeichert …");
    error.setVisible(false);
    validation.setVisible(false);
    tasks.submit(
        () -> service.save(id, draft),
        saved -> {
          setBusy(false, "");
          onSaved.accept(saved);
          stage.hide();
        },
        failure -> {
          setBusy(false, "");
          showError(failure);
        });
  }

  private void showError(Throwable failure) {
    error.setText(UiSupport.message(failure));
    error.setVisible(true);
  }

  private void setBusy(boolean value, String message) {
    busy = value;
    save.setDisable(value);
    cancel.setDisable(value);
    name.setDisable(value);
    material.setDisable(value);
    color.setDisable(value);
    location.setDisable(value);
    note.setDisable(value);
    status.setDisable(value);
    checklist.setDisable(value);
    progress.setText(message);
    progress.setVisible(value);
    updatePhotoButtons();
  }

  private void updatePhotoButtons() {
    choose.setDisable(busy);
    choose.setText(photo == null ? "Foto auswählen …" : "Foto ersetzen …");
    remove.setDisable(busy || photo == null);
  }

  void requestClose() {
    if (busy) {
      return;
    }
    if (input().equals(original)
        || UiSupport.confirm(
            stage,
            "Änderungen verwerfen?",
            "Deine ungespeicherten Texte, Foto-, Status- und Checklistenänderungen gehen beim Verwerfen verloren.",
            "Verwerfen",
            "Weiterbearbeiten")) {
      stage.hide();
    }
  }

  private record Input(
      String name,
      String material,
      String color,
      String location,
      String note,
      Photo photo,
      ProjectStatus status,
      List<ChecklistEditor.StepInput> steps) {}

  private record ImportedPhoto(Photo photo, Image image) {}
}
