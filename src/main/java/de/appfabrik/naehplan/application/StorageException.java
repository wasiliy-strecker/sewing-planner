package de.appfabrik.naehplan.application;

/** Locale-independent storage failure. Only the presentation layer supplies user-facing text. */
public final class StorageException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public enum Code {
    PATH,
    NEWER_SCHEMA,
    OPEN,
    CREATE,
    MISSING,
    PHOTO,
    STATUS,
    OPERATION
  }

  private final Code code;

  public StorageException(Code code, Throwable cause) {
    super(code.name(), cause);
    this.code = code;
  }

  public StorageException(Code code) {
    this(code, null);
  }

  public Code code() {
    return code;
  }
}
