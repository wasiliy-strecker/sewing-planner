package de.appfabrik.naehplan.application;

/** Locale-independent import failure. The UI supplies the translated explanation. */
public final class PhotoException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public enum Code {
    FILE,
    READ,
    DIMENSIONS,
    CORRUPT,
    INVALID,
    SIZE,
    DISPLAY
  }

  private final Code code;

  public PhotoException(Code code) {
    this(code, null);
  }

  public PhotoException(Code code, Throwable cause) {
    super(code.name(), cause);
    this.code = code;
  }

  public Code code() {
    return code;
  }
}
