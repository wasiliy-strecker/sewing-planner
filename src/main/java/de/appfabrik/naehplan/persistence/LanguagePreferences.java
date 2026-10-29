package de.appfabrik.naehplan.persistence;

import de.appfabrik.naehplan.i18n.AppLanguage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Small app-local setting, isolated by the same data-dir option as the collection. */
public final class LanguagePreferences {
  private final Path file;

  public LanguagePreferences(Path directory) {
    file = directory.toAbsolutePath().normalize().resolve("language.txt");
  }

  public String load() throws IOException {
    try {
      // Settings are written by this app and have a tiny, bounded format.
      if (Files.size(file) > 128) {
        return AppLanguage.ENGLISH.tag();
      }
      String stored = Files.readString(file, StandardCharsets.UTF_8).strip();
      return AppLanguage.isChoice(stored) ? stored : AppLanguage.ENGLISH.tag();
    } catch (NoSuchFileException missing) {
      return AppLanguage.AUTOMATIC;
    }
  }

  public void save(String choice) throws IOException {
    if (!AppLanguage.isChoice(choice)) {
      throw new IllegalArgumentException("Unsupported language choice");
    }
    Path directory = Objects.requireNonNull(file.getParent());
    Files.createDirectories(directory);
    Path temporary = Files.createTempFile(directory, "language-", ".tmp");
    try {
      Files.writeString(temporary, choice + "\n", StandardCharsets.UTF_8);
      Files.move(
          temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
