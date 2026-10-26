package de.appfabrik.naehplan.persistence;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.application.PhotoImporter;
import de.appfabrik.naehplan.application.ProjectService;
import de.appfabrik.naehplan.application.StorageException;
import de.appfabrik.naehplan.domain.Photo;
import de.appfabrik.naehplan.domain.ProjectDraft;
import de.appfabrik.naehplan.support.TestImages;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PhotoPersistenceTest {
  @TempDir Path directory;
  private Path database;
  private ProjectService service;
  private Photo first;
  private Photo second;

  @BeforeEach
  void setUp() throws Exception {
    database = directory.resolve("naehplan.db");
    service = new ProjectService(new SqliteProjectRepository(database));
    service.load();
    first = PhotoImporter.decode(TestImages.image("png", 120, 80, 0x937aa0));
    second = PhotoImporter.decode(TestImages.image("jpeg", 80, 120, 0x8cab98));
  }

  @Test
  void addsReplacesRemovesAndDeletesOneOwnedPhoto() throws Exception {
    var project = service.save(null, draft("Merino", null));
    assertFalse(project.hasPhoto());
    assertNull(service.photo(project.id()));
    Path source = directory.resolve("original.png");
    Files.write(source, first.bytes());
    var withPhoto = service.save(project.id(), draft("Merino", PhotoImporter.read(source)));
    assertTrue(withPhoto.hasPhoto());
    Files.delete(source);
    ProjectService reopened = new ProjectService(new SqliteProjectRepository(database));
    assertEquals(withPhoto, reopened.load().getFirst());
    assertEquals(first, reopened.photo(project.id()));
    Path untouchedSource = directory.resolve("ersatz.jpg");
    Files.write(untouchedSource, second.bytes());
    reopened.save(project.id(), draft("Merino geändert", PhotoImporter.read(untouchedSource)));
    assertEquals(second, service.photo(project.id()));
    var removed = service.save(project.id(), draft("Ohne Foto", null));
    assertFalse(removed.hasPhoto());
    assertNull(reopened.photo(project.id()));
    assertArrayEquals(second.bytes(), Files.readAllBytes(untouchedSource));
    service.save(project.id(), draft("Mit Foto löschen", second));
    service.delete(project.id());
    assertTrue(reopened.load().isEmpty());
    assertThrows(StorageException.class, () -> reopened.photo(project.id()));
  }

  @Test
  void lockRejectsCreateReplaceRemoveAndDeleteWithoutPartialChanges() throws Exception {
    var saved = service.save(null, draft("Original", first));
    var replacement = draft("Neue Texte", second);
    try (var lock = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = lock.createStatement()) {
      statement.execute("BEGIN EXCLUSIVE");
      assertThrows(StorageException.class, () -> service.save(null, replacement));
      assertThrows(StorageException.class, () -> service.save(saved.id(), replacement));
      assertThrows(
          StorageException.class, () -> service.save(saved.id(), draft("Entfernen", null)));
      assertThrows(StorageException.class, () -> service.delete(saved.id()));
      statement.execute("ROLLBACK");
    }
    assertEquals(saved, service.load().getFirst());
    assertEquals(1, service.load().size());
    assertEquals(first, service.photo(saved.id()));
    assertEquals(second, replacement.photo());
    service.save(saved.id(), replacement);
    assertEquals(second, service.photo(saved.id()));
  }

  @Test
  void statementFailureRollsBackTextAndBlobTogether() throws Exception {
    var saved = service.save(null, draft("Original", first));
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute(
          "CREATE TRIGGER fail_save AFTER UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END");
    }
    assertThrows(StorageException.class, () -> service.save(saved.id(), draft("Geändert", second)));
    assertEquals(saved, service.load().getFirst());
    assertEquals(first, service.photo(saved.id()));
  }

  private static ProjectDraft draft(String name, Photo photo) {
    return new ProjectDraft(name, "Wollwerk", "Grün", "Box 1", "Notiz\nZweite Zeile", photo);
  }
}
