package de.appfabrik.naehplan.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppDataDirectoryTest {
  @TempDir Path home;

  @Test
  void resolvesWindowsDataDirectoryAndFallback() {
    Path configured = home.resolve("Local");
    assertEquals(
        configured.resolve("NaehPlan"),
        AppDataDirectory.resolve(
            "Windows 11", home, Map.of("LOCALAPPDATA", configured.toString())));
    assertEquals(
        home.resolve("AppData/Local/NaehPlan"),
        AppDataDirectory.resolve("Windows 11", home, Map.of()));
  }

  @Test
  void resolvesMacDataDirectory() {
    assertEquals(
        home.resolve("Library/Application Support/NaehPlan"),
        AppDataDirectory.resolve("Mac OS X", home, Map.of()));
  }

  @Test
  void usesAbsoluteXdgDataHomeOrUserFallback() {
    Path configured = home.resolve("xdg");
    assertEquals(
        configured.resolve("naehplan"),
        AppDataDirectory.resolve("Linux", home, Map.of("XDG_DATA_HOME", configured.toString())));
    List<Map<String, String>> environments =
        List.of(Map.of(), Map.of("XDG_DATA_HOME", ""), Map.of("XDG_DATA_HOME", "relative"));
    for (Map<String, String> environment : environments) {
      assertEquals(
          home.resolve(".local/share/naehplan"),
          AppDataDirectory.resolve("Linux", home, environment));
    }
  }
}
