package de.appfabrik.naehplan.application;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.domain.Photo;
import de.appfabrik.naehplan.support.TestImages;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhotoImporterTest {
  @TempDir Path directory;

  @ParameterizedTest
  @ValueSource(strings = {"png", "jpeg"})
  void acceptsDecodedImagesIndependentlyOfExtension(String format) throws Exception {
    byte[] expected = TestImages.image(format, 120, 80, 0x556677);
    Path original = directory.resolve("Foto mit Umlaut Ö.txt");
    Files.write(original, expected);
    Photo photo = PhotoImporter.read(original);
    Files.delete(original);
    assertArrayEquals(expected, photo.bytes());
  }

  @Test
  void bytesAreDefensivelyCopiedAndEqualityUsesContent() throws Exception {
    byte[] expected = TestImages.image("png", 32, 24, 0);
    byte[] input = expected.clone();
    Photo photo = PhotoImporter.decode(input);
    input[0] = 0;
    photo.bytes()[0] = 0;
    assertArrayEquals(expected, photo.bytes());
    assertEquals(photo, new Photo(expected));
    assertEquals(photo.hashCode(), new Photo(expected).hashCode());
    assertArrayEquals(expected, photo.openStream().readAllBytes());
  }

  @Test
  void rejectsDisguisedUnsupportedEmptyAndTruncatedImages() throws Exception {
    Path path = directory.resolve("scheinbar.png");
    for (byte[] invalid :
        new byte[][] {
          new byte[0],
          "Kein Foto".getBytes(java.nio.charset.StandardCharsets.UTF_8),
          TestImages.image("gif", 20, 20, 0),
          Arrays.copyOf(TestImages.image("png", 200, 200, 0), 60),
          Arrays.copyOf(TestImages.image("jpeg", 200, 200, 0), 400)
        }) {
      Files.write(path, invalid);
      assertThrows(PhotoException.class, () -> PhotoImporter.read(path));
    }
  }

  @Test
  void rejectsFileAndMemorySizeOverLimit() throws Exception {
    Path large = directory.resolve("gross.png");
    byte[] oversized = new byte[PhotoImporter.MAX_BYTES + 1];
    Files.write(large, oversized);
    assertThrows(PhotoException.class, () -> PhotoImporter.read(large));
    assertThrows(PhotoException.class, () -> PhotoImporter.decode(oversized));
  }

  @Test
  void rejectsDimensionsAndTotalPixelsBeforeFullDecode() throws Exception {
    byte[] wide = TestImages.image("png", 6001, 1, 0);
    byte[] tall = TestImages.image("png", 1, 6001, 0);
    byte[] pixels = TestImages.image("png", 4001, 4000, 0);
    assertThrows(PhotoException.class, () -> PhotoImporter.decode(wide));
    assertThrows(PhotoException.class, () -> PhotoImporter.decode(tall));
    assertThrows(PhotoException.class, () -> PhotoImporter.decode(pixels));
    assertNotNull(PhotoImporter.decode(TestImages.image("png", 6000, 1, 0)));
    assertNotNull(PhotoImporter.decode(TestImages.image("png", 4000, 4000, 0)));
  }

  @Test
  void rejectsMissingFilesAndDirectoriesWithStableCode() {
    assertThrows(PhotoException.class, () -> PhotoImporter.read(directory));
    PhotoException failure =
        assertThrows(
            PhotoException.class, () -> PhotoImporter.read(directory.resolve("fehlt.png")));
    assertEquals(PhotoException.Code.FILE, failure.code());
  }
}
