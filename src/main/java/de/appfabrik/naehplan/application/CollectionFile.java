package de.appfabrik.naehplan.application;

import de.appfabrik.naehplan.domain.ProjectDraft;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Writes a collection to a new local file. Entries may be loaded lazily. */
public interface CollectionFile {
  void write(Path target, Iterable<ProjectDraft> projects) throws IOException;

  void read(Path source, Consumer<ProjectDraft> accept);
}
