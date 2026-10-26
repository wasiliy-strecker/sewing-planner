package de.appfabrik.naehplan.domain;

import java.util.List;

/** Lightweight list entry. Image bytes are loaded only for the selected entry. */
public record Project(
    long id,
    String name,
    String material,
    String color,
    String location,
    String note,
    boolean hasPhoto,
    ProjectStatus status,
    List<ChecklistStep> steps) {
  public Project(
      long id,
      String name,
      String material,
      String color,
      String location,
      String note,
      boolean hasPhoto,
      ProjectStatus status) {
    this(id, name, material, color, location, note, hasPhoto, status, List.of());
  }

  public Project {
    java.util.Objects.requireNonNull(status, "status");
    steps = List.copyOf(steps);
  }
}
