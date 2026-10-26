package de.appfabrik.naehplan.application;

import de.appfabrik.naehplan.domain.Photo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Bounded, content-based validation. Call outside the JavaFX thread. */
public final class PhotoImporter {
  public static final int MAX_BYTES = 10 * 1024 * 1024;
  public static final int MAX_EDGE = 6000;
  public static final long MAX_PIXELS = 16_000_000;

  private PhotoImporter() {}

  public static Photo read(Path path) {
    try {
      if (!Files.isRegularFile(path)) {
        throw new PhotoException("Bitte wähle eine lesbare JPEG- oder PNG-Datei aus.");
      }
      if (Files.size(path) > MAX_BYTES) {
        throw tooLarge();
      }
      // Limit the actual read too, in case the file changes after the size check.
      try (var input = Files.newInputStream(path)) {
        return decode(input.readNBytes(MAX_BYTES + 1));
      }
    } catch (IOException | SecurityException exception) {
      throw new PhotoException(
          "Das Foto konnte nicht gelesen werden. Bitte prüfe Datei und Zugriffsrechte.", exception);
    }
  }

  public static Photo decode(byte[] bytes) {
    if (bytes.length > MAX_BYTES) {
      throw tooLarge();
    }
    try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw invalid();
      }
      var reader = readers.next();
      try {
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!format.equals("jpeg") && !format.equals("png")) {
          throw invalid();
        }
        reader.setInput(input, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width < 1
            || height < 1
            || width > MAX_EDGE
            || height > MAX_EDGE
            || (long) width * height > MAX_PIXELS) {
          throw new PhotoException(
              "Das Foto ist zu groß. Erlaubt sind höchstens 6000 Pixel je Seite und insgesamt 16 Millionen Pixel.");
        }
        boolean[] warning = {false};
        reader.addIIOReadWarningListener((source, message) -> warning[0] = true);
        var decoded = reader.read(0);
        if (decoded == null || warning[0]) {
          throw invalid();
        }
        decoded.flush();
        return new Photo(bytes);
      } finally {
        reader.dispose();
      }
    } catch (IOException | IllegalArgumentException exception) {
      throw new PhotoException(
          "Das Foto ist beschädigt oder kein lesbares JPEG- oder PNG-Bild.", exception);
    }
  }

  private static PhotoException invalid() {
    return new PhotoException(
        "Bitte wähle ein gültiges JPEG- oder PNG-Bild. Andere oder beschädigte Dateien werden nicht übernommen.");
  }

  private static PhotoException tooLarge() {
    return new PhotoException(
        "Die Fotodatei ist zu groß. Erlaubt sind höchstens 10 MiB (10.485.760 Bytes).");
  }
}
