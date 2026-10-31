package de.appfabrik.naehplan.ui;

import com.google.gson.GsonBuilder;
import de.appfabrik.naehplan.application.CollectionImport;
import de.appfabrik.naehplan.domain.Project;
import de.appfabrik.naehplan.domain.ProjectDraft;
import de.appfabrik.naehplan.domain.ProjectStatus;
import de.appfabrik.naehplan.i18n.AppLanguage;
import de.appfabrik.naehplan.i18n.Messages;
import de.appfabrik.naehplan.i18n.PrivacyNotice;
import de.appfabrik.naehplan.persistence.JsonCollectionFile;
import de.appfabrik.naehplan.persistence.LanguagePreferences;
import de.appfabrik.naehplan.persistence.SqliteProjectRepository;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.ImageView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;

/** Captures the production UI with an isolated synthetic collection. Not a Mac acceptance test. */
public final class MacStoreContentCapture {
  private static volatile Throwable failure;

  private MacStoreContentCapture() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      throw new IllegalArgumentException("Provide a synthetic data directory and output directory");
    }
    Path data = Path.of(args[0]).toAbsolutePath();
    Path output = Path.of(args[1]).toAbsolutePath();
    if (!Files.isRegularFile(data.resolve(".synthetic-sewingplanner-demo"))
        || Files.exists(data.resolve("naehplan.db"))) {
      throw new IllegalArgumentException("A new marked synthetic data directory is required");
    }
    var files = new JsonCollectionFile();
    var expected = new ArrayList<ProjectDraft>();
    Path demo = Path.of("assets/store_demo/sewing-planner-de-DE.json");
    files.read(demo, expected::add);
    require(expected.size() == 5, "Five demonstration projects required");
    require(
        new HashSet<>(expected.stream().map(ProjectDraft::status).toList())
            .equals(new HashSet<>(List.of(ProjectStatus.values()))),
        "Demonstration must contain all three statuses");
    require(
        expected.stream().filter(project -> project.photo() != null).count() == 3,
        "Three demonstration photos required");
    require(
        expected.stream().allMatch(project -> project.color().matches("#[0-9A-F]{6}")),
        "Every demonstration project must have a color");
    require(
        expected.stream().mapToInt(project -> project.steps().size()).sum() == 15,
        "Fifteen demonstration checklist steps required");
    var repository = new SqliteProjectRepository(data.resolve("naehplan.db"));
    repository.initialize();
    var imports = new CollectionImport(repository, files);
    require(imports.importFile(demo).added() == 5, "Initial demonstration import failed");
    var repeated = imports.importFile(demo);
    require(repeated.added() == 0 && repeated.skipped() == 5, "Repeated import adds duplicates");
    var restored =
        repository.findAll().stream()
            .map(
                project ->
                    new ProjectDraft(
                        project.name(),
                        project.material(),
                        project.color(),
                        project.location(),
                        project.note(),
                        repository.findPhoto(project.id()),
                        project.status(),
                        project.steps()))
            .toList();
    require(expected.equals(restored), "Imported fields, colors, statuses or photo bytes changed");
    new LanguagePreferences(data).save("de-DE");
    var messages = new Messages(AppLanguage.GERMAN);
    Files.writeString(output.resolve("privacy-de-DE.txt"), PrivacyNotice.text(messages));

    Thread capture =
        new Thread(
            () -> {
              try {
                await(() -> find("add-project") != null && !find("add-project").isDisabled());
                fx(
                    () -> {
                      Stage main = (Stage) find("add-project").getScene().getWindow();
                      var screen = Screen.getPrimary().getBounds();
                      main.setX(0);
                      main.setY(0);
                      main.setWidth(screen.getWidth());
                      main.setHeight(screen.getHeight());
                      selectProject("Alltagstasche");
                      return null;
                    });
                await(() -> photoVisible("selected-photo"));
                screenshot(output.resolve("01-overview.png"));

                press("edit-project");
                await(() -> find("project-status") != null && photoVisible("editor-photo"));
                fx(
                    () -> {
                      Stage editor = (Stage) find("project-status").getScene().getWindow();
                      var screen = Screen.getPrimary().getVisualBounds();
                      editor.setWidth(900);
                      editor.setHeight(screen.getHeight() - 40);
                      editor.setX((screen.getWidth() - editor.getWidth()) / 2);
                      editor.setY(20);
                      return null;
                    });
                Thread.sleep(500);
                fx(
                    () -> {
                      ((ScrollPane) find("project-editor-scroll")).setVvalue(1);
                      return null;
                    });
                screenshot(output.resolve("02-edit-checklist.png"));
                press("cancel-project");
                await(() -> find("project-status") == null);

                fx(
                    () -> {
                      ((TextInputControl) find("search-projects")).setText("Innentasche");
                      return null;
                    });
                await(() -> ((TableView<?>) find("projects-table")).getItems().size() == 1);
                fx(
                    () -> {
                      selectProject("Alltagstasche");
                      return null;
                    });
                await(() -> photoVisible("selected-photo"));
                screenshot(output.resolve("03-search.png"));

                Path exported = data.resolve("Meine Nähprojekte.json");
                // Keep the search active to verify that the real export still includes all
                // projects.
                Platform.runLater(() -> ((Button) find("export-collection")).fire());
                chooseExport(exported, messages.text("backup.export.title"));
                await(
                    () ->
                        !find("export-collection").isDisabled()
                            && ((Label) find("status-message"))
                                .getText()
                                .equals(
                                    messages.text(
                                        "backup.export.saved",
                                        5,
                                        exported.getFileName().toString())));
                var exportedProjects = new ArrayList<ProjectDraft>();
                files.read(exported, exportedProjects::add);
                require(
                    expected.equals(exportedProjects),
                    "Export did not retain every complete project");
                press("clear-search");
                fx(
                    () -> {
                      selectProject("Alltagstasche");
                      return null;
                    });
                await(() -> photoVisible("selected-photo"));
                screenshot(output.resolve("04-json-export.png"));
                var checks =
                    Map.of(
                        "syntheticProjects", 5,
                        "photos", 3,
                        "checklistSteps", 15,
                        "allStatuses", true,
                        "allColors", true,
                        "exactImport", true,
                        "duplicateImportSkipped", 5,
                        "uiExportWithActiveSearch", true,
                        "exactExport", true);
                Files.writeString(
                    output.resolve("asset-checks.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(checks) + "\n");
                System.out.println(
                    "Four Linux drafts captured. Synthetic import and UI export verified.");
              } catch (Throwable problem) {
                failure = problem;
                problem.printStackTrace();
                try {
                  screenshot(output.resolve("failure.png"));
                } catch (Exception unavailable) {
                  System.err.println("Failure screenshot unavailable: " + unavailable);
                }
              } finally {
                Platform.runLater(
                    () -> {
                      for (Window window : List.copyOf(Window.getWindows())) window.hide();
                      Platform.exit();
                    });
              }
            },
            "sewing-planner-content-capture");
    capture.setDaemon(true);
    capture.start();
    Application.launch(ProjectsApplication.class, "--data-dir=" + data);
    capture.join(10000);
    if (failure != null) throw new IllegalStateException("Asset capture failed", failure);
    require(!capture.isAlive(), "Capture did not finish");
    require(Files.isRegularFile(output.resolve("asset-checks.json")), "Capture evidence missing");
  }

  private static void require(boolean valid, String explanation) {
    if (!valid) throw new IllegalStateException(explanation);
  }

  @SuppressWarnings("unchecked")
  private static void selectProject(String name) {
    var table = (TableView<Project>) find("projects-table");
    var project =
        table.getItems().stream()
            .filter(item -> item.name().equals(name))
            .findFirst()
            .orElseThrow();
    table.getSelectionModel().select(project);
  }

  private static Node find(String id) {
    for (Window window : Window.getWindows()) {
      if (window.isShowing() && window.getScene() != null) {
        Node node = window.getScene().lookup("#" + id);
        if (node != null && node.isVisible()) return node;
      }
    }
    return null;
  }

  private static boolean photoVisible(String id) {
    Node node = find(id);
    return node != null
        && node.lookup("#photo-image") instanceof ImageView view
        && view.getImage() != null;
  }

  private static void press(String id) throws Exception {
    fx(
        () -> {
          ((Button) find(id)).fire();
          return null;
        });
    Thread.sleep(350);
  }

  private static <T> T fx(Callable<T> action) throws Exception {
    FutureTask<T> task = new FutureTask<>(action);
    Platform.runLater(task);
    return task.get(15, TimeUnit.SECONDS);
  }

  private static void await(Callable<Boolean> available) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
    while (System.nanoTime() < deadline) {
      try {
        if (fx(available)) return;
      } catch (IllegalStateException starting) {
        if (!"Toolkit not initialized".equals(starting.getMessage())) throw starting;
      }
      Thread.sleep(100);
    }
    throw new IllegalStateException("The view needed for the screenshot did not become available");
  }

  private static void chooseExport(Path destination, String title) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
    String window = null;
    while (window == null && System.nanoTime() < deadline) {
      Process search =
          new ProcessBuilder("xdotool", "search", "--onlyvisible", "--name", "^" + title + "$")
              .redirectError(ProcessBuilder.Redirect.DISCARD)
              .start();
      String matches =
          new String(search.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
      if (search.waitFor() == 0 && !matches.isEmpty())
        window = matches.lines().findFirst().orElseThrow();
      else Thread.sleep(100);
    }
    require(window != null, "Native JSON save dialog did not open");
    xdo("windowfocus", "--sync", window);
    xdo("key", "--clearmodifiers", "ctrl+l");
    Thread.sleep(300);
    xdo("key", "--clearmodifiers", "ctrl+a");
    xdo("type", "--clearmodifiers", "--delay", "1", destination.toString());
    xdo("key", "--clearmodifiers", "Return");
  }

  private static void xdo(String... arguments) throws Exception {
    var command = new ArrayList<String>();
    command.add("xdotool");
    command.addAll(List.of(arguments));
    Process process = new ProcessBuilder(command).inheritIO().start();
    if (!process.waitFor(10, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new IllegalStateException("X11 input timed out");
    }
    require(process.exitValue() == 0, "X11 input failed");
  }

  private static void screenshot(Path destination) throws Exception {
    Thread.sleep(800);
    var screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    var bounds = screen.getDefaultConfiguration().getBounds();
    require(bounds.width == 2560 && bounds.height == 1600, "Wrong native Xvfb dimensions");
    // Read native screen pixels without fractional JavaFX scaling rounding the capture size.
    BufferedImage pixels = new Robot(screen).createScreenCapture(bounds);
    require(!pixels.getColorModel().hasAlpha(), "The screen capture must be opaque");
    require(ImageIO.write(pixels, "png", destination.toFile()), "PNG writer unavailable");
  }
}
