package de.appfabrik.naehplan.application;

import de.appfabrik.naehplan.domain.Photo;
import de.appfabrik.naehplan.domain.Project;
import de.appfabrik.naehplan.domain.ProjectDraft;
import java.util.List;

/** Storage port. Methods complete only after the database operation succeeds. */
public interface ProjectRepository {
  void initialize();

  List<Project> findAll();

  Photo findPhoto(long id);

  Project create(ProjectDraft draft);

  Project update(long id, ProjectDraft draft);

  void delete(long id);
}
