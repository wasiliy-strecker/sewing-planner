package de.appfabrik.naehplan.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class PrivacyPolicyTest {
  @Test
  void opensOnlyThisAppsCanonicalPolicyAndMatchesStoreLinks() throws Exception {
    var opened = new ArrayList<URI>();
    assertTrue(PrivacyPolicy.open(opened::add));
    assertEquals(
        java.util.List.of(
            URI.create("https://github.com/wasiliy-strecker/sewing-planner/blob/main/PRIVACY.md")),
        opened);
    assertTrue(
        Files.readString(Path.of("PRIVACY.md")).startsWith("# Privacy policy for Sewing Planner"));
    try (var files = Files.walk(Path.of("store-listings"))) {
      for (Path file :
          files.filter(path -> path.getFileName().toString().equals("privacy-url.txt")).toList()) {
        assertEquals(PrivacyPolicy.URL.toString(), Files.readString(file).strip(), file.toString());
      }
    }
  }

  @Test
  void reportsUnavailableBrowserAndLaunchFailures() {
    assertFalse(
        PrivacyPolicy.open(
            uri -> {
              throw new IOException("Synthetic launch failure");
            }));
    assertFalse(
        PrivacyPolicy.open(
            uri -> {
              throw new UnsupportedOperationException("No browser");
            }));
    assertFalse(
        PrivacyPolicy.open(
            uri -> {
              throw new SecurityException("Denied");
            }));
  }
}
