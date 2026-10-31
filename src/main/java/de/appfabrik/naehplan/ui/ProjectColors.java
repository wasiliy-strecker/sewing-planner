package de.appfabrik.naehplan.ui;

import java.util.Locale;
import javafx.scene.paint.Color;

/** Stable sRGB values, while older free-text descriptions remain readable. */
final class ProjectColors {
  private ProjectColors() {}

  static Color parse(String value) {
    return value != null && value.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")
        ? Color.web(value)
        : null;
  }

  static String encode(Color color) {
    String rgb =
        String.format(
            Locale.ROOT,
            "#%02X%02X%02X",
            Math.round(color.getRed() * 255),
            Math.round(color.getGreen() * 255),
            Math.round(color.getBlue() * 255));
    long alpha = Math.round(color.getOpacity() * 255);
    return alpha == 255 ? rgb : rgb + String.format(Locale.ROOT, "%02X", alpha);
  }
}
