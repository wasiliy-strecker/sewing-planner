package de.appfabrik.naehplan.persistence;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.application.ProjectService;
import de.appfabrik.naehplan.application.StorageException;
import de.appfabrik.naehplan.domain.Project;
import de.appfabrik.naehplan.domain.ProjectDraft;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteProjectRepositoryTest {
  @TempDir Path directory;
  private Path database;
  private SqliteProjectRepository repository;
  private ProjectService service;

  @BeforeEach
  void setUp() {
    database = directory.resolve("Projekte mit Leerzeichen/sammlung.db");
    repository = new SqliteProjectRepository(database);
    service = new ProjectService(repository);
    assertTrue(service.load().isEmpty());
  }

  @Test
  void createsEditsReopensAndDeletesIndependentProjects() {
    Project first = service.save(null, draft("Der Garten", "Mia", "Regal 2", "Erste\nNotiz"));
    Project second = service.save(null, draft(first));
    assertNotEquals(first.id(), second.id());
    Project edited =
        service.save(first.id(), draft("Der Garten", "Nora", "Regal 3", "Neue\nNotiz"));

    ProjectService reopened = new ProjectService(new SqliteProjectRepository(database));
    assertEquals(List.of(edited, second), ProjectService.search(reopened.load(), ""));
    assertEquals(List.of(edited), ProjectService.search(reopened.load(), "REGAL 3"));
    assertEquals(List.of(edited, second), ProjectService.search(reopened.load(), "garten"));
    reopened.delete(edited.id());
    assertEquals(List.of(second), service.load());
    reopened.delete(second.id());
    assertTrue(new ProjectService(new SqliteProjectRepository(database)).load().isEmpty());
  }

  @Test
  void storesQuotesSqlTextUnicodeAndLongMultilineNotesAsData() {
    ProjectDraft draft =
        draft("Garn'); DROP TABLE projects; --", "O'Connor", "Küche", "ÄÖÜ 日本語 📚\n".repeat(500));
    Project created = repository.create(draft);
    assertEquals(List.of(created), repository.findAll());
    assertEquals(draft, draft(repository.findAll().getFirst()));
    Project emptyOptional = repository.create(draft("Nur Titel", null, null, null));
    assertEquals("", emptyOptional.note());
    assertEquals(2, repository.findAll().size());
  }

  @Test
  void doesNotResurrectMissingEntriesOrTreatMissingDeletesAsSuccess() {
    Project project = repository.create(draft("Atlas", "", "", ""));
    repository.delete(project.id());
    assertThrows(StorageException.class, () -> repository.update(project.id(), draft(project)));
    assertThrows(StorageException.class, () -> repository.delete(project.id()));
    assertTrue(repository.findAll().isEmpty());
  }

  @Test
  void lockedDatabasePreservesPersistedProjectAndRejectsChanges() throws Exception {
    Project original = repository.create(draft("Original", "", "", "Erhalten"));
    try (var blocker = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = blocker.createStatement()) {
      statement.execute("BEGIN EXCLUSIVE");
      assertThrows(StorageException.class, () -> repository.create(draft("Neu", "", "", "")));
      assertThrows(
          StorageException.class,
          () -> repository.update(original.id(), draft("Geändert", "", "", "")));
      assertThrows(StorageException.class, () -> repository.delete(original.id()));
      statement.execute("ROLLBACK");
    }
    assertEquals(List.of(original), repository.findAll());
  }

  @Test
  void rejectsNewerSchemaWithoutChangingIt() throws Exception {
    Project original = repository.create(draft("Vorhanden", "", "", ""));
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute("PRAGMA user_version = 2");
    }
    assertThrows(StorageException.class, repository::initialize);
    assertEquals(List.of(original), repository.findAll());
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement();
        var result = statement.executeQuery("PRAGMA user_version")) {
      assertTrue(result.next());
      assertEquals(2, result.getInt(1));
    }
  }

  @Test
  void refusesCorruptedDatabaseInsteadOfReplacingIt() throws Exception {
    Path corrupted = directory.resolve("corrupted.db");
    byte[] original = "Keine SQLite-Datenbank".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    Files.write(corrupted, original);
    assertThrows(StorageException.class, () -> new SqliteProjectRepository(corrupted).initialize());
    assertArrayEquals(original, Files.readAllBytes(corrupted));
  }

  @Test
  void reportsInvalidDataDirectory() throws Exception {
    Path file = directory.resolve("file");
    Files.writeString(file, "blockiert");
    assertThrows(
        StorageException.class,
        () -> new SqliteProjectRepository(file.resolve("projects.db")).initialize());
  }

  private static ProjectDraft draft(String name, String material, String location, String note) {
    return new ProjectDraft(name, material, "Blau", location, note, null);
  }

  private static ProjectDraft draft(Project project) {
    return new ProjectDraft(
        project.name(),
        project.material(),
        project.color(),
        project.location(),
        project.note(),
        null);
  }
}
