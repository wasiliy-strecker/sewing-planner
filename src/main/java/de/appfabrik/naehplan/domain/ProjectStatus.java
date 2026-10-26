package de.appfabrik.naehplan.domain;

/** Stable storage values are independent of enum order and German labels. */
public enum ProjectStatus {
  PLANNED("planned", "Geplant"),
  STARTED("started", "Angefangen"),
  FINISHED("finished", "Fertig");

  private final String storedValue;
  private final String displayName;

  ProjectStatus(String storedValue, String displayName) {
    this.storedValue = storedValue;
    this.displayName = displayName;
  }

  public String storedValue() {
    return storedValue;
  }

  public String displayName() {
    return displayName;
  }

  public static ProjectStatus fromStoredValue(String value) {
    for (ProjectStatus status : values()) {
      if (status.storedValue.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unbekannter gespeicherter Projektstatus.");
  }
}
