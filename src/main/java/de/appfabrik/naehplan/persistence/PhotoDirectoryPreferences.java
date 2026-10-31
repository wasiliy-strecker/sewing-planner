package de.appfabrik.naehplan.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** Remembers only the photo directory, separately from collection contents and exports. */
public final class PhotoDirectoryPreferences {
  private static final int MAX_BYTES = 16_384;
  private final Path directory;
  private final Path file;
  private final Path home = Path.of(System.getProperty("user.home"));
  private final Path pictures;
  private volatile Path lastDirectory;

  public PhotoDirectoryPreferences(Path dataDirectory) {
    directory = dataDirectory.toAbsolutePath().normalize();
    file = directory.resolve("photo-directory.txt");
    lastDirectory = readSavedDirectory();
    pictures = findPicturesDirectory();
  }

  public Path initialDirectory() {
    Path saved = lastDirectory;
    if (usable(saved)) {
      return saved;
    }
    if (usable(pictures)) {
      return pictures;
    }
    if (usable(home.resolve("Pictures"))) {
      return home.resolve("Pictures");
    }
    if (usable(home.resolve("Bilder"))) {
      return home.resolve("Bilder");
    }
    return usable(home) ? home : null;
  }

  /** Called on the background worker after a file was chosen, even if its image is invalid. */
  public boolean remember(Path selectedFile) {
    Path selectedDirectory = selectedFile.toAbsolutePath().normalize().getParent();
    if (selectedDirectory == null) {
      return false;
    }
    lastDirectory = selectedDirectory;
    Path temporary = null;
    try {
      Files.createDirectories(directory);
      temporary = Files.createTempFile(directory, "photo-directory-", ".tmp");
      Files.writeString(temporary, selectedDirectory.toString(), StandardCharsets.UTF_8);
      try {
        Files.move(
            temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException unsupported) {
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
      }
      return true;
    } catch (IOException | SecurityException failure) {
      return false;
    } finally {
      if (temporary != null) {
        try {
          Files.deleteIfExists(temporary);
        } catch (IOException | SecurityException ignored) {
          // A cleanup failure does not change a successfully saved preference.
        }
      }
    }
  }

  private Path readSavedDirectory() {
    try {
      String value = readSmallFile(file);
      if (value.isEmpty()) {
        return null;
      }
      Path stored = Path.of(value);
      return stored.isAbsolute() ? stored : null;
    } catch (IOException | InvalidPathException | SecurityException failure) {
      return null;
    }
  }

  private Path findPicturesDirectory() {
    if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux")) {
      return home.resolve("Pictures");
    }
    try {
      String configured = System.getenv("XDG_CONFIG_HOME");
      Path config =
          configured == null || configured.isBlank()
              ? home.resolve(".config")
              : Path.of(configured);
      if (!config.isAbsolute()) {
        config = home.resolve(".config");
      }
      for (String line : readSmallFile(config.resolve("user-dirs.dirs")).lines().toList()) {
        String entry = line.strip();
        if (entry.startsWith("XDG_PICTURES_DIR=\"") && entry.endsWith("\"")) {
          String value = entry.substring("XDG_PICTURES_DIR=\"".length(), entry.length() - 1);
          // Interpret the conventional home prefix as data, never execute shell expressions.
          if (value.equals("$HOME") || value.startsWith("$HOME/")) {
            value = home + value.substring(5);
          }
          value = value.replace("\\\"", "\"").replace("\\\\", "\\");
          Path directory = Path.of(value);
          if (directory.isAbsolute()) {
            return directory;
          }
        }
      }
    } catch (IOException | InvalidPathException | SecurityException failure) {
      // Optional system configuration may be absent or unreadable.
    }
    return home.resolve("Pictures");
  }

  private static boolean usable(Path directory) {
    try {
      return directory != null && Files.isDirectory(directory) && Files.isReadable(directory);
    } catch (SecurityException failure) {
      return false;
    }
  }

  private static String readSmallFile(Path path) throws IOException {
    try (var input = Files.newInputStream(path)) {
      byte[] content = input.readNBytes(MAX_BYTES + 1);
      if (content.length > MAX_BYTES) {
        throw new IOException("Preference file exceeds the size limit");
      }
      return new String(content, StandardCharsets.UTF_8);
    }
  }
}
