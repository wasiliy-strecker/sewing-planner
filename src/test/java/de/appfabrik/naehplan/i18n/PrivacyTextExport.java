package de.appfabrik.naehplan.i18n;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Exports the production privacy text without launching JavaFX or loading user data. */
public final class PrivacyTextExport {
  private PrivacyTextExport() {}

  public static void main(String[] arguments) throws IOException {
    if (arguments.length != 1) {
      throw new IllegalArgumentException("Provide one new output directory");
    }
    export(Path.of(arguments[0]));
  }

  static void export(Path output) throws IOException {
    Files.createDirectory(output);
    for (AppLanguage language : AppLanguage.values()) {
      Files.writeString(
          output.resolve(language.tag() + ".txt"),
          PrivacyNotice.text(new Messages(language)),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE_NEW);
    }
  }
}
