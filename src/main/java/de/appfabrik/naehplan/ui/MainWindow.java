package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.application.ProjectService;
import de.appfabrik.naehplan.domain.Project;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

final class MainWindow {
  private final Stage stage;
  private final ProjectService service;
  private final AsyncTasks tasks;
  private final TableView<Project> table = new TableView<>();
  private final TextField search = new TextField();
  private final Label count = new Label();
  private final Label emptyTitle = new Label();
  private final Label emptyHint = new Label();
  private final Label status = new Label();
  private final TextArea note = new TextArea();
  private final VBox checklist = new VBox(6);
  private final Label checklistHint = new Label();
  private final PhotoPreview preview = new PhotoPreview(240, 180);
  private final Button add = new Button("+  Projekt hinzufügen");
  private final Button edit = new Button("Bearbeiten");
  private final Button delete = new Button("Löschen");
  private final Button reload = new Button("Neu laden");
  private final List<Project> projects = new ArrayList<>();
  private boolean busy;
  private boolean ready;
  private boolean loadFailed;
  private ProjectEditor editor;

  MainWindow(Stage stage, ProjectService service, AsyncTasks tasks) {
    this.stage = stage;
    this.service = service;
    this.tasks = tasks;
    Label heading = new Label("NähPlan");
    heading.getStyleClass().add("app-heading");
    Label subname = new Label("Deine Ideen. Stich für Stich.");
    subname.getStyleClass().add("muted");
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    add.setId("add-project");
    add.getStyleClass().add("primary");
    add.setOnAction(event -> openEditor(null));
    HBox header = new HBox(20, new VBox(4, heading, subname), spacer, add);
    header.setAlignment(Pos.CENTER_LEFT);
    search.setId("search-projects");
    search.setPromptText("Projekttexte und Arbeitsschritte durchsuchen");
    search.setAccessibleText("Projekte durchsuchen");
    search.textProperty().addListener((observable, previous, current) -> refresh(null));
    HBox.setHgrow(search, Priority.ALWAYS);
    Button clear = new Button("Suche leeren");
    clear.setOnAction(
        event -> {
          search.clear();
          search.requestFocus();
        });
    clear.disableProperty().bind(search.textProperty().isEmpty());
    HBox searchBar = new HBox(10, search, clear);
    table.setId("projects-table");
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.getColumns().add(column("Projektname", Project::name, 300));
    TableColumn<Project, String> projectStatus =
        column("Status", project -> project.status().displayName(), 130);
    projectStatus.setMinWidth(125);
    table.getColumns().add(projectStatus);
    table.getColumns().add(column("Material", Project::material, 190));
    table.getColumns().add(column("Farbe", Project::color, 170));
    table.getColumns().add(column("Aufbewahrungsort", Project::location, 240));
    table
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, previous, selected) -> {
              note.setText(selected == null ? "" : selected.note());
              showChecklist(selected);
              note.setPromptText(
                  selected == null
                      ? "Wähle ein Projekt aus, um seine Notiz zu lesen."
                      : "Für dieses Projekt ist keine Notiz hinterlegt.");
              if (selected == null || !selected.hasPhoto()) {
                preview.showImage(null);
              } else {
                preview.load(() -> service.photo(selected.id()), tasks);
              }
              updateButtons();
            });
    table.setOnMouseClicked(
        event -> {
          if (event.getClickCount() == 2 && event.getTarget() instanceof javafx.scene.Node node) {
            while (node != null && !(node instanceof javafx.scene.control.TableRow<?>)) {
              node = node.getParent();
            }
            if (node instanceof javafx.scene.control.TableRow<?> row && !row.isEmpty()) {
              editSelected();
            }
          }
        });
    table.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER) {
            editSelected();
            event.consume();
          }
          if (event.getCode() == KeyCode.DELETE) {
            deleteSelected();
            event.consume();
          }
        });
    emptyTitle.getStyleClass().add("section-heading");
    emptyHint.getStyleClass().add("muted");
    emptyHint.setWrapText(true);
    emptyHint.setMaxWidth(470);
    VBox placeholder = new VBox(12, emptyTitle, emptyHint);
    placeholder.setAlignment(Pos.CENTER);
    placeholder.setPadding(new Insets(30));
    table.setPlaceholder(placeholder);
    count.getStyleClass().add("muted");
    edit.setOnAction(event -> editSelected());
    delete.setOnAction(event -> deleteSelected());
    delete.getStyleClass().add("danger");
    Region actionsSpacer = new Region();
    HBox.setHgrow(actionsSpacer, Priority.ALWAYS);
    HBox actions = new HBox(10, count, actionsSpacer, edit, delete);
    actions.setAlignment(Pos.CENTER_LEFT);
    Label noteHeading = new Label("Notiz");
    noteHeading.getStyleClass().add("field-label");
    note.setEditable(false);
    note.setWrapText(true);
    note.setPrefRowCount(2);
    note.setPromptText("Wähle ein Projekt aus, um seine Notiz zu lesen.");
    note.setAccessibleText("Notiz des ausgewählten Projekts");
    status.setWrapText(true);
    status.setMinHeight(Region.USE_PREF_SIZE);
    status.getStyleClass().add("muted");
    status.setMaxWidth(Double.MAX_VALUE);
    status.setMinWidth(0);
    HBox.setHgrow(status, Priority.ALWAYS);
    reload.setOnAction(event -> load());
    reload.setPrefWidth(120);
    reload.setMinWidth(Region.USE_PREF_SIZE);
    HBox footer = new HBox(16, status, reload);
    footer.setAlignment(Pos.CENTER_LEFT);
    Label checklistHeading = new Label("Arbeitsschritte");
    checklistHeading.getStyleClass().add("field-label");
    checklistHint.getStyleClass().add("muted");
    checklistHint.setWrapText(true);
    checklist.setId("saved-checklist");
    ScrollPane checklistScroll = new ScrollPane(checklist);
    checklistScroll.setFitToWidth(true);
    checklistScroll.setPrefHeight(110);
    checklistScroll.setMinHeight(70);
    showChecklist(null);
    VBox details = new VBox(6, noteHeading, note, checklistHeading, checklistHint, checklistScroll);
    HBox.setHgrow(details, Priority.ALWAYS);
    VBox root =
        new VBox(18, header, searchBar, table, actions, new HBox(22, preview, details), footer);
    root.setPadding(new Insets(30));
    VBox.setVgrow(table, Priority.ALWAYS);
    Scene scene = new Scene(root, 1080, 800);
    UiSupport.style(scene);
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
            () -> openEditor(null));
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
            () -> {
              search.requestFocus();
              search.selectAll();
            });
    stage.setTitle("NähPlan");
    stage.setMinWidth(860);
    stage.setMinHeight(700);
    stage.setScene(scene);
    stage.setOnCloseRequest(
        event -> {
          if (busy) {
            event.consume();
          } else if (editor != null && editor.isShowing()) {
            event.consume();
            editor.requestClose();
          }
        });
  }

  private void showChecklist(Project project) {
    checklist.getChildren().clear();
    checklistHint.setText(
        project == null
            ? "Wähle ein Projekt aus, um seine Checkliste zu sehen."
            : project.steps().isEmpty()
                ? "Noch keine Arbeitsschritte. Ergänze sie über „Bearbeiten“."
                : "Gespeicherter Stand. Änderungen über „Bearbeiten“.");
    if (project != null) {
      for (var step : project.steps()) {
        Label label = new Label((step.completed() ? "✓  " : "○  ") + step.text());
        label.setAccessibleText((step.completed() ? "Erledigt: " : "Offen: ") + step.text());
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.getStyleClass().add(step.completed() ? "completed-step" : "open-step");
        checklist.getChildren().add(label);
      }
    }
  }

  private static TableColumn<Project, String> column(
      String name, Function<Project, String> value, int width) {
    TableColumn<Project, String> column = new TableColumn<>(name);
    column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
    column.setPrefWidth(width);
    column.setSortable(false);
    return column;
  }

  void show() {
    stage.show();
    load();
  }

  private void load() {
    if (busy) {
      return;
    }
    setBusy(true);
    loadFailed = false;
    showStatus("Sammlung wird geöffnet …", false);
    refresh(null);
    tasks.submit(
        service::load,
        loaded -> {
          projects.clear();
          projects.addAll(loaded);
          ready = true;
          setBusy(false);
          refresh(null);
          showStatus("Lokal auf diesem Computer gespeichert", false);
        },
        failure -> {
          loadFailed = true;
          setBusy(false);
          refresh(null);
          showStatus(UiSupport.message(failure), true);
        });
  }

  private void refresh(Long preferredId) {
    Project selected = table.getSelectionModel().getSelectedItem();
    Long selection = preferredId != null ? preferredId : selected == null ? null : selected.id();
    List<Project> matches = ProjectService.search(projects, search.getText());
    table.getItems().setAll(matches);
    if (selection != null) {
      matches.stream()
          .filter(project -> project.id() == selection)
          .findFirst()
          .ifPresent(
              project -> {
                table.getSelectionModel().select(project);
                if (preferredId != null) {
                  table.scrollTo(project);
                }
              });
    }
    count.setText(
        !ready
            ? ""
            : search.getText().isBlank()
                ? projects.size() + (projects.size() == 1 ? " Projekt" : " Projekte")
                : matches.size() + " von " + projects.size() + " Projekten");
    if (loadFailed) {
      emptyTitle.setText("Sammlung nicht geladen");
      emptyHint.setText("Bitte beachte den Hinweis unten und versuche es mit „Neu laden“ erneut.");
    } else if (!ready) {
      emptyTitle.setText("Deine Sammlung wird geöffnet …");
      emptyHint.setText("");
    } else if (projects.isEmpty()) {
      emptyTitle.setText("Platz für dein erstes Projekt");
      emptyHint.setText(
          "Mit „Projekt hinzufügen“ legst du deine Sammlung an. Ein Projektname genügt für den Anfang.");
    } else {
      emptyTitle.setText("Keine Projekte gefunden");
      emptyHint.setText(
          "Versuche einen anderen Suchbegriff oder leere die Suche, um alle Projekte zu sehen.");
    }
    updateButtons();
  }

  private void openEditor(Project project) {
    if (!ready || busy || loadFailed || (editor != null && editor.isShowing())) {
      return;
    }
    setBusy(true);
    tasks.submit(
        () -> project == null ? null : service.photo(project.id()),
        photo -> {
          setBusy(false);
          editor =
              new ProjectEditor(
                  stage,
                  project,
                  photo,
                  service,
                  tasks,
                  saved -> {
                    projects.removeIf(existing -> existing.id() == saved.id());
                    projects.add(saved);
                    refresh(saved.id());
                    showStatus(
                        ProjectService.search(List.of(saved), search.getText()).isEmpty()
                            ? "Projekt gespeichert. Es passt nicht zur aktuellen Suche."
                            : "Lokal auf diesem Computer gespeichert",
                        false);
                  });
          editor.show();
        },
        failure -> {
          setBusy(false);
          showStatus(UiSupport.message(failure), true);
        });
  }

  private void editSelected() {
    Project selected = table.getSelectionModel().getSelectedItem();
    if (selected != null) {
      openEditor(selected);
    }
  }

  private void deleteSelected() {
    Project selected = table.getSelectionModel().getSelectedItem();
    if (selected == null || busy || loadFailed) {
      return;
    }
    if (!UiSupport.confirm(
        stage,
        "Projekt löschen?",
        "„" + selected.name() + "“ wird dauerhaft aus deiner Sammlung entfernt.",
        "Löschen",
        "Abbrechen")) {
      return;
    }
    setBusy(true);
    showStatus("Projekt wird gelöscht …", false);
    tasks.submit(
        () -> {
          service.delete(selected.id());
          return selected;
        },
        deleted -> {
          projects.removeIf(project -> project.id() == deleted.id());
          setBusy(false);
          refresh(null);
          showStatus("Lokal auf diesem Computer gespeichert", false);
        },
        failure -> {
          setBusy(false);
          showStatus(UiSupport.message(failure), true);
        });
  }

  private void setBusy(boolean value) {
    busy = value;
    table.setDisable(value);
    reload.setDisable(value);
    updateButtons();
  }

  private void updateButtons() {
    add.setDisable(busy || !ready || loadFailed);
    boolean unavailable = busy || loadFailed || table.getSelectionModel().getSelectedItem() == null;
    edit.setDisable(unavailable);
    delete.setDisable(unavailable);
  }

  private void showStatus(String text, boolean error) {
    status.setText(text);
    status.getStyleClass().removeAll("muted", "error-text");
    status.getStyleClass().add(error ? "error-text" : "muted");
  }
}
