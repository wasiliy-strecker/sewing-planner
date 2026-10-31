package de.appfabrik.naehplan.application;

/** Import failures that the UI translates without displaying file contents. */
public final class ImportException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public enum Code {
    READ,
    FORMAT,
    VERSION,
    LIMIT,
    PHOTO
  }

  private final Code code;

  public ImportException(Code code) {
    this(code, null);
  }

  public ImportException(Code code, Throwable cause) {
    super(code.name(), cause);
    this.code = code;
  }

  public Code code() {
    return code;
  }
}
