package de.appfabrik.naehplan.domain;

/** Stable storage values are independent of enum order and translated labels. */
public enum ProjectStatus {
  PLANNED("planned"),
  STARTED("started"),
  FINISHED("finished");

  private final String storedValue;

  ProjectStatus(String storedValue) {
    this.storedValue = storedValue;
  }

  public String storedValue() {
    return storedValue;
  }

  public static ProjectStatus fromStoredValue(String value) {
    for (ProjectStatus status : values()) {
      if (status.storedValue.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown stored project status.");
  }
}
