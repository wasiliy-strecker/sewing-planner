package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.i18n.Messages;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

final class AppInformation {
  private AppInformation() {}

  static String about(Messages messages) {
    Properties properties = new Properties();
    try {
      properties.load(new StringReader(resource("/app-info/version.properties")));
    } catch (IOException exception) {
      throw new IllegalStateException("Missing app version", exception);
    }
    return "Sewing Planner\n"
        + messages.text("about.version", properties.getProperty("version"))
        + "\n\n"
        + messages.text("app.subtitle")
        + "\n"
        + messages.text("stored")
        + "\n\n"
        + messages.text("about.developer")
        + "\nsupport@appfabrik-ai.de";
  }

  static String licenses(Messages messages) {
    StringBuilder text = new StringBuilder(messages.text("licenses.intro"));
    for (String path :
        resource("/app-info/license-index.txt").lines().filter(line -> !line.isBlank()).toList()) {
      text.append("\n\n────────────────────\n")
          .append(path.substring(path.lastIndexOf('/') + 1))
          .append("\n\n")
          .append(resource(path));
    }
    return text.toString();
  }

  private static String resource(String path) {
    try (var input = AppInformation.class.getResourceAsStream(path)) {
      if (input == null) {
        throw new IllegalStateException("Missing application resource: " + path);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Unreadable application resource: " + path, exception);
    }
  }
}
