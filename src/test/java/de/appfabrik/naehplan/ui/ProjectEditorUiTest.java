package de.appfabrik.naehplan.ui;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.application.PhotoImporter;
import de.appfabrik.naehplan.application.ProjectService;
import de.appfabrik.naehplan.domain.ChecklistStep;
import de.appfabrik.naehplan.domain.Project;
import de.appfabrik.naehplan.domain.ProjectDraft;
import de.appfabrik.naehplan.domain.ProjectStatus;
import de.appfabrik.naehplan.i18n.AppLanguage;
import de.appfabrik.naehplan.i18n.Messages;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

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
    FxTestSupport.start();
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
  void summaryTracksUnsavedStepsWithoutChangingProjectStatus() throws Exception {
    var original = fixture(false);
    open(original);
    fx(
        () -> {
          Label summary = (Label) node("checklist-summary");
          assertEquals("0 von 1 Schritten erledigt", summary.getText());
          ((CheckBox) node("step-completed-" + original.steps().getFirst().id())).fire();
          assertEquals("1 von 1 Schritten erledigt", summary.getText());
          button("add-step").fire();
          assertEquals("1 von 2 Schritten erledigt", summary.getText());
          button("remove-step-" + original.steps().getFirst().id()).fire();
          assertEquals("0 von 1 Schritten erledigt", summary.getText());
          assertEquals(ProjectStatus.FINISHED, ((ComboBox<?>) node("project-status")).getValue());
          return null;
        });
    assertNull(saved.get());
    assertEquals(List.of(original), service.load());
  }

  @ParameterizedTest
  @EnumSource(AppLanguage.class)
  void importedMaximumPositionIsEditableAndExplainedInEveryLanguage(AppLanguage language)
      throws Exception {
    var step = new ChecklistStep(UUID.randomUUID(), "Finish seam", true, Integer.MAX_VALUE);
    var draft =
        new ProjectDraft("Boundary", "", "", "", "", null, ProjectStatus.STARTED, List.of(step));
    var files = new de.appfabrik.naehplan.persistence.JsonCollectionFile();
    Path backup = directory.resolve("boundary.json");
    files.write(backup, List.of(draft));
    var imported =
        new de.appfabrik.naehplan.application.CollectionImport(
                new SqliteProjectRepository(database), files)
            .importFile(backup)
            .projects()
            .getFirst();
    open(imported, language);
    fx(
        () -> {
          assertTrue(button("add-step").isDisabled());
          Label limit = (Label) node("checklist-limit");
          assertTrue(limit.isVisible());
          assertEquals(new Messages(language).text("checklist.positionLimit"), limit.getText());
          assertEquals(
              new Messages(language).text("checklist.summary", 1, 1),
              ((Label) node("checklist-summary")).getText());
          button("add-step").fire();
          assertEquals(1, checklist().input().size());
          field("step-text-" + step.id()).setText("Edited safely");
          button("save-project").fire();
          return null;
        });
    await(() -> saved.get() != null);
    assertEquals(
        new ChecklistStep(step.id(), "Edited safely", true, Integer.MAX_VALUE),
        service.load().getFirst().steps().getFirst());
    open(saved.get(), language);
    fx(
        () -> {
          button("remove-step-" + step.id()).fire();
          assertFalse(button("add-step").isDisabled());
          assertFalse(node("checklist-limit").isVisible());
          button("add-step").fire();
          assertEquals(0, checklist().input().getFirst().position());
          return null;
        });
  }

  @Test
  void enterContinuesChecklistWithoutSavingOrCreatingRepeatedEmptyRows() throws Exception {
    var original = fixture(false);
    open(original);
    await(() -> uncheckedFx(() -> dialog.getOpacity() == 1));
    fx(
        () -> {
          TextField first = field("step-text-" + original.steps().getFirst().id());
          first.requestFocus();
          pressEnter(first);
          assertEquals(2, checklist().input().size());
          TextField second = field("step-text-" + checklist().input().getLast().id());
          pressEnter(second);
          assertEquals(2, checklist().input().size());
          second.setText("Finish seam");
          first.requestFocus();
          pressEnter(first);
          assertSame(second, dialog.getScene().getFocusOwner());
          assertEquals(2, checklist().input().size());
          button("remove-step-" + checklist().input().getLast().id()).fire();
          assertSame(first, dialog.getScene().getFocusOwner());
          button("remove-step-" + checklist().input().getFirst().id()).fire();
          assertSame(button("add-step"), dialog.getScene().getFocusOwner());
          return null;
        });
    assertNull(saved.get());
    assertTrue(fx(dialog::isShowing));
    assertEquals(List.of(original), service.load());
  }

  @ParameterizedTest
  @EnumSource(AppLanguage.class)
  void narrowEditorKeepsChecklistAwayFromScrollbarAndFocusedStepVisible(AppLanguage language)
      throws Exception {
    var steps =
        java.util.stream.IntStream.range(0, 24)
            .mapToObj(i -> new ChecklistStep(UUID.randomUUID(), "Work step " + (i + 1), i < 3, i))
            .toList();
    var project =
        service.save(
            null,
            new ProjectDraft(
                "Checklist layout", "Linen", "", "", "", null, ProjectStatus.PLANNED, steps));
    open(project, language);
    await(() -> uncheckedFx(() -> dialog.getOpacity() == 1));
    fx(
        () -> {
          dialog.setWidth(700);
          dialog.setHeight(560);
          return null;
        });
    await(
        () ->
            uncheckedFx(
                () -> dialog.getScene().getWidth() <= 700 && dialog.getScene().getHeight() <= 560));
    fx(
        () -> {
          field("step-text-" + steps.getLast().id()).requestFocus();
          return null;
        });
    await(() -> uncheckedFx(() -> ((ScrollPane) node("project-editor-scroll")).getVvalue() > 0.5));
    fx(
        () -> {
          ScrollPane scroll = (ScrollPane) node("project-editor-scroll");
          var viewport = scroll.lookup(".viewport");
          var visible = viewport.localToScene(viewport.getLayoutBounds());
          var remove = button("remove-step-" + steps.getLast().id());
          var right = remove.localToScene(remove.getLayoutBounds()).getMaxX();
          assertTrue(
              visible.getMaxX() - right >= 15, "Keep at least 15 pixels before the scrollbar");
          TextField last = field("step-text-" + steps.getLast().id());
          var focused = last.localToScene(last.getLayoutBounds());
          assertTrue(focused.getMinY() >= visible.getMinY() - 1);
          assertTrue(focused.getMaxY() <= visible.getMaxY() + 1);
          capture("checklist-" + language.tag());
          return null;
        });
  }

  @Test
  void repeatedHeaderClicksAndSearchPreserveSortingAndSelectedProject() throws Exception {
    var zulu =
        service.save(
            null,
            new ProjectDraft(
                "Zulu", "Wool", "#006600", "Z shelf", "", null, ProjectStatus.PLANNED, List.of()));
    var alpha =
        service.save(
            null,
            new ProjectDraft(
                "Alpha",
                "Cotton",
                "#FF0000",
                "A shelf",
                "",
                null,
                ProjectStatus.STARTED,
                List.of(new ChecklistStep(UUID.randomUUID(), "Alpha step", false, 0))));
    fx(
        () -> {
          var repository = new SqliteProjectRepository(database);
          new MainWindow(
                  owner,
                  service,
                  new de.appfabrik.naehplan.application.CollectionExport(
                      repository, new de.appfabrik.naehplan.persistence.JsonCollectionFile()),
                  new de.appfabrik.naehplan.application.CollectionImport(
                      repository, new de.appfabrik.naehplan.persistence.JsonCollectionFile()),
                  tasks,
                  new Messages(AppLanguage.ENGLISH),
                  new de.appfabrik.naehplan.persistence.LanguagePreferences(directory),
                  new de.appfabrik.naehplan.persistence.PhotoDirectoryPreferences(directory),
                  "en-US",
                  false)
              .show();
          return null;
        });
    await(
        () -> uncheckedFx(() -> !((Button) owner.getScene().lookup("#add-project")).isDisabled()));
    TableView<?> table = fx(() -> (TableView<?>) owner.getScene().lookup("#projects-table"));
    long[] ascendingFirst = {alpha.id(), zulu.id(), alpha.id(), zulu.id(), alpha.id()};
    var originalColumns = fx(() -> List.copyOf(table.getColumns()));
    for (int index = 0; index < 5; index++) {
      TableColumn<?, ?> column = originalColumns.get(index);
      for (int click = 0; click < 6; click++) {
        boolean descending =
            fx(
                () ->
                    table.getSortOrder().contains(column)
                        && column.getSortType() == TableColumn.SortType.ASCENDING);
        long expectedFirst =
            descending
                ? (ascendingFirst[index] == alpha.id() ? zulu.id() : alpha.id())
                : ascendingFirst[index];
        fx(
            () -> {
              TableColumnHeader header =
                  table.lookupAll(".column-header").stream()
                      .filter(TableColumnHeader.class::isInstance)
                      .map(TableColumnHeader.class::cast)
                      .filter(value -> value.getTableColumn() == column)
                      .findFirst()
                      .orElseThrow();
              var bounds = header.localToScreen(header.getBoundsInLocal());
              var robot = new javafx.scene.robot.Robot();
              robot.mouseMove(bounds.getCenterX(), bounds.getCenterY());
              robot.mouseClick(javafx.scene.input.MouseButton.PRIMARY);
              return null;
            });
        await(
            () -> uncheckedFx(() -> ((Project) table.getItems().getFirst()).id() == expectedFirst));
        fx(
            () -> {
              assertFalse(column.isReorderable());
              assertEquals(2, column.getGraphic().lookupAll(".sort-chevron").size());
              assertEquals(originalColumns, table.getColumns());
              return null;
            });
      }
    }
    fx(
        () -> {
          int selected = table.getItems().indexOf(alpha);
          table.getSelectionModel().select(selected);
          TextField search = (TextField) owner.getScene().lookup("#search-projects");
          search.setText("Alpha");
          search.clear();
          assertEquals(alpha.id(), ((Project) table.getSelectionModel().getSelectedItem()).id());
          assertEquals(List.of(originalColumns.getLast()), table.getSortOrder());
          assertEquals(TableColumn.SortType.DESCENDING, originalColumns.getLast().getSortType());
          ((Button) owner.getScene().lookup("#edit-project")).fire();
          return null;
        });
    await(
        () -> uncheckedFx(() -> Window.getWindows().stream().anyMatch(window -> window != owner)));
    dialog =
        fx(
            () ->
                Window.getWindows().stream()
                    .filter(window -> window != owner)
                    .map(Stage.class::cast)
                    .findFirst()
                    .orElseThrow());
    assertEquals("Alpha", fx(() -> field("project-name").getText()));
    assertEquals("Alpha step", fx(() -> checklist().input().getFirst().text()));
  }

  private static void pressEnter(TextField field) {
    Event.fireEvent(
        field,
        new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
    Event.fireEvent(
        field,
        new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.ENTER, false, false, false, false));
  }

  private void capture(String name) throws Exception {
    var image = dialog.getScene().snapshot(null);
    int width = (int) image.getWidth();
    int height = (int) image.getHeight();
    int[] pixels = new int[width * height];
    image
        .getPixelReader()
        .getPixels(
            0,
            0,
            width,
            height,
            javafx.scene.image.PixelFormat.getIntArgbInstance(),
            pixels,
            0,
            width);
    var output =
        new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    output.setRGB(0, 0, width, height, pixels, 0, width);
    Path folder = Path.of("target", "checklist-review");
    java.nio.file.Files.createDirectories(folder);
    javax.imageio.ImageIO.write(output, "png", folder.resolve(name + ".png").toFile());
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
    PhotoPreview preview =
        fx(
            () ->
                new PhotoPreview(
                    240,
                    180,
                    new de.appfabrik.naehplan.i18n.Messages(
                        de.appfabrik.naehplan.i18n.AppLanguage.GERMAN)));
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
    open(project, AppLanguage.GERMAN);
  }

  private void open(Project project, AppLanguage language) throws Exception {
    var photo = project == null ? null : service.photo(project.id());
    dialog =
        fx(
            () -> {
              var editor =
                  new ProjectEditor(
                      owner,
                      project,
                      photo,
                      service,
                      tasks,
                      saved::set,
                      new Messages(language),
                      new de.appfabrik.naehplan.persistence.PhotoDirectoryPreferences(directory));
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
