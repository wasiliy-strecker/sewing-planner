package de.appfabrik.naehplan.application;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExportFilenameTest {
  @TempDir Path folder;

  @Test
  void acceptsUnicodeAndKeepsBothExportFormatsInsideTheGrantedFolder() {
    for (String extension : List.of("json", "pdf")) {
      assertEquals(
          folder.resolve("Bücher 日本." + extension),
          ExportFilename.resolve(folder, " Bücher 日本 ", extension));
      assertEquals(
          folder.resolve("Export." + extension.toUpperCase(java.util.Locale.ROOT)),
          ExportFilename.resolve(
              folder, "Export." + extension.toUpperCase(java.util.Locale.ROOT), extension));
    }
  }

  @Test
  void rejectsPathsControlsAndNamesExceedingTheMacFilenameLimit() {
    for (String extension : List.of("json", "pdf")) {
      for (String name :
          List.of(
              "",
              " ",
              ".",
              "..",
              "../export",
              "/tmp/export",
              "x/y",
              "x\\y",
              "C:export",
              "a\nb",
              "書".repeat(85),
              "x".repeat(252))) {
        assertThrows(
            IllegalArgumentException.class, () -> ExportFilename.resolve(folder, name, extension));
      }
    }
  }
}
