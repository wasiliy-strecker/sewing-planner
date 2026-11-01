package de.appfabrik.naehplan.ui;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.i18n.AppLanguage;
import de.appfabrik.naehplan.i18n.Messages;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@EnabledIfSystemProperty(named = "naehplan.uiTests", matches = "true")
class ExportFileDialogUiTest {
  @TempDir Path folder;
  private Stage owner;

  @BeforeAll
  static void startToolkit() throws Exception {
    FxTestSupport.start();
  }

  @BeforeEach
  void showOwner() throws Exception {
    owner =
        FxTestSupport.fx(
            () -> {
              var stage = new Stage();
              stage.setScene(new Scene(new VBox(), 500, 300));
              stage.show();
              return stage;
            });
  }

  @AfterEach
  void closeWindows() throws Exception {
    FxTestSupport.fx(
        () -> {
          for (Window window : List.copyOf(Window.getWindows())) {
            window.hide();
          }
          return null;
        });
  }

  @ParameterizedTest
  @EnumSource(AppLanguage.class)
  void localizedDialogReturnsOnlyAFileWithinTheGrantedFolder(AppLanguage language)
      throws Exception {
    for (String extension : List.of("json")) {
      var chosen = open(language, extension);
      FxTestSupport.fx(
          () -> {
            field().setText("Bücher 日本");
            confirm().fire();
            return null;
          });
      assertEquals(folder.resolve("Bücher 日本." + extension), chosen.get(10, TimeUnit.SECONDS));
    }
    assertEmptyFolder();
  }

  @Test
  void traversalAndInvalidNamesCannotBeConfirmedButAValidCorrectionCan() throws Exception {
    var chosen = open(AppLanguage.ENGLISH, "json");
    for (String invalid :
        List.of("", "../outside", "/tmp/outside", "nested/file", "x\\y", "書".repeat(85))) {
      FxTestSupport.fx(
          () -> {
            field().setText(invalid);
            assertTrue(confirm().isDisabled());
            confirm().fire();
            return null;
          });
      assertFalse(chosen.isDone());
    }
    FxTestSupport.fx(
        () -> {
          field().setText("Valid.JSON");
          assertFalse(confirm().isDisabled());
          confirm().fire();
          return null;
        });
    assertEquals(folder.resolve("Valid.JSON"), chosen.get(10, TimeUnit.SECONDS));
    assertEmptyFolder();
  }

  @Test
  void cancellingTheFilenameDialogDoesNotCreateAnExport() throws Exception {
    var chosen = open(AppLanguage.ENGLISH, "json");
    FxTestSupport.fx(
        () -> {
          DialogPane pane = pane();
          var cancel =
              pane.getButtonTypes().stream()
                  .filter(type -> type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE)
                  .findFirst()
                  .orElseThrow();
          ((Button) pane.lookupButton(cancel)).fire();
          return null;
        });
    assertNull(chosen.get(10, TimeUnit.SECONDS));
    assertEmptyFolder();
  }

  @Test
  void everyMacLaunchUsesFolderConsentWithoutDependingOnAPackagingFlag() {
    assertTrue(ExportFileDialog.usesFolderSelection("Mac OS X"));
    assertTrue(ExportFileDialog.usesFolderSelection("macOS"));
    assertFalse(ExportFileDialog.usesFolderSelection("Windows 11"));
    assertFalse(ExportFileDialog.usesFolderSelection("Linux"));
  }

  private CompletableFuture<Path> open(AppLanguage language, String extension) throws Exception {
    var chosen = new CompletableFuture<Path>();
    Platform.runLater(
        () -> {
          try {
            chosen.complete(
                ExportFileDialog.filename(
                    owner,
                    new Messages(language),
                    "Export",
                    folder,
                    "Collection." + extension,
                    extension));
          } catch (RuntimeException failure) {
            chosen.completeExceptionally(failure);
          }
        });
    FxTestSupport.await(() -> FxTestSupport.uncheckedFx(() -> pane() != null));
    return chosen;
  }

  private static DialogPane pane() {
    for (Window window : Window.getWindows()) {
      if (window.getScene().getRoot() instanceof DialogPane pane) {
        return pane;
      }
    }
    return null;
  }

  private static TextField field() {
    return (TextField) pane().lookup("#export-filename");
  }

  private static Button confirm() {
    return (Button) pane().lookup("#export-filename-confirm");
  }

  private void assertEmptyFolder() throws Exception {
    try (var contents = Files.list(folder)) {
      assertEquals(0, contents.count());
    }
  }
}
