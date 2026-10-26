package de.appfabrik.naehplan.application;

/** Safe import error suitable for a dialog. */
public final class PhotoException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public PhotoException(String message) {
    super(message);
  }

  public PhotoException(String message, Throwable cause) {
    super(message, cause);
  }
}
