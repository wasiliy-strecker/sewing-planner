package de.appfabrik.naehplan.application;

import de.appfabrik.naehplan.domain.Photo;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/** Produces the single stored copy without modifying the selected source file. */
final class PhotoOptimizer {
  private static final int MAX_EDGE = 1600;
  private static final float JPEG_QUALITY = 0.85f;

  private PhotoOptimizer() {}

  static Photo optimize(BufferedImage source, int orientation) {
    boolean transparent = hasTransparency(source);
    int width = orientation >= 5 ? source.getHeight() : source.getWidth();
    int height = orientation >= 5 ? source.getWidth() : source.getHeight();
    double scale = Math.min(1.0, (double) MAX_EDGE / Math.max(width, height));
    int targetWidth = Math.max(1, (int) Math.round(width * scale));
    int targetHeight = Math.max(1, (int) Math.round(height * scale));
    var resized =
        new BufferedImage(
            targetWidth,
            targetHeight,
            transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    try {
      var graphics = resized.createGraphics();
      try {
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(
            RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.scale((double) targetWidth / width, (double) targetHeight / height);
        graphics.drawImage(
            source, transform(orientation, source.getWidth(), source.getHeight()), null);
      } finally {
        graphics.dispose();
      }
      return encode(resized, transparent ? "png" : "jpeg");
    } catch (IOException | IllegalArgumentException exception) {
      throw new PhotoException(PhotoException.Code.OPTIMIZE, exception);
    } finally {
      resized.flush();
    }
  }

  private static boolean hasTransparency(BufferedImage source) {
    if (!source.getColorModel().hasAlpha()) {
      return false;
    }
    int[] row = new int[source.getWidth()];
    for (int y = 0; y < source.getHeight(); y++) {
      source.getRGB(0, y, row.length, 1, row, 0, row.length);
      for (int pixel : row) {
        if ((pixel >>> 24) != 255) {
          return true;
        }
      }
    }
    return false;
  }

  private static AffineTransform transform(int orientation, int width, int height) {
    return switch (orientation) {
      case 2 -> new AffineTransform(-1, 0, 0, 1, width, 0);
      case 3 -> new AffineTransform(-1, 0, 0, -1, width, height);
      case 4 -> new AffineTransform(1, 0, 0, -1, 0, height);
      case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
      case 6 -> new AffineTransform(0, 1, -1, 0, height, 0);
      case 7 -> new AffineTransform(0, -1, -1, 0, height, width);
      case 8 -> new AffineTransform(0, -1, 1, 0, 0, width);
      default -> new AffineTransform();
    };
  }

  private static Photo encode(BufferedImage image, String format) throws IOException {
    var writers = ImageIO.getImageWritersByFormatName(format);
    if (!writers.hasNext()) {
      throw new IOException("No image writer for " + format);
    }
    var writer = writers.next();
    var bytes = new ByteArrayOutputStream();
    try (var output = new MemoryCacheImageOutputStream(bytes)) {
      writer.setOutput(output);
      var parameters = writer.getDefaultWriteParam();
      if (format.equals("jpeg")) {
        parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parameters.setCompressionQuality(JPEG_QUALITY);
      }
      // Fresh metadata: bake orientation into pixels, omit source EXIF/GPS and thumbnails.
      writer.write(null, new IIOImage(image, null, null), parameters);
      output.flush();
    } finally {
      writer.dispose();
    }
    if (bytes.size() > PhotoImporter.MAX_BYTES) {
      throw new IOException("Optimized image exceeds storage limit");
    }
    return new Photo(bytes.toByteArray());
  }
}
