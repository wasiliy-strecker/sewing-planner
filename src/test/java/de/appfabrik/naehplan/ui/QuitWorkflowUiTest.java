package de.appfabrik.naehplan.ui;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.application.*;
import de.appfabrik.naehplan.i18n.AppLanguage;
import de.appfabrik.naehplan.i18n.Messages;
import de.appfabrik.naehplan.persistence.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the same Glass quit callback that macOS invokes for Cmd+Q. */
@EnabledIfSystemProperty(named = "naehplan.uiTests", matches = "true")
class QuitWorkflowUiTest {
  @TempDir Path directory;
  private Stage main;
  private AsyncTasks tasks;
  private SqliteProjectRepository repository;
  private final AtomicInteger prompts = new AtomicInteger();
  private final ListChangeListener<Window> observer =
      change -> {
        while (change.next()) {
          for (Window window : change.getAddedSubList()) {
            if (window.getScene().getRoot() instanceof DialogPane) {
              prompts.incrementAndGet();
            }
          }
        }
      };

  @BeforeAll
  static void startToolkit() throws Exception {
    FxTestSupport.start();
  }

  @BeforeEach
  void setUp() throws Exception {
    repository = new SqliteProjectRepository(directory.resolve("synthetic.db"));
    var service = new ProjectService(repository);
    service.load();
    tasks = new AsyncTasks();
    main =
        FxTestSupport.fx(
            () -> {
              var stage = new Stage();
              new MainWindow(
                      stage,
                      service,
                      new CollectionExport(repository, new JsonCollectionFile()),
                      new CollectionImport(repository, new JsonCollectionFile()),
                      tasks,
                      new Messages(AppLanguage.ENGLISH),
                      new LanguagePreferences(directory),
                      new PhotoDirectoryPreferences(directory),
                      AppLanguage.ENGLISH.tag(),
                      false)
                  .show();
              Window.getWindows().addListener(observer);
              return stage;
            });
    waitFor(() -> node("#add-project") instanceof Button button && !button.isDisabled());
  }

  @AfterEach
  void tearDown() throws Exception {
    FxTestSupport.fx(
        () -> {
          Window.getWindows().removeListener(observer);
          for (Window window : List.copyOf(Window.getWindows())) {
            window.hide();
          }
          return null;
        });
    tasks.close();
  }

  @Test
  void quitAsksOnceAndKeepPreservesTheEntireDraft() throws Exception {
    openEditor(true);
    quit();
    answer(false);
    assertEquals(1, prompts.get());
    assertTrue(FxTestSupport.fx(main::isShowing));
    assertEquals(
        "Unsaved synthetic draft",
        FxTestSupport.fx(() -> ((TextField) node("#project-name")).getText()));
    assertEquals(0, repository.findAll().size());
    // A cancelled quit must not leave behind a callback that later closes the main window.
    FxTestSupport.fx(
        () -> {
          ((Button) node("#cancel-project")).fire();
          return null;
        });
    answer(true);
    waitFor(() -> node("#project-name") == null);
    assertTrue(FxTestSupport.fx(main::isShowing));
    assertEquals(2, prompts.get());
  }

  @Test
  void discardOnQuitClosesBothWindowsWithoutSaving() throws Exception {
    openEditor(true);
    quit();
    answer(true);
    waitFor(() -> !main.isShowing());
    assertEquals(1, prompts.get());
    assertEquals(0, repository.findAll().size());
  }

  @Test
  void cleanEditorAndMainCloseWithoutAQuestion() throws Exception {
    openEditor(false);
    quit();
    waitFor(() -> !main.isShowing());
    assertEquals(0, prompts.get());
  }

  @Test
  void closingOnlyTheEditorKeepsMainOpen() throws Exception {
    openEditor(true);
    FxTestSupport.fx(
        () -> {
          Window editor = node("#project-name").getScene().getWindow();
          editor.fireEvent(new WindowEvent(editor, WindowEvent.WINDOW_CLOSE_REQUEST));
          return null;
        });
    answer(true);
    waitFor(() -> node("#project-name") == null);
    assertTrue(FxTestSupport.fx(main::isShowing));
    assertEquals(1, prompts.get());
  }

  @Test
  void repeatedQuitRequestsShareOneConfirmation() throws Exception {
    openEditor(true);
    FxTestSupport.fx(
        () -> {
          nativeQuit();
          nativeQuit();
          return null;
        });
    answer(false);
    assertEquals(1, prompts.get());
    assertTrue(FxTestSupport.fx(main::isShowing));
    assertNotNull(FxTestSupport.fx(() -> node("#project-name")));
  }

  @Test
  void quitWaitsForAnInFlightSaveAndPreservesTheSavedRecord() throws Exception {
    openEditor(true);
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    tasks.submit(
        () -> {
          started.countDown();
          if (!release.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Test worker not released");
          }
          return null;
        },
        ignored -> {},
        failure -> {});
    assertTrue(started.await(10, TimeUnit.SECONDS));
    try {
      FxTestSupport.fx(
          () -> {
            ((Button) node("#save-project")).fire();
            return null;
          });
      quit();
      assertTrue(FxTestSupport.fx(main::isShowing));
      assertNotNull(FxTestSupport.fx(() -> node("#project-name")));
      assertEquals(0, prompts.get());
    } finally {
      release.countDown();
    }
    waitFor(() -> node("#project-name") == null);
    assertTrue(FxTestSupport.fx(main::isShowing));
    assertEquals(1, repository.findAll().size());
    assertEquals("Unsaved synthetic draft", repository.findAll().getFirst().name());
    quit();
    waitFor(() -> !main.isShowing());
  }

  private void openEditor(boolean dirty) throws Exception {
    FxTestSupport.fx(
        () -> {
          ((Button) node("#add-project")).fire();
          return null;
        });
    waitFor(() -> node("#project-name") != null);
    if (dirty) {
      FxTestSupport.fx(
          () -> {
            ((TextField) node("#project-name")).setText("Unsaved synthetic draft");
            return null;
          });
    }
  }

  private static void nativeQuit() {
    var application = com.sun.glass.ui.Application.GetApplication();
    application.getEventHandler().handleQuitAction(application, System.nanoTime());
  }

  private static void quit() throws Exception {
    FxTestSupport.fx(
        () -> {
          nativeQuit();
          return null;
        });
  }

  private static void answer(boolean discard) throws Exception {
    waitFor(() -> confirmation() != null);
    FxTestSupport.fx(
        () -> {
          var pane = confirmation();
          var data = discard ? ButtonBar.ButtonData.OK_DONE : ButtonBar.ButtonData.CANCEL_CLOSE;
          var type =
              pane.getButtonTypes().stream()
                  .filter(value -> value.getButtonData() == data)
                  .findFirst()
                  .orElseThrow();
          ((Button) pane.lookupButton(type)).fire();
          return null;
        });
    waitFor(() -> confirmation() == null);
    FxTestSupport.fx(() -> null);
  }

  private static DialogPane confirmation() {
    for (Window window : Window.getWindows()) {
      if (window.getScene().getRoot() instanceof DialogPane pane) {
        return pane;
      }
    }
    return null;
  }

  private static Node node(String selector) {
    for (Window window : Window.getWindows()) {
      Node found = window.getScene().lookup(selector);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  private static void waitFor(Callable<Boolean> condition) throws Exception {
    FxTestSupport.await(() -> FxTestSupport.uncheckedFx(condition));
  }
}
