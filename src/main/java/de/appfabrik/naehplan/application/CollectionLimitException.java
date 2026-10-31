package de.appfabrik.naehplan.application;

/** A collection boundary, translated by the UI without losing the current draft. */
public final class CollectionLimitException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public enum Code {
    ENTRIES,
    TEXT,
    STEPS,
    FILE_SIZE
  }

  private final Code code;

  public CollectionLimitException(Code code) {
    super(code.name());
    this.code = code;
  }

  public Code code() {
    return code;
  }
}
