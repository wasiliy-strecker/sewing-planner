package de.appfabrik.naehplan.application;

import de.appfabrik.naehplan.domain.Photo;
import de.appfabrik.naehplan.domain.Project;
import de.appfabrik.naehplan.domain.ProjectDraft;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Small application layer, independent of JavaFX and JDBC. */
public final class ProjectService {
  private final ProjectRepository repository;

  public ProjectService(ProjectRepository repository) {
    this.repository = repository;
  }

  public List<Project> load() {
    repository.initialize();
    return repository.findAll();
  }

  public Photo photo(long id) {
    return repository.findPhoto(id);
  }

  public Project save(Long id, ProjectDraft draft) {
    return id == null ? repository.create(draft) : repository.update(id, draft);
  }

  public void delete(long id) {
    repository.delete(id);
  }

  public static List<Project> search(List<Project> projects, String query) {
    String needle = normalize(query == null ? "" : query.strip());
    Collator collator = Collator.getInstance(Locale.GERMAN);
    collator.setStrength(Collator.SECONDARY);
    return projects.stream()
        .filter(project -> matches(project, needle))
        .sorted(Comparator.comparing(Project::name, collator).thenComparingLong(Project::id))
        .toList();
  }

  private static boolean matches(Project project, String needle) {
    return normalize(project.name()).contains(needle)
        || normalize(project.material()).contains(needle)
        || normalize(project.color()).contains(needle)
        || normalize(project.location()).contains(needle)
        || normalize(project.note()).contains(needle)
        || project.steps().stream().anyMatch(step -> normalize(step.text()).contains(needle));
  }

  private static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
  }
}
