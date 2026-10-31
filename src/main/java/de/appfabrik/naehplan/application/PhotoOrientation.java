package de.appfabrik.naehplan.application;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Reads only EXIF's primary orientation, bounded to its JPEG segment or PNG chunk. */
final class PhotoOrientation {
  private PhotoOrientation() {}

  static int read(byte[] bytes, String format) {
    return format.equals("jpeg") ? jpeg(bytes) : png(bytes);
  }

  private static int jpeg(byte[] bytes) {
    int offset = 2; // The image decoder already validated the JPEG signature.
    while (offset < bytes.length) {
      if (unsigned(bytes[offset++]) != 0xff) {
        return 1;
      }
      while (offset < bytes.length && unsigned(bytes[offset]) == 0xff) {
        offset++;
      }
      if (offset >= bytes.length) {
        return 1;
      }
      int marker = unsigned(bytes[offset++]);
      if (marker == 0xda || marker == 0xd9) {
        return 1; // Stop at scan data or end of image.
      }
      if (marker == 0x01 || (marker >= 0xd0 && marker <= 0xd8)) {
        continue;
      }
      if (offset > bytes.length - 2) {
        return 1;
      }
      int length = (unsigned(bytes[offset]) << 8) | unsigned(bytes[offset + 1]);
      if (length < 2 || length > bytes.length - offset) {
        return 1;
      }
      if (marker == 0xe1
          && length >= 8
          && bytes[offset + 2] == 'E'
          && bytes[offset + 3] == 'x'
          && bytes[offset + 4] == 'i'
          && bytes[offset + 5] == 'f'
          && bytes[offset + 6] == 0
          && bytes[offset + 7] == 0) {
        return tiff(bytes, offset + 8, length - 8);
      }
      offset += length;
    }
    return 1;
  }

  private static int png(byte[] bytes) {
    int offset = 8; // Skip the PNG signature, after successful image decoding.
    while (offset <= bytes.length - 12) {
      long length = Integer.toUnsignedLong(ByteBuffer.wrap(bytes, offset, 4).getInt());
      if (length > bytes.length - offset - 12) {
        return 1;
      }
      if (bytes[offset + 4] == 'e'
          && bytes[offset + 5] == 'X'
          && bytes[offset + 6] == 'I'
          && bytes[offset + 7] == 'f') {
        return tiff(bytes, offset + 8, (int) length);
      }
      offset += (int) length + 12;
    }
    return 1;
  }

  private static int tiff(byte[] bytes, int offset, int length) {
    if (length < 8) {
      return 1;
    }
    var data = ByteBuffer.wrap(bytes, offset, length).slice();
    int byteOrder = Short.toUnsignedInt(data.getShort(0));
    if (byteOrder == 0x4949) {
      data.order(ByteOrder.LITTLE_ENDIAN);
    } else if (byteOrder != 0x4d4d) {
      return 1;
    }
    if (data.getShort(2) != 42) {
      return 1;
    }
    long directory = Integer.toUnsignedLong(data.getInt(4));
    if (directory < 8 || directory > length - 2) {
      return 1;
    }
    int count = Short.toUnsignedInt(data.getShort((int) directory));
    int start = (int) directory + 2;
    if (count > (length - start) / 12) {
      return 1;
    }
    for (int index = 0; index < count; index++) {
      int entry = start + index * 12;
      if (Short.toUnsignedInt(data.getShort(entry)) == 0x0112
          && data.getShort(entry + 2) == 3
          && data.getInt(entry + 4) == 1) {
        int orientation = Short.toUnsignedInt(data.getShort(entry + 8));
        return orientation >= 1 && orientation <= 8 ? orientation : 1;
      }
    }
    return 1;
  }

  private static int unsigned(byte value) {
    return Byte.toUnsignedInt(value);
  }
}
