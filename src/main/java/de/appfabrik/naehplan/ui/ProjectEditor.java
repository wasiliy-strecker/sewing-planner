package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.application.PhotoImporter;
import de.appfabrik.naehplan.application.ProjectService;
import de.appfabrik.naehplan.domain.Photo;
import de.appfabrik.naehplan.domain.Project;
import de.appfabrik.naehplan.domain.ProjectDraft;
import de.appfabrik.naehplan.domain.ProjectStatus;
import de.appfabrik.naehplan.i18n.Messages;
import de.appfabrik.naehplan.persistence.PhotoDirectoryPreferences;
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
  private final Messages messages;
  private final Stage stage = new Stage();
  private final TextField name = new TextField();
  private final TextField material = new TextField();
  private final ProjectColorField color;
  private final PhotoDirectoryPreferences photoDirectories;
  private final TextField location = new TextField();
  private final ComboBox<ProjectStatus> status = new ComboBox<>();
  private final TextArea note = new TextArea();
  private final Label validation = new Label();
  private final Label error = new Label();
  private final Label progress = new Label();
  private final Button save = new Button();
  private final Button cancel = new Button();
  private final Button choose = new Button();
  private final Button remove = new Button();
  private final PhotoPreview preview;
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
      Consumer<Project> onSaved,
      Messages messages,
      PhotoDirectoryPreferences photoDirectories) {
    this.messages = messages;
    this.photoDirectories = photoDirectories;
    color = new ProjectColorField(existing == null ? "" : existing.color(), messages);
    preview = new PhotoPreview(232, 170, messages);
    preview.setId("editor-photo");
    save.setText(messages.text("action.save"));
    cancel.setText(messages.text("action.cancel"));
    choose.setText(messages.text("photo.choose"));
    remove.setText(messages.text("photo.remove"));
    this.service = service;
    this.tasks = tasks;
    this.onSaved = onSaved;
    id = existing == null ? null : existing.id();
    photo = existingPhoto;
    checklist = new ChecklistEditor(existing == null ? List.of() : existing.steps(), messages);
    status.getItems().setAll(ProjectStatus.values());
    status.setValue(existing == null ? ProjectStatus.PLANNED : existing.status());
    status.setCellFactory(list -> statusCell());
    status.setButtonCell(statusCell());
    status.setMaxWidth(Double.MAX_VALUE);
    status.setId("project-status");
    if (existing != null) {
      name.setText(existing.name());
      material.setText(existing.material());
      location.setText(existing.location());
      note.setText(existing.note());
    }
    original = input();
    stage.initOwner(owner);
    stage.initModality(Modality.WINDOW_MODAL);
    stage.setTitle(existing == null ? messages.text("project.add") : messages.text("project.edit"));
    stage.setMinWidth(660);
    stage.setMinHeight(540);
    Label heading = new Label(stage.getTitle());
    heading.getStyleClass().add("section-heading");
    Label hint = new Label(messages.text("editor.hint"));
    hint.getStyleClass().add("muted");
    hint.setWrapText(true);
    UiSupport.icon(stage);
    for (var field : new javafx.scene.control.TextInputControl[] {name, material, location, note}) {
      UiSupport.textMenu(field, messages);
    }
    validation.setId("name-validation");
    error.setId("save-error");
    cancel.setId("cancel-project");
    name.setId("project-name");
    material.setId("project-material");
    location.setId("project-location");
    note.setId("project-note");
    name.setPromptText(messages.text("editor.namePrompt"));
    material.setPromptText(messages.text("field.optional"));
    location.setPromptText(messages.text("editor.locationPrompt"));
    note.setPromptText(messages.text("editor.notePrompt"));
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
    Label photoHint = new Label(messages.text("photo.hint"));
    photoHint.setWrapText(true);
    photoHint.setMinHeight(Region.USE_PREF_SIZE);
    photoHint.setMaxWidth(232);
    photoHint.getStyleClass().add("muted");
    VBox photoForm =
        new VBox(
            10, new Label(messages.text("photo.optional")), preview, choose, remove, photoHint);
    VBox fields =
        new VBox(
            12,
            field(messages.text("field.requiredName"), name),
            field(messages.text("field.status"), status),
            field(messages.text("field.material"), material),
            field(messages.text("field.color"), color),
            field(messages.text("field.location"), location),
            field(messages.text("field.note"), note));
    fields.setMinWidth(280);
    HBox.setHgrow(fields, Priority.ALWAYS);
    HBox columns = new HBox(24, fields, photoForm);
    columns.setPadding(new Insets(2));
    VBox content = new VBox(22, columns, checklist);
    content.setPadding(new Insets(0, 16, 4, 0));
    ScrollPane scroll = new ScrollPane(content);
    scroll.setId("project-editor-scroll");
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
        .focusOwnerProperty()
        .addListener((observable, previous, current) -> revealFocus(scroll, current));
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
    UiSupport.centerAfterLayout(stage);
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

  private static void revealFocus(ScrollPane scroll, javafx.scene.Node control) {
    if (control == null) {
      return;
    }
    javafx.scene.Node ancestor = control;
    while (ancestor != null && ancestor != scroll.getContent()) {
      ancestor = ancestor.getParent();
    }
    if (ancestor == null) {
      return;
    }
    Platform.runLater(
        () -> {
          if (control.getScene() == null || control.getScene().getFocusOwner() != control) {
            return;
          }
          control.getScene().getRoot().layout();
          var content = scroll.getContent();
          var bounds = content.sceneToLocal(control.localToScene(control.getBoundsInLocal()));
          double viewport = scroll.getViewportBounds().getHeight();
          double range = content.getLayoutBounds().getHeight() - viewport;
          if (range <= 0) {
            return;
          }
          double top = scroll.getVvalue() * range;
          double target = top;
          if (bounds.getMinY() < top) {
            target = bounds.getMinY() - 8;
          } else if (bounds.getMaxY() > top + viewport) {
            target = bounds.getMaxY() + 8 - viewport;
          }
          scroll.setVvalue(Math.max(0, Math.min(1, target / range)));
        });
  }

  private ListCell<ProjectStatus> statusCell() {
    return new ListCell<>() {
      @Override
      protected void updateItem(ProjectStatus value, boolean empty) {
        super.updateItem(value, empty);
        setText(empty || value == null ? null : messages.status(value));
      }
    };
  }

  private static VBox field(String text, javafx.scene.Node control) {
    Label label = new Label(text);
    label.setLabelFor(control);
    label.getStyleClass().add("field-label");
    return new VBox(6, label, control);
  }

  void show() {
    stage.setOpacity(0);
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
        color.value(),
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
    chooser.setTitle(messages.text("photo.chooserTitle"));
    var initialDirectory = photoDirectories.initialDirectory();
    if (initialDirectory != null) {
      chooser.setInitialDirectory(initialDirectory.toFile());
    }
    chooser
        .getExtensionFilters()
        .add(
            new FileChooser.ExtensionFilter(
                messages.text("photo.filter"),
                "*.jpg",
                "*.jpeg",
                "*.png",
                "*.JPG",
                "*.JPEG",
                "*.PNG"));
    File selected;
    try {
      selected = chooser.showOpenDialog(stage);
    } catch (IllegalArgumentException unavailableDirectory) {
      // A removable directory can disappear between validation and the native dialog.
      chooser.setInitialDirectory(null);
      selected = chooser.showOpenDialog(stage);
    }
    if (selected == null) {
      return;
    }
    setBusy(true, messages.text("photo.checking"));
    error.setVisible(false);
    var selectedPath = selected.toPath();
    tasks.submit(
        () -> {
          boolean directorySaved = photoDirectories.remember(selectedPath);
          Photo imported = PhotoImporter.read(selectedPath);
          return new ImportedPhoto(imported, PhotoPreview.decode(imported), directorySaved);
        },
        imported -> {
          photo = imported.photo();
          preview.showImage(imported.image());
          setBusy(false, "");
          if (!imported.directorySaved()) {
            error.setText(messages.text("photo.directoryNotSaved"));
            error.setVisible(true);
          }
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
      validation.setText(
          messages.text(
              name.getText()
                      .codePoints()
                      .noneMatch(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c))
                  ? "validation.name"
                  : "validation.steps"));
      validation.setVisible(true);
      return;
    }
    setBusy(true, messages.text("save.busy"));
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
    error.setText(UiSupport.message(failure, messages));
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
    choose.setText(photo == null ? messages.text("photo.choose") : messages.text("photo.replace"));
    remove.setDisable(busy || photo == null);
  }

  void requestClose() {
    if (busy) {
      return;
    }
    if (input().equals(original)
        || UiSupport.confirm(
            stage,
            messages.text("discard.title"),
            messages.text("discard.detail"),
            messages.text("action.discard"),
            messages.text("action.keep"))) {
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

  private record ImportedPhoto(Photo photo, Image image, boolean directorySaved) {}
}
