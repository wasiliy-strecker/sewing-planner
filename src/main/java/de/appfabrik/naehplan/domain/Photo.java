package de.appfabrik.naehplan.domain;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

/** Owned immutable bytes. No reference to the original file is kept. */
public final class Photo {
  private final byte[] data;

  public Photo(byte[] data) {
    this.data = data.clone();
  }

  public byte[] bytes() {
    return data.clone();
  }

  public InputStream openStream() {
    return new ByteArrayInputStream(data);
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof Photo photo && Arrays.equals(data, photo.data);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }
}
