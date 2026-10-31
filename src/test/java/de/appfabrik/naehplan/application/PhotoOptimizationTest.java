package de.appfabrik.naehplan.application;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhotoOptimizationTest {
  @TempDir Path directory;

  @Test
  void shrinksLargeOpaquePhotosButNeverUpscalesOrChangesTheOriginal() throws Exception {
    for (int width : new int[] {120, 3200}) {
      Path path = directory.resolve("photo-" + width + ".png");
      byte[] original = image("png", width, width / 2, false);
      Files.write(path, original);
      var photo = PhotoImporter.read(path);
      var decoded = ImageIO.read(photo.openStream());
      assertEquals(Math.min(width, 1600), decoded.getWidth());
      assertEquals(Math.min(width, 1600) / 2, decoded.getHeight());
      assertEquals(0xff, Byte.toUnsignedInt(photo.bytes()[0]));
      assertArrayEquals(original, Files.readAllBytes(path));
      assertEquals(photo, PhotoImporter.decode(photo.bytes()));
    }
  }

  @Test
  void preservesTransparentPngAndStoredBytes() throws Exception {
    Path path = directory.resolve("alpha.png");
    Files.write(path, image("png", 120, 80, true));
    var photo = PhotoImporter.read(path);
    assertEquals(137, Byte.toUnsignedInt(photo.bytes()[0]));
    var decoded = ImageIO.read(photo.openStream());
    assertEquals(64, decoded.getRGB(0, 0) >>> 24);
    assertEquals(120, decoded.getWidth());
    assertEquals(80, decoded.getHeight());
    assertEquals(photo, PhotoImporter.decode(photo.bytes()));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8})
  void bakesEveryExifRotationAndReflectionIntoJpegAndPngPixels(int orientation) throws Exception {
    int[][] corners = {
      {0, 1, 2, 3}, {1, 0, 3, 2}, {3, 2, 1, 0}, {2, 3, 0, 1},
      {0, 2, 1, 3}, {2, 0, 3, 1}, {3, 1, 2, 0}, {1, 3, 0, 2}
    };
    int[] colors = {0xff0000, 0x00ff00, 0x0000ff, 0xffff00};
    for (String format : new String[] {"jpeg", "png"}) {
      Path path = directory.resolve(format + orientation);
      byte[] original = oriented(image(format, 120, 80, false), format, orientation);
      Files.write(path, original);
      var photo = PhotoImporter.read(path);
      var decoded = ImageIO.read(photo.openStream());
      assertEquals(orientation >= 5 ? 80 : 120, decoded.getWidth());
      assertEquals(orientation >= 5 ? 120 : 80, decoded.getHeight());
      int[][] positions = {
        {10, 10},
        {decoded.getWidth() - 11, 10},
        {10, decoded.getHeight() - 11},
        {decoded.getWidth() - 11, decoded.getHeight() - 11}
      };
      for (int c = 0; c < 4; c++) {
        int actual = decoded.getRGB(positions[c][0], positions[c][1]);
        int expected = colors[corners[orientation - 1][c]];
        for (int shift : new int[] {0, 8, 16}) {
          assertTrue(
              Math.abs(((actual >> shift) & 255) - ((expected >> shift) & 255)) < 20,
              format + " orientation " + orientation + " corner " + c);
        }
      }
      assertEquals(1, PhotoOrientation.read(photo.bytes(), "jpeg"));
      assertArrayEquals(original, Files.readAllBytes(path));
    }
  }

  private static byte[] image(String format, int width, int height, boolean alpha)
      throws Exception {
    var image =
        new BufferedImage(
            width, height, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int rgb =
            y < height / 2
                ? (x < width / 2 ? 0xff0000 : 0x00ff00)
                : (x < width / 2 ? 0x0000ff : 0xffff00);
        image.setRGB(x, y, (alpha && x == 0 ? 0x40000000 : 0xff000000) | rgb);
      }
    }
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, format, bytes);
    image.flush();
    return bytes.toByteArray();
  }

  private static byte[] oriented(byte[] input, String format, int orientation) throws Exception {
    var tiff = ByteBuffer.allocate(26).order(ByteOrder.LITTLE_ENDIAN);
    tiff.putShort((short) 0x4949).putShort((short) 42).putInt(8).putShort((short) 1);
    tiff.putShort((short) 0x0112)
        .putShort((short) 3)
        .putInt(1)
        .putShort((short) orientation)
        .putShort((short) 0)
        .putInt(0);
    var bytes = new ByteArrayOutputStream();
    if (format.equals("jpeg")) {
      bytes.write(input, 0, 2);
      bytes.write(new byte[] {(byte) 0xff, (byte) 0xe1, 0, 34, 'E', 'x', 'i', 'f', 0, 0});
      bytes.write(tiff.array());
      bytes.write(input, 2, input.length - 2);
    } else {
      bytes.write(input, 0, 33);
      var chunk = new ByteArrayOutputStream();
      chunk.write(new byte[] {'e', 'X', 'I', 'f'});
      chunk.write(tiff.array());
      var crc = new CRC32();
      crc.update(chunk.toByteArray());
      bytes.write(ByteBuffer.allocate(4).putInt(26).array());
      bytes.write(chunk.toByteArray());
      bytes.write(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
      bytes.write(input, 33, input.length - 33);
    }
    return bytes.toByteArray();
  }
}
