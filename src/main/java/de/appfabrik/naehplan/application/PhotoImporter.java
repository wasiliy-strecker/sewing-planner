package de.appfabrik.naehplan.application;

import de.appfabrik.naehplan.domain.Photo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Bounded validation and optimization of new file selections. Call outside the JavaFX thread. */
public final class PhotoImporter {
  public static final int MAX_BYTES = 10 * 1024 * 1024;
  public static final int MAX_EDGE = 6000;
  public static final long MAX_PIXELS = 16_000_000;

  private PhotoImporter() {}

  public static Photo read(Path path) {
    try {
      if (!Files.isRegularFile(path)) {
        throw new PhotoException(PhotoException.Code.FILE);
      }
      if (Files.size(path) > MAX_BYTES) {
        throw tooLarge();
      }
      // Limit the actual read too, in case the file changes after the size check.
      try (var input = Files.newInputStream(path)) {
        return decode(input.readNBytes(MAX_BYTES + 1), true);
      }
    } catch (IOException | SecurityException exception) {
      throw new PhotoException(PhotoException.Code.READ, exception);
    }
  }

  public static Photo decode(byte[] bytes) {
    // Database reads and backup restores must retain the exact stored bytes.
    return decode(bytes, false);
  }

  private static Photo decode(byte[] bytes, boolean optimize) {
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
          throw new PhotoException(PhotoException.Code.DIMENSIONS);
        }
        boolean[] warning = {false};
        reader.addIIOReadWarningListener((source, message) -> warning[0] = true);
        var decoded = reader.read(0);
        if (decoded == null) {
          throw invalid();
        }
        try {
          if (warning[0]) {
            throw invalid();
          }
          return optimize
              ? PhotoOptimizer.optimize(decoded, PhotoOrientation.read(bytes, format))
              : new Photo(bytes);
        } finally {
          decoded.flush();
        }
      } finally {
        reader.dispose();
      }
    } catch (IOException | IllegalArgumentException exception) {
      throw new PhotoException(PhotoException.Code.CORRUPT, exception);
    }
  }

  private static PhotoException invalid() {
    return new PhotoException(PhotoException.Code.INVALID);
  }

  private static PhotoException tooLarge() {
    return new PhotoException(PhotoException.Code.SIZE);
  }
}
