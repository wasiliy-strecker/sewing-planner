package de.appfabrik.naehplan.domain;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** Validated, immutable replacement of the entire project aggregate. */
public record ProjectDraft(
    String name,
    String material,
    String color,
    String location,
    String note,
    Photo photo,
    ProjectStatus status,
    List<ChecklistStep> steps) {
  public ProjectDraft(
      String name,
      String material,
      String color,
      String location,
      String note,
      Photo photo,
      ProjectStatus status) {
    this(name, material, color, location, note, photo, status, List.of());
  }

  /** A new project starts planned unless the user chooses another status. */
  public ProjectDraft(
      String name, String material, String color, String location, String note, Photo photo) {
    this(name, material, color, location, note, photo, ProjectStatus.PLANNED);
  }

  public ProjectDraft {
    if (status == null) {
      throw new IllegalArgumentException("Bitte wähle einen Projektstatus aus.");
    }
    name = name == null ? "" : name.strip();
    if (name.codePoints().noneMatch(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c))) {
      throw new IllegalArgumentException("Bitte gib einen Projektnamen ein.");
    }
    material = material == null ? "" : material.strip();
    color = color == null ? "" : color.strip();
    location = location == null ? "" : location.strip();
    note = note == null ? "" : note;
    steps = List.copyOf(steps);
    var identities = new HashSet<UUID>();
    int previous = -1;
    for (ChecklistStep step : steps) {
      if (!identities.add(step.id()) || step.position() <= previous) {
        throw new IllegalArgumentException(
            "Arbeitsschritte benötigen eindeutige IDs und aufsteigende Positionen.");
      }
      previous = step.position();
    }
  }

  public Project withId(long id) {
    return new Project(id, name, material, color, location, note, photo != null, status, steps);
  }
}
