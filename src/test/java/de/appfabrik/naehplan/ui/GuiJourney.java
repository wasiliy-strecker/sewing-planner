package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.domain.ChecklistStep;
import de.appfabrik.naehplan.domain.Photo;
import de.appfabrik.naehplan.domain.ProjectDraft;
import de.appfabrik.naehplan.domain.ProjectStatus;
import de.appfabrik.naehplan.i18n.AppLanguage;
import de.appfabrik.naehplan.i18n.Messages;
import de.appfabrik.naehplan.persistence.LanguagePreferences;
import de.appfabrik.naehplan.persistence.SqliteProjectRepository;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;

/** Drives the production application with actual mouse/keyboard events on an isolated display. */
public final class GuiJourney {
  private static final List<String> checks = new ArrayList<>();
  private static String failure;

  private GuiJourney() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      throw new IllegalArgumentException("data-dir output-dir locale mode");
    }
    Path data = Path.of(args[0]).toAbsolutePath();
    Path output = Path.of(args[1]).toAbsolutePath();
    String tag = args[2];
    String mode = args[3];
    if (!Files.isRegularFile(data.resolve(".synthetic-sewingplanner-demo"))) {
      throw new IllegalArgumentException(
          "An isolated, explicitly marked synthetic data directory is required");
    }
    Files.createDirectories(output);
    AppLanguage language = AppLanguage.resolve(tag, Locale.US);
    Messages messages = new Messages(language);
    Properties demo = new Properties();
    try (var reader =
        Files.newBufferedReader(
            Path.of("assets/store_demo", tag + ".properties"), StandardCharsets.UTF_8)) {
      demo.load(reader);
    }
    if (!mode.equals("restart")) {
      if (Files.exists(data.resolve("naehplan.db"))) {
        throw new IllegalArgumentException("Fresh demo directory required");
      }
      var repository = new SqliteProjectRepository(data.resolve("naehplan.db"));
      repository.initialize();
      for (int i = 1; i <= 5; i++) {
        repository.create(
            new ProjectDraft(
                demo.getProperty("project." + i + ".name"),
                demo.getProperty("project." + i + ".material"),
                demo.getProperty("project." + i + ".color"),
                demo.getProperty("project." + i + ".location"),
                demo.getProperty("project." + i + ".note"),
                demo.getProperty("project." + i + ".photo", "").isEmpty()
                    ? null
                    : new Photo(
                        Files.readAllBytes(Path.of(demo.getProperty("project." + i + ".photo")))),
                ProjectStatus.fromStoredValue(demo.getProperty("project." + i + ".status")),
                List.of(
                    new ChecklistStep(
                        UUID.nameUUIDFromBytes(
                            ("demo-" + i + "-0").getBytes(StandardCharsets.UTF_8)),
                        demo.getProperty("step.1"),
                        true,
                        0),
                    new ChecklistStep(
                        UUID.nameUUIDFromBytes(
                            ("demo-" + i + "-1").getBytes(StandardCharsets.UTF_8)),
                        demo.getProperty("step.2"),
                        false,
                        2),
                    new ChecklistStep(
                        UUID.nameUUIDFromBytes(
                            ("demo-" + i + "-2").getBytes(StandardCharsets.UTF_8)),
                        demo.getProperty("step.3"),
                        false,
                        3))));
      }
      new LanguagePreferences(data).save(tag);
    }
    // A deliberately different system language proves the saved preference wins at real startup.
    Locale.setDefault(Locale.JAPAN);
    Thread driver =
        new Thread(
            () -> {
              try {
                waitFor(
                    () ->
                        find("project-count") instanceof Label label && !label.getText().isEmpty());
                check(
                    fx(() -> ((Button) find("add-project")).getText())
                        .equals("+  " + messages.text("project.add")),
                    "saved language applied at startup");
                if (mode.equals("capture")) {
                  captureJourney(output, messages, demo);
                } else if (mode.equals("verify")) {
                  verifyJourney(data, output, messages);
                } else if (mode.equals("restart")) {
                  check(
                      new LanguagePreferences(data).load().equals(tag),
                      "preference survived process restart");
                  var projects = new SqliteProjectRepository(data.resolve("naehplan.db")).findAll();
                  check(
                      projects.stream()
                          .anyMatch(
                              project ->
                                  project.name().equals("L'été d'Álvaro – ação – Bücher")
                                      && project.note().contains("Line 30")),
                      "Unicode and multiline draft survived restart");
                  var saved =
                      projects.stream()
                          .filter(
                              project -> project.name().equals("L'été d'Álvaro – ação – Bücher"))
                          .findFirst()
                          .orElseThrow();
                  check(
                      saved
                          .steps()
                          .toString()
                          .equals(Files.readString(data.resolve("expected-steps.txt"))),
                      "step IDs, text, completion and positions survive language restart");
                  check(
                      saved.status() == ProjectStatus.STARTED,
                      "status code survives language change and process restart");
                  check(
                      Arrays.equals(
                          new SqliteProjectRepository(data.resolve("naehplan.db"))
                              .findPhoto(saved.id())
                              .bytes(),
                          Files.readAllBytes(Path.of("assets/store_demo/images/sage.png"))),
                      "photo bytes survive language change and restart");
                  enter("search-projects", "ÁLVARO");
                  waitFor(() -> ((TableView<?>) find("projects-table")).getItems().size() == 1);
                  clickTableFirstRow();
                  waitFor(() -> photoVisible("selected-photo"));
                  screenshot(output.resolve("restart.png"));
                } else {
                  throw new IllegalArgumentException("Unknown mode");
                }
                Files.write(output.resolve("checks.txt"), checks, StandardCharsets.UTF_8);
              } catch (Exception | AssertionError exception) {
                failure = exception.toString();
                exception.printStackTrace();
                try {
                  screenshot(output.resolve("failure.png"));
                } catch (Exception ignored) {
                  /* Toolkit may be unavailable. */
                }
              } finally {
                Platform.runLater(
                    () -> {
                      for (Window window : List.copyOf(Window.getWindows())) {
                        window.hide();
                      }
                      Platform.exit();
                    });
              }
            },
            "sewingplanner-gui-driver");
    driver.setDaemon(true);
    driver.start();
    Application.launch(ProjectsApplication.class, "--data-dir=" + data);
    driver.join(10000);
    if (failure != null) {
      throw new IllegalStateException(failure);
    }
    if (driver.isAlive()) {
      throw new IllegalStateException("GUI driver did not finish");
    }
    System.out.println("GUI " + mode + " passed: " + tag + " (" + checks.size() + " checks)");
  }

  private static void captureJourney(Path output, Messages messages, Properties demo)
      throws Exception {
    fx(
        () -> {
          Stage main = (Stage) find("add-project").getScene().getWindow();
          main.setX(0);
          main.setY(0);
          main.setWidth(1920);
          main.setHeight(1080);
          return null;
        });
    pause();
    clickTablePhotoRow();
    waitFor(() -> photoVisible("selected-photo"));
    checkStatusLabels(messages);
    screenshot(output.resolve("01-overview.png"));
    click("edit-project");
    waitFor(() -> find("project-name") != null);
    waitFor(() -> photoVisible("editor-photo"));
    fx(
        () -> {
          Stage editor = (Stage) find("project-name").getScene().getWindow();
          editor.setY(20);
          return null;
        });
    pause();
    fx(
        () -> {
          Stage editor = (Stage) find("project-name").getScene().getWindow();
          editor.setHeight(1040);
          return null;
        });
    pause();
    String editorTitle = fx(() -> ((Stage) find("project-name").getScene().getWindow()).getTitle());
    xdo("search", "--onlyvisible", "--name", "^" + editorTitle + "$", "windowsize", "800", "1040");
    pause();
    screenshot(output.resolve("02-edit.png"));
    click("cancel-project");
    waitFor(() -> find("project-name") == null);
    enter("search-projects", demo.getProperty("search"));
    waitFor(() -> ((TableView<?>) find("projects-table")).getItems().size() == 1);
    clickTableFirstRow();
    waitFor(() -> photoVisible("selected-photo"));
    screenshot(output.resolve("03-search.png"));
    click("clear-search");
    click("language-settings");
    waitFor(() -> find("language-choice") != null);
    click("language-choice");
    screenshot(output.resolve("04-language.png"));
    key(KeyCode.ESCAPE);
    click("language-cancel");
    check(true, "four real 1920x1080 screen captures");
    check(
        fx(() -> ((Label) find("project-count")).getText()).equals(messages.count(5, 5, false)),
        "localized collection count");
  }

  private static void verifyJourney(Path data, Path output, Messages messages) throws Exception {
    fx(
        () -> {
          Stage main = (Stage) find("add-project").getScene().getWindow();
          main.setWidth(860);
          main.setHeight(700);
          return null;
        });
    pause();
    checkButtonsFit();
    screenshot(output.resolve("minimum-main.png"));
    click("add-project");
    waitFor(() -> find("save-project") != null);
    click("save-project");
    waitFor(() -> find("name-validation") instanceof Label label && label.isVisible());
    check(
        fx(() -> ((Label) find("name-validation")).getText())
            .equals(messages.text("validation.name")),
        "localized required-name validation");
    String title = "L'été d'Álvaro – ação – Bücher";
    selectStatus(ProjectStatus.STARTED);
    enter("project-name", title);
    checkContextMenu(messages, output);
    enter("project-material", "Élise O'Connor");
    enter("project-color", "Sauge · Salbeigrün");
    enter("project-location", "São Paulo · étagère 12");
    enter(
        "project-note",
        "Line 1 – Grüße, coração, français\\n".replace("\\n", "\n")
            + "A long note with accents.\n".repeat(28)
            + "Line 30");
    click("add-step");
    click("save-project");
    check(
        fx(() -> ((Label) find("name-validation")).getText())
            .equals(messages.text("validation.steps")),
        "localized empty-step validation");
    var firstStep = fx(() -> ((ChecklistEditor) find("checklist-editor")).input().getFirst().id());
    enter("step-text-" + firstStep, "Préparer le tissu – ação");
    click("add-step");
    var middleStep = fx(() -> ((ChecklistEditor) find("checklist-editor")).input().getLast().id());
    enter("step-text-" + middleStep, "Temporary step");
    click("add-step");
    var lastStep = fx(() -> ((ChecklistEditor) find("checklist-editor")).input().getLast().id());
    enter("step-text-" + lastStep, "Préparer le tissu – ação");
    click("remove-step-" + middleStep);
    click("step-completed-" + firstStep);
    check(
        fx(() -> ((ChecklistEditor) find("checklist-editor")).input().getLast().position()) == 2,
        "removal preserves step positions");
    var expectedSteps = fx(() -> ((ChecklistEditor) find("checklist-editor")).validatedSteps());
    check(
        expectedSteps.size() == 2
            && expectedSteps.get(0).text().equals(expectedSteps.get(1).text()),
        "duplicate step text keeps distinct identities");
    importPhoto(Path.of("assets/store_demo/images/sage.png"), messages);
    waitFor(() -> photoVisible("editor-photo"));
    Path invalid = data.resolve("invalid.png");
    Files.writeString(invalid, "This is not a photograph");
    importPhoto(invalid, messages);
    waitFor(() -> find("save-error") instanceof Label label && label.isVisible());
    check(
        fx(() -> ((Label) find("save-error")).getText())
            .equals(messages.text("photoError.INVALID")),
        "invalid photo message is localized");
    check(fx(() -> photoVisible("editor-photo")), "invalid import retains the previous preview");
    importPhoto(Path.of("assets/store_demo/images/lilac.jpg"), messages);
    waitFor(() -> photoVisible("editor-photo") && !find("choose-photo").isDisabled());
    key(KeyCode.ESCAPE);
    waitFor(() -> confirmationButton(messages.text("action.keep")) != null);
    check(
        fx(() -> confirmationButton(messages.text("action.keep")).isDefaultButton()),
        "keep editing is the safe default");
    key(KeyCode.ENTER);
    waitFor(() -> confirmationButton(messages.text("action.keep")) == null);
    check(
        fx(() -> ((TextInputControl) find("project-name")).getText()).equals(title),
        "draft survives Escape and continue");
    // Force a real SQLite write failure while preserving the entire edit form.
    try (var connection =
            DriverManager.getConnection("jdbc:sqlite:" + data.resolve("naehplan.db"));
        var statement = connection.createStatement()) {
      statement.execute("BEGIN EXCLUSIVE");
      click("save-project");
      waitFor(() -> find("save-error") instanceof Label label && label.isVisible());
      check(
          fx(() -> ((Label) find("save-error")).getText())
              .equals(messages.text("storage.OPERATION")),
          "localized storage failure");
      check(
          fx(() -> ((TextInputControl) find("project-note")).getText()).contains("Line 30"),
          "full draft survives database lock");
      check(
          fx(() -> ((ComboBox<?>) find("project-status")).getValue()) == ProjectStatus.STARTED,
          "status draft survives database lock");
      fx(
          () -> {
            Stage editor = (Stage) find("project-name").getScene().getWindow();
            editor.setWidth(660);
            editor.setHeight(540);
            return null;
          });
      pause();
      checkButtonsFit();
      check(fx(() -> photoVisible("editor-photo")), "photo draft survives database lock");
      check(
          fx(() -> ((ChecklistEditor) find("checklist-editor")).validatedSteps())
              .equals(expectedSteps),
          "checklist draft survives database lock");
      screenshot(output.resolve("minimum-editor-error.png"));
      statement.execute("ROLLBACK");
    }
    click("save-project");
    waitFor(() -> find("save-project") == null);
    check(
        new SqliteProjectRepository(data.resolve("naehplan.db")).findAll().size() == 6,
        "retry saves exactly one project");
    enter("search-projects", "ÁLVARO");
    waitFor(() -> ((TableView<?>) find("projects-table")).getItems().size() == 1);
    clickTableFirstRow();
    click("delete-project");
    waitFor(() -> confirmationButton(messages.text("action.cancel")) != null);
    check(
        fx(() -> confirmationButton(messages.text("action.cancel")).isDefaultButton()),
        "delete defaults to cancel");
    key(KeyCode.ESCAPE);
    waitFor(() -> confirmationButton(messages.text("action.cancel")) == null);
    check(
        new SqliteProjectRepository(data.resolve("naehplan.db")).findAll().size() == 6,
        "cancelled deletion preserves data");
    var repository = new SqliteProjectRepository(data.resolve("naehplan.db"));
    long savedId =
        repository.findAll().stream()
            .filter(project -> project.name().equals(title))
            .findFirst()
            .orElseThrow()
            .id();
    check(
        Arrays.equals(
            repository.findPhoto(savedId).bytes(),
            Files.readAllBytes(Path.of("assets/store_demo/images/lilac.jpg"))),
        "JPEG replacement saved atomically with text after retry");
    check(
        repository.findAll().stream()
                .filter(project -> project.id() == savedId)
                .findFirst()
                .orElseThrow()
                .status()
            == ProjectStatus.STARTED,
        "status and photo saved atomically after retry");
    check(
        repository.findAll().stream()
            .filter(project -> project.id() == savedId)
            .findFirst()
            .orElseThrow()
            .steps()
            .equals(expectedSteps),
        "text, photo, status and steps saved atomically");
    click("edit-project");
    waitFor(() -> find("project-status") != null);
    click("step-completed-" + firstStep);
    key(KeyCode.ESCAPE);
    waitFor(() -> confirmationButton(messages.text("action.discard")) != null);
    fx(
        () -> {
          mouse(confirmationButton(messages.text("action.discard")));
          return null;
        });
    waitFor(() -> find("save-project") == null);
    check(
        repository.findAll().stream()
            .filter(project -> project.id() == savedId)
            .findFirst()
            .orElseThrow()
            .steps()
            .equals(expectedSteps),
        "checkbox-only discard preserves saved checklist");
    click("edit-project");
    waitFor(() -> find("project-status") != null);
    click("step-completed-" + firstStep);
    enter("step-text-" + lastStep, "Coudre les bords – coração");
    expectedSteps = fx(() -> ((ChecklistEditor) find("checklist-editor")).validatedSteps());
    click("save-project");
    waitFor(() -> find("save-project") == null);
    check(
        repository.findAll().stream()
            .filter(project -> project.id() == savedId)
            .findFirst()
            .orElseThrow()
            .steps()
            .equals(expectedSteps),
        "reopen checkbox and rename retain UUID and position");
    Files.writeString(data.resolve("expected-steps.txt"), expectedSteps.toString());
    click("edit-project");
    waitFor(() -> find("project-status") != null);
    selectStatus(ProjectStatus.FINISHED);
    key(KeyCode.ESCAPE);
    waitFor(() -> confirmationButton(messages.text("action.discard")) != null);
    fx(
        () -> {
          mouse(confirmationButton(messages.text("action.discard")));
          return null;
        });
    waitFor(() -> find("save-project") == null);
    check(
        repository.findAll().stream()
                .filter(project -> project.id() == savedId)
                .findFirst()
                .orElseThrow()
                .status()
            == ProjectStatus.STARTED,
        "status-only draft discard preserves saved state");
    for (ProjectStatus next :
        new ProjectStatus[] {
          ProjectStatus.FINISHED, ProjectStatus.PLANNED, ProjectStatus.STARTED
        }) {
      click("edit-project");
      waitFor(() -> find("project-status") != null);
      selectStatus(next);
      click("save-project");
      waitFor(() -> find("save-project") == null);
      check(
          repository.findAll().stream()
                  .filter(project -> project.id() == savedId)
                  .findFirst()
                  .orElseThrow()
                  .status()
              == next,
          "direct status transition persists: " + next.storedValue());
    }
    click("edit-project");
    waitFor(() -> find("remove-photo") != null && !find("remove-photo").isDisabled());
    click("remove-photo");
    check(!fx(() -> photoVisible("editor-photo")), "photo removal clears preview");
    key(KeyCode.ESCAPE);
    waitFor(() -> confirmationButton(messages.text("action.discard")) != null);
    fx(
        () -> {
          mouse(confirmationButton(messages.text("action.discard")));
          return null;
        });
    waitFor(() -> find("save-project") == null);
    check(repository.findPhoto(savedId) != null, "discarding photo removal preserves stored image");
    click("edit-project");
    waitFor(() -> find("remove-photo") != null && !find("remove-photo").isDisabled());
    click("remove-photo");
    click("save-project");
    waitFor(() -> find("save-project") == null);
    check(repository.findPhoto(savedId) == null, "saved photo removal clears the blob");
    click("edit-project");
    waitFor(() -> find("choose-photo") != null);
    importPhoto(Path.of("assets/store_demo/images/sage.png"), messages);
    waitFor(() -> photoVisible("editor-photo") && !find("choose-photo").isDisabled());
    click("save-project");
    waitFor(() -> find("save-project") == null);
    check(
        Arrays.equals(
            repository.findPhoto(savedId).bytes(),
            Files.readAllBytes(Path.of("assets/store_demo/images/sage.png"))),
        "new PNG saved after removal");
    click("clear-search");
    click("language-settings");
    waitFor(() -> find("language-choice") != null);
    Path preference = data.resolve("language.txt");
    Path backup = data.resolve("language.before-test");
    Files.move(preference, backup);
    Files.createDirectory(preference);
    Files.writeString(preference.resolve("blocker"), "synthetic");
    click("language-save");
    waitFor(() -> find("language-error") instanceof Label label && label.isVisible());
    check(
        fx(() -> ((Label) find("language-error")).getText())
            .equals(messages.text("language.saveError")),
        "settings write failure preserves dialog");
    Files.delete(preference.resolve("blocker"));
    Files.delete(preference);
    Files.move(backup, preference);
    click("language-choice");
    key(KeyCode.HOME);
    // automatic, German, English. Switch every language to another language for the restart test.
    String next = messages.locale().getLanguage().equals("en") ? "de-DE" : "en-US";
    key(KeyCode.DOWN);
    if (next.equals("en-US")) {
      key(KeyCode.DOWN);
    }
    key(KeyCode.ENTER);
    click("language-save");
    waitFor(() -> find("language-choice") == null);
    check(new LanguagePreferences(data).load().equals(next), "new language selection saved");
    check(
        fx(() -> ((Label) find("status-message")).getText())
            .equals(messages.text("language.saved")),
        "restart notice in current language");
    Files.writeString(output.resolve("restart-locale.txt"), next + "\n");
  }

  private static void checkContextMenu(Messages messages, Path output) throws Exception {
    fx(
        () -> {
          var control = (TextInputControl) find("project-name");
          Bounds bounds = control.localToScreen(control.getBoundsInLocal());
          Robot robot = new Robot();
          robot.mouseMove(bounds.getCenterX(), bounds.getCenterY());
          robot.mouseClick(MouseButton.SECONDARY);
          return null;
        });
    waitFor(() -> ((TextInputControl) find("project-name")).getContextMenu().isShowing());
    check(
        fx(() ->
                ((TextInputControl) find("project-name"))
                    .getContextMenu().getItems().stream()
                        .map(javafx.scene.control.MenuItem::getText)
                        .toList())
            .equals(
                List.of("undo", "redo", "cut", "copy", "paste", "selectAll").stream()
                    .map(key -> messages.text("context." + key))
                    .toList()),
        "real context menu uses all six translated labels");
    screenshot(output.resolve("context-menu.png"));
    key(KeyCode.ESCAPE);
  }

  private static void selectStatus(ProjectStatus value) throws Exception {
    click("project-status");
    key(KeyCode.HOME);
    for (int index = 0; index < value.ordinal(); index++) {
      key(KeyCode.DOWN);
    }
    key(KeyCode.ENTER);
    check(
        fx(() -> ((ComboBox<?>) find("project-status")).getValue()) == value,
        "keyboard status selection: " + value.storedValue());
  }

  private static void checkStatusLabels(Messages messages) throws Exception {
    fx(
        () -> {
          TableView<?> table = (TableView<?>) find("projects-table");
          for (int index = 0; index < table.getItems().size(); index++) {
            var project = (de.appfabrik.naehplan.domain.Project) table.getItems().get(index);
            check(
                table
                    .getColumns()
                    .get(1)
                    .getCellData(index)
                    .equals(messages.status(project.status())),
                "localized table status: " + project.status().storedValue());
          }
          return null;
        });
  }

  private static void clickTablePhotoRow() throws Exception {
    fx(
        () -> {
          Node row =
              find("projects-table").lookupAll(".table-row-cell").stream()
                  .filter(
                      node ->
                          node instanceof TableRow<?> candidate
                              && candidate.getItem()
                                  instanceof de.appfabrik.naehplan.domain.Project project
                              && project.hasPhoto())
                  .findFirst()
                  .orElseThrow();
          mouse(row);
          return null;
        });
    pause();
  }

  private static boolean photoVisible(String previewId) {
    Node preview = find(previewId);
    return preview != null
        && preview.lookup("#photo-image") instanceof ImageView view
        && view.getImage() != null;
  }

  private static void importPhoto(Path path, Messages messages) throws Exception {
    click("choose-photo");
    String window = null;
    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    while (window == null && System.nanoTime() < deadline) {
      Process search =
          new ProcessBuilder(
                  "xdotool",
                  "search",
                  "--onlyvisible",
                  "--name",
                  "^" + messages.text("photo.chooserTitle") + "$")
              .redirectError(ProcessBuilder.Redirect.DISCARD)
              .start();
      String result =
          new String(search.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      if (search.waitFor() == 0 && !result.isEmpty()) {
        window = result.lines().findFirst().orElseThrow();
      } else {
        pause();
      }
    }
    if (window == null) {
      throw new AssertionError("Native photo chooser did not open");
    }
    xdo("windowfocus", "--sync", window);
    xdo("key", "--clearmodifiers", "ctrl+l");
    pause();
    xdo("type", "--clearmodifiers", "--delay", "1", path.toAbsolutePath().toString());
    xdo("key", "--clearmodifiers", "Return");
    Thread.sleep(600);
  }

  private static void xdo(String... arguments) throws Exception {
    var command = new ArrayList<String>();
    command.add("xdotool");
    command.addAll(List.of(arguments));
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
      process.destroyForcibly();
      throw new AssertionError("X11 input failed: " + output);
    }
  }

  private static void checkButtonsFit() throws Exception {
    fx(
        () -> {
          for (Window window : Window.getWindows()) {
            if (!window.isShowing() || window.getScene() == null) {
              continue;
            }
            for (Node node : window.getScene().getRoot().lookupAll(".button")) {
              if (node instanceof Button button && button.isVisible()) {
                check(
                    button.getWidth() + 1 >= button.prefWidth(-1),
                    "button fits: " + button.getText());
                Bounds bounds = button.localToScene(button.getBoundsInLocal());
                boolean scrollable = false;
                for (var parent = button.getParent(); parent != null; parent = parent.getParent()) {
                  if (parent instanceof ScrollPane) scrollable = true;
                }
                check(
                    scrollable || bounds.getMaxY() <= window.getScene().getHeight() + 1,
                    "button is within window or reachable by scrolling");
              }
            }
          }
          return null;
        });
  }

  private static void clickTableFirstRow() throws Exception {
    fx(
        () -> {
          Node row =
              find("projects-table").lookupAll(".table-row-cell").stream()
                  .filter(
                      node ->
                          node instanceof TableRow<?> candidate
                              && candidate.getIndex() == 0
                              && !candidate.isEmpty())
                  .findFirst()
                  .orElse(null);
          if (row == null) {
            throw new AssertionError("No visible table row");
          }
          mouse(row);
          return null;
        });
    pause();
  }

  private static Button confirmationButton(String text) {
    for (Window window : Window.getWindows()) {
      if (window.isShowing() && window.getScene() != null) {
        for (Node node : window.getScene().getRoot().lookupAll(".button")) {
          if (node instanceof Button button
              && button.getText().equals(text)
              && button.getScene().getRoot().getStyleClass().contains("dialog-pane")) {
            return button;
          }
        }
      }
    }
    return null;
  }

  private static Node find(String id) {
    for (Window window : Window.getWindows()) {
      if (window.isShowing() && window.getScene() != null) {
        Node node = window.getScene().lookup("#" + id);
        if (node != null && node.isVisible()) {
          return node;
        }
      }
    }
    return null;
  }

  private static void click(String id) throws Exception {
    fx(
        () -> {
          Node node = find(id);
          if (node == null || node.isDisabled()) {
            throw new AssertionError("Control unavailable: " + id);
          }
          mouse(node);
          return null;
        });
    pause();
  }

  private static void mouse(Node node) {
    for (var parent = node.getParent(); parent != null; parent = parent.getParent()) {
      if (parent instanceof ScrollPane scroll) {
        Bounds viewport =
            scroll.lookup(".viewport").localToScene(scroll.lookup(".viewport").getBoundsInLocal());
        Bounds target = node.localToScene(node.getBoundsInLocal());
        Bounds content = scroll.getContent().localToScene(scroll.getContent().getBoundsInLocal());
        if (target.getMinY() < viewport.getMinY() || target.getMaxY() > viewport.getMaxY()) {
          double offset = target.getCenterY() - content.getMinY() - viewport.getHeight() / 2;
          scroll.setVvalue(
              Math.max(
                  0,
                  Math.min(1, offset / Math.max(1, content.getHeight() - viewport.getHeight()))));
          scroll.getScene().getRoot().layout();
        }
      }
    }
    Bounds b = node.localToScreen(node.getBoundsInLocal());
    Robot robot = new Robot();
    robot.mouseMove(b.getCenterX(), b.getCenterY());
    robot.mouseClick(MouseButton.PRIMARY);
  }

  private static void enter(String id, String text) throws Exception {
    click(id);
    fx(
        () -> {
          ClipboardContent content = new ClipboardContent();
          content.putString(text);
          Clipboard.getSystemClipboard().setContent(content);
          Robot robot = new Robot();
          robot.keyPress(KeyCode.CONTROL);
          robot.keyType(KeyCode.A);
          robot.keyType(KeyCode.V);
          robot.keyRelease(KeyCode.CONTROL);
          return null;
        });
    pause();
    check(fx(() -> ((TextInputControl) find(id)).getText()).equals(text), "keyboard input: " + id);
  }

  private static void key(KeyCode key) throws Exception {
    fx(
        () -> {
          new Robot().keyType(key);
          return null;
        });
    pause();
  }

  private static void screenshot(Path file) throws Exception {
    pause();
    BufferedImage image =
        fx(
            () -> {
              var capture = new Robot().getScreenCapture(null, 0, 0, 1920, 1080, false);
              int[] pixels = new int[1920 * 1080];
              capture
                  .getPixelReader()
                  .getPixels(0, 0, 1920, 1080, PixelFormat.getIntArgbInstance(), pixels, 0, 1920);
              BufferedImage result = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
              result.setRGB(0, 0, 1920, 1080, pixels, 0, 1920);
              return result;
            });
    if (!ImageIO.write(image, "png", file.toFile())) {
      throw new IllegalStateException("PNG encoder missing");
    }
  }

  private static <T> T fx(Callable<T> operation) throws Exception {
    FutureTask<T> task = new FutureTask<>(operation);
    Platform.runLater(task);
    return task.get(10, TimeUnit.SECONDS);
  }

  private static void waitFor(Callable<Boolean> condition) throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    while (System.nanoTime() < deadline) {
      try {
        if (fx(condition)) {
          return;
        }
      } catch (IllegalStateException notStarted) {
        /* JavaFX startup is asynchronous. */
      }
      Thread.sleep(100);
    }
    throw new AssertionError("Timed out waiting for GUI state");
  }

  private static void pause() throws InterruptedException {
    Thread.sleep(250);
  }

  private static void check(boolean passed, String description) {
    if (!passed) {
      throw new AssertionError(description);
    }
    checks.add(description);
  }
}
