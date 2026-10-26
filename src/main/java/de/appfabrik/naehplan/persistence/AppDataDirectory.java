package de.appfabrik.naehplan.persistence;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public final class AppDataDirectory {
  private AppDataDirectory() {}

  public static Path current() {
    return resolve(
        System.getProperty("os.name"), Path.of(System.getProperty("user.home")), System.getenv());
  }

  static Path resolve(String os, Path home, Map<String, String> environment) {
    String platform = os.toLowerCase(Locale.ROOT);
    if (platform.startsWith("windows")) {
      return base(environment.get("LOCALAPPDATA"), home.resolve("AppData/Local"))
          .resolve("NaehPlan");
    }
    if (platform.startsWith("mac")) {
      return home.resolve("Library/Application Support/NaehPlan");
    }
    return base(environment.get("XDG_DATA_HOME"), home.resolve(".local/share")).resolve("naehplan");
  }

  private static Path base(String configured, Path fallback) {
    return configured == null || configured.isBlank() || !Path.of(configured).isAbsolute()
        ? fallback
        : Path.of(configured);
  }
}
