package de.appfabrik.naehplan.ui;

import java.util.Objects;
import javafx.scene.text.Font;

/** Registers the bundled UI fonts before applying an app stylesheet. */
final class UiFonts {
  private static boolean loaded;

  private UiFonts() {}

  static synchronized void load() {
    if (loaded) {
      return;
    }
    for (String style : new String[] {"Regular", "Bold"}) {
      var resource =
          Objects.requireNonNull(
              UiFonts.class.getResource(
                  "/de/appfabrik/naehplan/fonts/NotoSansCJKjp-" + style + ".otf"),
              "Missing bundled UI font: " + style);
      if (Font.loadFont(resource.toExternalForm(), 14) == null) {
        throw new IllegalStateException("Cannot load bundled UI font: " + style);
      }
    }
    loaded = true;
  }
}
