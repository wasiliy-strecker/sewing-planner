package de.appfabrik.naehplan.ui;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.i18n.AppLanguage;
import de.appfabrik.naehplan.i18n.Messages;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AppInformationTest {
  @ParameterizedTest
  @EnumSource(AppLanguage.class)
  void embedsActualLicenseFilesAndSeparatesAboutFromLegalText(AppLanguage language)
      throws Exception {
    Messages messages = new Messages(language);
    String about = AppInformation.about(messages);
    assertTrue(about.startsWith("Sewing Planner\n"));
    assertTrue(about.contains(messages.text("app.subtitle")));
    assertFalse(about.contains("${project.version}"));
    assertFalse(about.contains("null"));
    assertFalse(about.contains("GNU General Public License"));
    String licenses = AppInformation.licenses(messages);
    assertTrue(licenses.startsWith(messages.text("licenses.intro")));
    assertTrue(licenses.contains("GNU General Public License"));
    assertTrue(licenses.contains("Apache License"));
    assertTrue(licenses.contains("SIL OPEN FONT LICENSE"));
    try (var paths = Files.walk(Path.of("third-party-licenses"))) {
      for (Path path : paths.filter(Files::isRegularFile).toList()) {
        assertTrue(licenses.contains(Files.readString(path)), path.toString());
      }
    }
  }
}
