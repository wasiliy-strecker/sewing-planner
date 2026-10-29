package de.appfabrik.naehplan.persistence;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.application.PhotoImporter;
import de.appfabrik.naehplan.application.ProjectService;
import de.appfabrik.naehplan.application.StorageException;
import de.appfabrik.naehplan.domain.Photo;
import de.appfabrik.naehplan.domain.ProjectDraft;
import de.appfabrik.naehplan.domain.ProjectStatus;
import de.appfabrik.naehplan.support.TestImages;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProjectStatusPersistenceTest {
  @TempDir Path directory;
  private Path database;
  private ProjectService service;
  private Photo photo;

  @BeforeEach
  void setUp() throws Exception {
    database = directory.resolve("naehplan.db");
    service = reopen();
    service.load();
    photo = PhotoImporter.decode(TestImages.image("png", 100, 80, 0x436a70));
  }

  private ProjectService reopen() {
    return new ProjectService(new SqliteProjectRepository(database));
  }

  @Test
  void newProjectDefaultsToPlannedInDomainAndDatabase() throws Exception {
    var draft = new ProjectDraft("Schal", "", "", "", "", null);
    assertEquals(ProjectStatus.PLANNED, draft.status());
    var saved = service.save(null, draft);
    assertEquals(ProjectStatus.PLANNED, saved.status());
    assertEquals(saved, reopen().load().getFirst());
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.executeUpdate("INSERT INTO projects (name) VALUES ('Mütze')");
      try (var result = statement.executeQuery("SELECT status FROM projects")) {
        while (result.next()) {
          assertEquals("planned", result.getString(1));
        }
      }
    }
  }

  @ParameterizedTest
  @EnumSource(ProjectStatus.class)
  void reopensEveryStatusAndStoresStableText(ProjectStatus status) throws Exception {
    var saved = service.save(null, draft(status, photo));
    assertEquals(saved, reopen().load().getFirst());
    assertEquals(photo, reopen().photo(saved.id()));
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement();
        var result = statement.executeQuery("SELECT status, typeof(status) FROM projects")) {
      assertTrue(result.next());
      assertEquals(status.storedValue(), result.getString(1));
      assertEquals("text", result.getString(2));
      assertNotEquals(
          new de.appfabrik.naehplan.i18n.Messages(de.appfabrik.naehplan.i18n.AppLanguage.GERMAN)
              .status(status),
          result.getString(1));
    }
  }

  @ParameterizedTest
  @EnumSource(ProjectStatus.class)
  void everyStatusCanChangeToEveryOtherWithoutChangingTextsPhotoOrId(ProjectStatus initial) {
    var saved = service.save(null, draft(initial, photo));
    for (ProjectStatus target : ProjectStatus.values()) {
      var changed = service.save(saved.id(), draft(target, service.photo(saved.id())));
      var loaded = reopen().load().getFirst();
      assertEquals(changed, loaded);
      assertEquals(target, loaded.status());
      assertEquals(saved.id(), loaded.id());
      assertEquals(saved.name(), loaded.name());
      assertEquals(saved.material(), loaded.material());
      assertEquals(saved.color(), loaded.color());
      assertEquals(saved.location(), loaded.location());
      assertEquals(saved.note(), loaded.note());
      assertEquals(photo, reopen().photo(saved.id()));
      service.save(saved.id(), draft(initial, photo));
    }
  }

  @Test
  void abortAfterUpdateRollsBackTextsPhotoAndStatusAndAllowsRetry() throws Exception {
    var original = service.save(null, draft(ProjectStatus.PLANNED, photo));
    var replacement = PhotoImporter.decode(TestImages.image("jpeg", 80, 100, 0x884422));
    var input =
        new ProjectDraft(
            "Neuer Name",
            "Baumwolle",
            "Rot",
            "Box 2",
            "Neue\nNotiz",
            replacement,
            ProjectStatus.FINISHED);
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute(
          "CREATE TRIGGER fail_save AFTER UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END");
      assertThrows(StorageException.class, () -> service.save(original.id(), input));
      assertEquals(original, reopen().load().getFirst());
      assertEquals(photo, service.photo(original.id()));
      assertEquals(ProjectStatus.FINISHED, input.status());
      assertEquals(replacement, input.photo());
      statement.execute("DROP TRIGGER fail_save");
    }
    assertEquals(input.withId(original.id()), service.save(original.id(), input));
    assertEquals(replacement, reopen().photo(original.id()));
  }

  @Test
  void failedInsertDoesNotLeavePartialProject() throws Exception {
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute(
          "CREATE TRIGGER fail_create AFTER INSERT ON projects BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END");
      assertThrows(
          StorageException.class, () -> service.save(null, draft(ProjectStatus.STARTED, photo)));
      assertTrue(reopen().load().isEmpty());
    }
  }

  @Test
  void lockedDatabaseRejectsStatusOnlyChangeWithoutLosingOriginal() throws Exception {
    var original = service.save(null, draft(ProjectStatus.FINISHED, photo));
    var input = draft(ProjectStatus.STARTED, photo);
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute("BEGIN EXCLUSIVE");
      assertThrows(StorageException.class, () -> service.save(original.id(), input));
      statement.execute("ROLLBACK");
    }
    assertEquals(original, reopen().load().getFirst());
    assertEquals(photo, service.photo(original.id()));
    assertEquals(input.withId(original.id()), service.save(original.id(), input));
  }

  @Test
  void databaseRejectsNullUnknownLabelsAndOrdinals() throws Exception {
    var original = service.save(null, draft(ProjectStatus.STARTED, photo));
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.prepareStatement("UPDATE projects SET status = ?")) {
      for (String invalid : new String[] {null, "", "Geplant", "0", "archived"}) {
        statement.setString(1, invalid);
        assertThrows(SQLException.class, statement::executeUpdate);
      }
    }
    assertEquals(original, reopen().load().getFirst());
  }

  @Test
  void externallyCorruptedStatusIsReportedWithoutSilentlyResettingIt() throws Exception {
    service.save(null, draft(ProjectStatus.STARTED, photo));
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute("PRAGMA ignore_check_constraints = ON");
      statement.executeUpdate("UPDATE projects SET status = 'unknown'");
    }
    assertThrows(StorageException.class, () -> reopen().load());
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement();
        var result = statement.executeQuery("SELECT status FROM projects")) {
      assertTrue(result.next());
      assertEquals("unknown", result.getString(1));
    }
  }

  private static ProjectDraft draft(ProjectStatus status, Photo photo) {
    return new ProjectDraft(
        "Herbstschal", "Merino", "Grün", "Box 1", "  Für Ömer\nZweite Zeile  ", photo, status);
  }
}
