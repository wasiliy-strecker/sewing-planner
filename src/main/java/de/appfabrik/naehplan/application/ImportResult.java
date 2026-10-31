package de.appfabrik.naehplan.application;

import de.appfabrik.naehplan.domain.Project;
import java.util.List;

/** The committed collection, with counts of new and already present entries. */
public record ImportResult(int added, int skipped, List<Project> projects) {
  public ImportResult {
    projects = List.copyOf(projects);
  }
}
