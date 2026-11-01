package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.i18n.Messages;
import java.util.function.Supplier;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.stage.Window;

final class InfoMenu {
  private InfoMenu() {}

  static MenuButton create(Window owner, Messages messages) {
    MenuItem about = new MenuItem(messages.text("info.about"));
    about.setId("info-about");
    about.setOnAction(
        event ->
            show(owner, messages, "about", "info.about", () -> AppInformation.about(messages)));
    MenuItem privacy = new MenuItem(messages.text("info.privacy"));
    privacy.setId("info-privacy");
    privacy.setOnAction(event -> new PrivacyDialog(owner, messages).show());
    MenuItem licenses = new MenuItem(messages.text("info.licenses"));
    licenses.setId("info-licenses");
    licenses.setOnAction(
        event ->
            show(
                owner,
                messages,
                "licenses",
                "info.licenses",
                () -> AppInformation.licenses(messages)));
    MenuButton menu = new MenuButton(messages.text("info.title"), null, about, privacy, licenses);
    menu.setId("app-info");
    return menu;
  }

  private static void show(
      Window owner, Messages messages, String prefix, String title, Supplier<String> content) {
    String text;
    try {
      text = content.get();
    } catch (IllegalStateException exception) {
      text = messages.text("info.unavailable");
    }
    new InfoDialog(owner, messages, messages.text(title), text, prefix).show();
  }
}
