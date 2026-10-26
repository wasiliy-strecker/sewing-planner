package de.appfabrik.naehplan.persistence;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.application.PhotoImporter;
import de.appfabrik.naehplan.application.ProjectService;
import de.appfabrik.naehplan.application.StorageException;
import de.appfabrik.naehplan.domain.ChecklistStep;
import de.appfabrik.naehplan.domain.Photo;
import de.appfabrik.naehplan.domain.ProjectDraft;
import de.appfabrik.naehplan.domain.ProjectStatus;
import de.appfabrik.naehplan.support.TestImages;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class ChecklistPersistenceTest {
  @TempDir Path directory;
  private Path database;
  private ProjectService service;
  private Photo photo;

  @BeforeEach
  void setUp() throws Exception {
    database = directory.resolve("naehplan.db");
    service = reopen();
    service.load();
    photo = PhotoImporter.decode(TestImages.image("png", 80, 60, 0x74536f));
  }

  private ProjectService reopen() {
    return new ProjectService(new SqliteProjectRepository(database));
  }

  @ParameterizedTest
  @EnumSource(ProjectStatus.class)
  void stepsCanBeRenamedCompletedReopenedRemovedAndAppendedWithoutAffectingStatus(
      ProjectStatus status) {
    var first = step("Nähen", false, 0);
    var second = step("Nähen", false, 1);
    var saved = service.save(null, draft(status, List.of(first, second)));
    assertEquals(saved, reopen().load().getFirst());
    var completed = new ChecklistStep(first.id(), "Nähen", true, first.position());
    service.save(
        saved.id(),
        draft(status, List.of(completed, new ChecklistStep(second.id(), "Nähen", true, 1))));
    assertEquals(status, reopen().load().getFirst().status());
    var renamed = new ChecklistStep(first.id(), "Saum nähen", false, first.position());
    var appended = step("Bügeln", false, 2);
    var changed = service.save(saved.id(), draft(status, List.of(renamed, appended)));
    var loaded = reopen().load().getFirst();
    assertEquals(changed, loaded);
    assertEquals(List.of(renamed, appended), loaded.steps());
    assertEquals(first.id(), loaded.steps().getFirst().id());
    assertEquals(status, loaded.status());
    assertEquals(saved.name(), loaded.name());
    assertEquals(saved.material(), loaded.material());
    assertEquals(saved.color(), loaded.color());
    assertEquals(saved.location(), loaded.location());
    assertEquals(saved.note(), loaded.note());
    assertEquals(photo, reopen().photo(saved.id()));
    service.save(saved.id(), draft(status, List.of()));
    assertTrue(reopen().load().getFirst().steps().isEmpty());
    assertEquals(status, reopen().load().getFirst().status());
  }

  @Test
  void everyStatusTransitionPreservesOpenAndCompletedSteps() {
    var steps = List.of(step("Zuschneiden", true, 0), step("Nähen", false, 3));
    var saved = service.save(null, draft(ProjectStatus.PLANNED, steps));
    for (ProjectStatus from : ProjectStatus.values()) {
      service.save(saved.id(), draft(from, steps));
      for (ProjectStatus to : ProjectStatus.values()) {
        service.save(saved.id(), draft(to, steps));
        assertEquals(steps, reopen().load().getFirst().steps());
        assertEquals(to, reopen().load().getFirst().status());
        service.save(saved.id(), draft(from, steps));
      }
    }
  }

  @Test
  void independentDuplicateProjectsAndStepsSurviveDeletionOfTheOtherProject() throws Exception {
    var first =
        service.save(
            null,
            draft(
                ProjectStatus.PLANNED, List.of(step("Nähen", false, 0), step("Nähen", false, 1))));
    var second = service.save(null, draft(ProjectStatus.PLANNED, List.of(step("Nähen", false, 0))));
    assertNotEquals(first.id(), second.id());
    assertEquals(2, ProjectService.search(reopen().load(), "NÄHEN").size());
    service.delete(first.id());
    assertEquals(List.of(second), reopen().load());
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement();
        var result =
            statement.executeQuery(
                "SELECT project_id, COUNT(*) FROM checklist_steps GROUP BY project_id")) {
      assertTrue(result.next());
      assertEquals(second.id(), result.getLong(1));
      assertEquals(1, result.getInt(2));
      assertFalse(result.next());
    }
  }

  @Test
  void lateStepFailureRollsBackAllProjectFieldsAndEveryStepThenAllowsExactRetry() throws Exception {
    var original =
        service.save(null, draft(ProjectStatus.PLANNED, List.of(step("Original", false, 0))));
    var newPhoto = PhotoImporter.decode(TestImages.image("jpeg", 70, 50, 0x00aaff));
    var input =
        new ProjectDraft(
            "Geändert",
            "Leinen",
            "Blau",
            "Box 2",
            "Neu\nNotiz",
            newPhoto,
            ProjectStatus.FINISHED,
            List.of(step("Erster neuer Schritt", true, 0), step("Fehler", false, 1)));
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute(
          "CREATE TRIGGER fail_step AFTER INSERT ON checklist_steps WHEN NEW.position = 1 BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END");
      assertThrows(StorageException.class, () -> service.save(original.id(), input));
      assertEquals(List.of(original), reopen().load());
      assertEquals(photo, reopen().photo(original.id()));
      assertEquals(2, input.steps().size());
      assertThrows(StorageException.class, () -> service.save(null, input));
      assertEquals(List.of(original), reopen().load());
      statement.execute("DROP TRIGGER fail_step");
    }
    assertEquals(input.withId(original.id()), service.save(original.id(), input));
    assertEquals(input.withId(original.id()), reopen().load().getFirst());
    assertEquals(newPhoto, reopen().photo(original.id()));
  }

  @Test
  void lockedDatabasePreservesChecklistAndDraft() throws Exception {
    var original =
        service.save(null, draft(ProjectStatus.FINISHED, List.of(step("Nähen", false, 0))));
    var step = original.steps().getFirst();
    var input =
        draft(
            ProjectStatus.FINISHED,
            List.of(new ChecklistStep(step.id(), step.text(), true, step.position())));
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute("BEGIN EXCLUSIVE");
      assertThrows(StorageException.class, () -> service.save(original.id(), input));
      statement.execute("ROLLBACK");
    }
    assertEquals(original, reopen().load().getFirst());
    assertTrue(input.steps().getFirst().completed());
    service.save(original.id(), input);
    assertTrue(reopen().load().getFirst().steps().getFirst().completed());
  }

  @Test
  void aStepIdentityCannotBeMovedToAnotherProjectBySavingACollidingDraft() {
    var shared = step("Nähen", false, 0);
    var first = service.save(null, draft(ProjectStatus.PLANNED, List.of(shared)));
    var second = service.save(null, draft(ProjectStatus.PLANNED, List.of()));
    assertThrows(
        StorageException.class,
        () -> service.save(second.id(), draft(ProjectStatus.STARTED, List.of(shared))));
    assertEquals(List.of(first, second), reopen().load());
  }

  @Test
  void databaseEnforcesPositionBooleanTextAndForeignKeyConstraints() throws Exception {
    var saved = service.save(null, draft(ProjectStatus.PLANNED, List.of(step("Nähen", false, 0))));
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
      for (String assignment :
          List.of(
              "text = ''",
              "text = '   '",
              "completed = 2",
              "completed = NULL",
              "position = -1",
              "position = 0.5",
              "project_id = 9999")) {
        assertThrows(
            SQLException.class,
            () -> statement.executeUpdate("UPDATE checklist_steps SET " + assignment));
      }
      assertThrows(
          SQLException.class,
          () ->
              statement.executeUpdate(
                  "INSERT INTO checklist_steps (id, project_id, text, position) VALUES ('other', "
                      + saved.id()
                      + ", 'Gleich', 0)"));
    }
    assertEquals(saved, reopen().load().getFirst());
  }

  @ParameterizedTest
  @ValueSource(strings = {"completed = 9", "text = '   '", "id = 'broken'", "position = -1"})
  void reportsExternallyCorruptedStepsWithoutWritingRepairs(String assignment) throws Exception {
    service.save(null, draft(ProjectStatus.PLANNED, List.of(step("Nähen", false, 0))));
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute("PRAGMA ignore_check_constraints = ON");
      statement.executeUpdate("UPDATE checklist_steps SET " + assignment);
      assertThrows(StorageException.class, () -> reopen().load());
      try (var result =
          statement.executeQuery("SELECT COUNT(*) FROM checklist_steps WHERE " + assignment)) {
        assertTrue(result.next());
        assertEquals(1, result.getInt(1));
      }
    }
  }

  private ProjectDraft draft(ProjectStatus status, List<ChecklistStep> steps) {
    return new ProjectDraft(
        "Tasche", "Baumwolle", "Grün", "Box 1", "Für Ömer\nMit Innenfutter", photo, status, steps);
  }

  private static ChecklistStep step(String text, boolean completed, int position) {
    return new ChecklistStep(UUID.randomUUID(), text, completed, position);
  }
}
