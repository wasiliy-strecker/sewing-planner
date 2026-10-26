package de.appfabrik.naehplan.ui;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.application.PhotoImporter;
import de.appfabrik.naehplan.application.ProjectService;
import de.appfabrik.naehplan.domain.ChecklistStep;
import de.appfabrik.naehplan.domain.Project;
import de.appfabrik.naehplan.domain.ProjectDraft;
import de.appfabrik.naehplan.domain.ProjectStatus;
import de.appfabrik.naehplan.persistence.SqliteProjectRepository;
import de.appfabrik.naehplan.support.TestImages;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Real JavaFX controls and SQLite, opt in on a dedicated graphical display. */
@EnabledIfSystemProperty(named = "naehplan.uiTests", matches = "true")
class ProjectEditorUiTest {
  @TempDir Path directory;
  private ProjectService service;
  private Path database;
  private AsyncTasks tasks;
  private Stage owner;
  private Stage dialog;
  private final AtomicReference<Project> saved = new AtomicReference<>();

  @BeforeAll
  static void startToolkit() throws Exception {
    var ready = new CountDownLatch(1);
    Platform.startup(
        () -> {
          Platform.setImplicitExit(false);
          ready.countDown();
        });
    assertTrue(ready.await(10, TimeUnit.SECONDS));
  }

  @AfterAll
  static void stopToolkit() {
    Platform.exit();
  }

  @BeforeEach
  void setUp() throws Exception {
    database = directory.resolve("naehplan.db");
    service = new ProjectService(new SqliteProjectRepository(database));
    service.load();
    tasks = new AsyncTasks();
    owner =
        fx(
            () -> {
              Stage stage = new Stage();
              stage.setScene(new Scene(new VBox(), 900, 800));
              stage.show();
              return stage;
            });
  }

  @AfterEach
  void closeWindows() throws Exception {
    fx(
        () -> {
          for (Window window : List.copyOf(Window.getWindows())) {
            window.hide();
          }
          return null;
        });
    tasks.close();
  }

  @Test
  void newProjectStartsPlannedAndEmptyAndCanSaveWithoutSteps() throws Exception {
    open(null);
    fx(
        () -> {
          assertEquals(ProjectStatus.PLANNED, ((ComboBox<?>) node("project-status")).getValue());
          assertTrue(checklist().input().isEmpty());
          field("project-name").setText("Tasche");
          button("save-project").fire();
          return null;
        });
    await(() -> saved.get() != null);
    assertEquals(ProjectStatus.PLANNED, saved.get().status());
    assertTrue(saved.get().steps().isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"complete,cancel", "reopen,escape", "rename,window", "add,cancel", "remove,window"})
  void checklistOnlyChangesAreProtectedAndDiscardKeepsSavedState(String mutation, String close)
      throws Exception {
    var original = fixture(mutation.equals("reopen"));
    open(original);
    fx(
        () -> {
          switch (mutation) {
            case "complete", "reopen" ->
                ((CheckBox) node("step-completed-" + original.steps().getFirst().id())).fire();
            case "rename" ->
                field("step-text-" + original.steps().getFirst().id()).setText("Saum nähen");
            case "add" -> button("add-step").fire();
            case "remove" -> button("remove-step-" + original.steps().getFirst().id()).fire();
            default -> throw new AssertionError(mutation);
          }
          return null;
        });
    var changed = fx(() -> checklist().input());
    requestClose(close);
    awaitAlert();
    fx(
        () -> {
          var keep = alertButton("Weiterbearbeiten");
          assertTrue(keep.isDefaultButton());
          assertFalse(alertButton("Verwerfen").isDefaultButton());
          keep.fire();
          return null;
        });
    assertTrue(fx(dialog::isShowing));
    assertEquals(changed, fx(() -> checklist().input()));
    requestClose(close);
    awaitAlert();
    fx(
        () -> {
          alertButton("Verwerfen").fire();
          return null;
        });
    await(() -> !uncheckedFx(dialog::isShowing));
    assertNull(saved.get());
    assertEquals(List.of(original), service.load());
  }

  @Test
  void invalidStepTextPreservesWholeDraftAndCanBeCorrectedBeforeSaving() throws Exception {
    var original = fixture(false);
    open(original);
    fx(
        () -> {
          field("project-name").setText("Tasche geändert");
          button("add-step").fire();
          var input = checklist().input();
          assertFalse(input.getLast().completed());
          assertEquals(1, input.getLast().position());
          field("step-text-" + input.getLast().id()).setText("\u00a0 \t");
          button("save-project").fire();
          assertTrue(dialog.isShowing());
          assertTrue(
              dialog.getScene().getRoot().lookupAll(".label").stream()
                  .map(Label.class::cast)
                  .anyMatch(
                      label ->
                          label.isVisible() && label.getText().contains("jeden Arbeitsschritt")));
          assertEquals("Tasche geändert", field("project-name").getText());
          return null;
        });
    assertEquals(List.of(original), service.load());
    fx(
        () -> {
          field("step-text-" + checklist().input().getLast().id()).setText("Nähen");
          button("save-project").fire();
          return null;
        });
    await(() -> saved.get() != null);
    assertEquals(2, service.load().getFirst().steps().size());
    assertEquals(
        original.steps().getFirst().id(), service.load().getFirst().steps().getFirst().id());
  }

  @Test
  void lateDatabaseFailureKeepsDraftAndPhotoForRetry() throws Exception {
    var original = fixture(false);
    open(original);
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute(
          "CREATE TRIGGER fail_step AFTER INSERT ON checklist_steps BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END");
      fx(
          () -> {
            field("project-name").setText("Geändert");
            ((CheckBox) node("step-completed-" + original.steps().getFirst().id())).fire();
            button("save-project").fire();
            return null;
          });
      await(() -> uncheckedFx(() -> !button("save-project").isDisabled()));
      assertNull(saved.get());
      assertEquals(List.of(original), service.load());
      assertTrue(fx(dialog::isShowing));
      assertTrue(fx(() -> checklist().input().getFirst().completed()));
      assertEquals("Geändert", fx(() -> field("project-name").getText()));
      statement.execute("DROP TRIGGER fail_step");
      fx(
          () -> {
            button("save-project").fire();
            return null;
          });
      await(() -> saved.get() != null);
      assertEquals("Geändert", service.load().getFirst().name());
      assertTrue(service.load().getFirst().steps().getFirst().completed());
      assertNotNull(service.photo(original.id()));
    }
  }

  @Test
  void unchangedDialogClosesWithoutDiscardPrompt() throws Exception {
    open(fixture(false));
    fx(
        () -> {
          button("cancel-project").fire();
          return null;
        });
    assertFalse(fx(dialog::isShowing));
  }

  @Test
  void stalePhotoCallbackCannotReplaceNewSelection() throws Exception {
    var photo = PhotoImporter.decode(TestImages.image("png", 60, 40, 0xff00aa));
    var release = new CountDownLatch(1);
    var done = new CountDownLatch(1);
    PhotoPreview preview = fx(() -> new PhotoPreview(240, 180));
    try {
      fx(
          () -> {
            preview.load(
                () -> {
                  if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out");
                  }
                  return photo;
                },
                tasks);
            preview.showImage(null);
            tasks.submit(() -> true, result -> done.countDown(), error -> done.countDown());
            return null;
          });
    } finally {
      release.countDown();
    }
    assertTrue(done.await(10, TimeUnit.SECONDS));
    assertNull(fx(() -> ((ImageView) preview.getChildren().getFirst()).getImage()));
  }

  private Project fixture(boolean completed) throws Exception {
    var photo = PhotoImporter.decode(TestImages.image("png", 60, 40, 0x74536f));
    return service.save(
        null,
        new ProjectDraft(
            "Tasche",
            "Leinen",
            "Grün",
            "Box",
            "Notiz",
            photo,
            ProjectStatus.FINISHED,
            List.of(new ChecklistStep(UUID.randomUUID(), "Nähen", completed, 0))));
  }

  private void open(Project project) throws Exception {
    var photo = project == null ? null : service.photo(project.id());
    dialog =
        fx(
            () -> {
              var editor = new ProjectEditor(owner, project, photo, service, tasks, saved::set);
              editor.show();
              return Window.getWindows().stream()
                  .filter(window -> window != owner)
                  .map(Stage.class::cast)
                  .findFirst()
                  .orElseThrow();
            });
  }

  private void requestClose(String mode) {
    Platform.runLater(
        () -> {
          switch (mode) {
            case "cancel" -> button("cancel-project").fire();
            case "escape" ->
                Event.fireEvent(
                    dialog.getScene().getRoot(),
                    new KeyEvent(
                        KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
            case "window" ->
                Event.fireEvent(dialog, new WindowEvent(dialog, WindowEvent.WINDOW_CLOSE_REQUEST));
            default -> throw new AssertionError(mode);
          }
        });
  }

  private void awaitAlert() throws Exception {
    await(
        () ->
            uncheckedFx(
                () ->
                    Window.getWindows().stream()
                        .anyMatch(window -> window != owner && window != dialog)));
  }

  private Button alertButton(String text) {
    return Window.getWindows().stream()
        .filter(window -> window != owner && window != dialog)
        .flatMap(window -> window.getScene().getRoot().lookupAll(".button").stream())
        .map(Button.class::cast)
        .filter(button -> button.getText().equals(text))
        .findFirst()
        .orElseThrow();
  }

  private Node node(String id) {
    return dialog.getScene().lookup("#" + id);
  }

  private Button button(String id) {
    return (Button) node(id);
  }

  private TextField field(String id) {
    return (TextField) node(id);
  }

  private ChecklistEditor checklist() {
    return (ChecklistEditor) node("checklist-editor");
  }

  private static <T> T fx(Callable<T> action) throws Exception {
    var task = new FutureTask<>(action);
    Platform.runLater(task);
    return task.get(10, TimeUnit.SECONDS);
  }

  private static <T> T uncheckedFx(Callable<T> action) {
    try {
      return fx(action);
    } catch (Exception exception) {
      throw new AssertionError(exception);
    }
  }

  private static void await(BooleanSupplier condition) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) {
        fail("Timed out waiting for JavaFX");
      }
      Thread.sleep(20);
    }
  }
}
