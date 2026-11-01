package de.appfabrik.naehplan.application;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;

/** Resolves one export filename within the folder granted by the native panel. */
public final class ExportFilename {
  private ExportFilename() {}

  public static Path resolve(Path directory, String entered, String extension) {
    if (!extension.equals("json") && !extension.equals("pdf")) {
      throw new IllegalArgumentException("Unsupported export format");
    }
    String name = entered.strip();
    if (name.isBlank()
        || name.equals(".")
        || name.equals("..")
        || name.contains("/")
        || name.contains("\\")
        || name.contains(":")) {
      throw new IllegalArgumentException("Enter a filename, not a path");
    }
    if (name.codePoints().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("Control characters are not filenames");
    }
    if (!name.toLowerCase(Locale.ROOT).endsWith("." + extension)) {
      name += "." + extension;
    }
    if (name.getBytes(StandardCharsets.UTF_8).length > 255) {
      throw new IllegalArgumentException("Filename too long");
    }
    return directory.resolve(name);
  }
}
