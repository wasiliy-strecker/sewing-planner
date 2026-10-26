package de.appfabrik.naehplan.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Generated color rectangles, never personal or downloaded photos. */
public final class TestImages {
  private TestImages() {}

  public static byte[] image(String format, int width, int height, int rgb) throws IOException {
    var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    try {
      graphics.setColor(new java.awt.Color(rgb));
      graphics.fillRect(0, 0, width, height);
    } finally {
      graphics.dispose();
    }
    try (var output = new ByteArrayOutputStream()) {
      if (!ImageIO.write(image, format, output)) {
        throw new IOException("No test writer");
      }
      return output.toByteArray();
    } finally {
      image.flush();
    }
  }
}
