package de.appfabrik.naehplan.ui;

import static de.appfabrik.naehplan.ui.FxTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.i18n.AppLanguage;
import de.appfabrik.naehplan.i18n.Messages;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.image.PixelFormat;
import javafx.scene.layout.StackPane;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@EnabledIfSystemProperty(named = "naehplan.uiTests", matches = "true")
class InfoMenuUiTest {
  @BeforeAll
  static void startToolkit() throws Exception {
    start();
  }

  @AfterEach
  void closeWindows() throws Exception {
    fx(
        () -> {
          for (Window window : List.copyOf(Window.getWindows())) window.hide();
          return null;
        });
  }

  @ParameterizedTest
  @EnumSource(AppLanguage.class)
  void opensCanonicalPolicyAndKeepsAboutAndLicensesLocalized(AppLanguage language)
      throws Exception {
    Messages messages = new Messages(language);
    Stage owner =
        fx(
            () -> {
              Stage stage = new Stage();
              stage.setTitle("Synthetic info test");
              stage.setScene(new Scene(new StackPane(), 780, 660));
              stage.show();
              return stage;
            });
    var opened = new java.util.ArrayList<java.net.URI>();
    MenuButton menu =
        fx(
            () -> {
              MenuButton button = InfoMenu.create(owner, messages, opened::add);
              ((StackPane) owner.getScene().getRoot()).getChildren().add(button);
              return button;
            });
    List<String> names = List.of("about", "privacy", "licenses");
    assertEquals(
        names.stream().map(name -> messages.text("info." + name)).toList(),
        fx(() -> menu.getItems().stream().map(item -> item.getText()).toList()));
    for (int i = 0; i < names.size(); i++) {
      int selected = i;
      String name = names.get(i);
      fx(
          () -> {
            menu.getItems().get(selected).fire();
            return null;
          });
      if (name.equals("privacy")) {
        assertEquals(List.of(PrivacyPolicy.URL), opened);
        assertEquals(1, fx(() -> Window.getWindows().stream().filter(Window::isShowing).count()));
        continue;
      }
      await(
          () ->
              uncheckedFx(
                  () ->
                      Window.getWindows().stream()
                          .anyMatch(
                              window ->
                                  window != owner
                                      && window.isShowing()
                                      && window.getOpacity() == 1)));
      Stage dialog =
          fx(
              () ->
                  (Stage)
                      Window.getWindows().stream()
                          .filter(window -> window != owner && window.isShowing())
                          .findFirst()
                          .orElseThrow());
      TextArea text = fx(() -> (TextArea) dialog.getScene().lookup("#" + name + "-text"));
      assertNotNull(text);
      assertFalse(fx(text::isEditable));
      assertEquals(owner, fx(dialog::getOwner));
      String expected =
          switch (name) {
            case "about" -> AppInformation.about(messages);
            default -> AppInformation.licenses(messages);
          };
      assertEquals(expected.replace("\r\n", "\n").replace("\r", "\n"), fx(text::getText));
      assertEquals(messages.text("info." + name), fx(dialog::getTitle));
      String captures = System.getenv("naehplan_INFO_SCREENSHOTS");
      if (captures != null) {
        Path path = Path.of(captures, language.tag() + "-" + name + ".png");
        Files.createDirectories(path.getParent());
        BufferedImage pixels =
            fx(
                () -> {
                  var image =
                      new Robot()
                          .getScreenCapture(
                              null,
                              dialog.getX(),
                              dialog.getY(),
                              dialog.getWidth(),
                              dialog.getHeight(),
                              false);
                  int width = (int) image.getWidth();
                  int height = (int) image.getHeight();
                  int[] argb = new int[width * height];
                  image
                      .getPixelReader()
                      .getPixels(
                          0, 0, width, height, PixelFormat.getIntArgbInstance(), argb, 0, width);
                  BufferedImage result =
                      new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                  result.setRGB(0, 0, width, height, argb, 0, width);
                  return result;
                });
        assertTrue(ImageIO.write(pixels, "png", path.toFile()));
      }
      fx(
          () -> {
            ((Button) dialog.getScene().lookup("#" + name + "-close")).fire();
            return null;
          });
      await(() -> !uncheckedFx(dialog::isShowing));
      assertTrue(fx(owner::isShowing));
    }
  }

  @ParameterizedTest
  @EnumSource(AppLanguage.class)
  void failedBrowserLaunchShowsLocalizedCopyableAddress(AppLanguage language) throws Exception {
    Messages messages = new Messages(language);
    Stage owner =
        fx(
            () -> {
              Stage stage = new Stage();
              stage.setScene(new Scene(new StackPane(), 780, 660));
              stage.show();
              MenuButton menu =
                  InfoMenu.create(
                      stage,
                      messages,
                      uri -> {
                        throw new java.io.IOException("Synthetic browser failure");
                      });
              ((StackPane) stage.getScene().getRoot()).getChildren().add(menu);
              menu.getItems().get(1).fire();
              return stage;
            });
    await(
        () ->
            uncheckedFx(
                () ->
                    Window.getWindows().stream()
                        .anyMatch(
                            window ->
                                window != owner
                                    && window.isShowing()
                                    && window.getOpacity() == 1)));
    TextArea text =
        fx(
            () ->
                (TextArea)
                    Window.getWindows().stream()
                        .filter(window -> window != owner && window.isShowing())
                        .findFirst()
                        .orElseThrow()
                        .getScene()
                        .lookup("#privacy-error-text"));
    assertNotNull(text);
    assertFalse(fx(text::isEditable));
    assertEquals(
        messages.text("privacy.openError") + "\n\n" + PrivacyPolicy.URL, fx(text::getText));
    assertEquals(
        PrivacyPolicy.URL.toString(),
        fx(
            () -> {
              int start = text.getText().lastIndexOf(PrivacyPolicy.URL.toString());
              text.selectRange(start, text.getLength());
              return text.getSelectedText();
            }));
  }
}
