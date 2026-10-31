package de.appfabrik.naehplan.application;

/** Transactional import port, separate from ordinary single-entry operations. */
public interface CollectionImportRepository {
  ImportResult importProjects(ProjectImportSource source);
}
