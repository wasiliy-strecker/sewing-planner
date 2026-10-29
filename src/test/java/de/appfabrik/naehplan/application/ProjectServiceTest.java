package de.appfabrik.naehplan.application;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.domain.Project;
import de.appfabrik.naehplan.domain.ProjectDraft;
import de.appfabrik.naehplan.domain.ProjectStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectServiceTest {
  private final Project first =
      new Project(
          1,
          "Projekte im Frühling",
          "Änne Müller",
          "Grün",
          "Wohnzimmer, Regal 2",
          "Geschenk\nvon Ömer. 100% schön_",
          false,
          ProjectStatus.PLANNED);
  private final Project duplicate =
      new Project(2, first.name(), "", "", "", "", false, ProjectStatus.PLANNED);
  private final Project other =
      new Project(3, "Alpaka", "Ben", "Rot", "Flur", "", false, ProjectStatus.PLANNED);

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n", "\u2003", "\u00a0"})
  void rejectsMissingOrWhitespaceOnlyTitle(String name) {
    assertThrows(IllegalArgumentException.class, () -> draft(name, "", "", ""));
  }

  @Test
  void normalizesSingleLineInputButPreservesNoteFormatting() {
    var draft = draft("  Atlas  ", null, " Flur ", "  Notiz\n\nEnde  ");
    assertEquals("Atlas", draft.name());
    assertEquals("", draft.material());
    var colored = new ProjectDraft("Merino", null, " Grün ", null, null, null);
    assertEquals("Grün", colored.color());
    assertEquals("", new ProjectDraft("Merino", null, null, null, null, null).color());
    assertEquals("Flur", draft.location());
    assertEquals("  Notiz\n\nEnde  ", draft.note());
    assertEquals("", draft("Atlas", null, null, null).note());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "PROJEKTE",
        "äNNE",
        "MÜLLER",
        "WOHNZIMMER",
        "regal 2",
        "öMER",
        "geschenk",
        "100%",
        "schön_",
        "FRU\u0308HLING",
        "GRÜN"
      })
  void searchesEveryFieldWithUnicodeCaseHandlingAndLiteralSubstrings(String query) {
    assertEquals(List.of(first), ProjectService.search(List.of(first, other), query));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\n\t"})
  void emptyQueryShowsEntireCollectionSortedByTitleThenId(String query) {
    assertEquals(
        List.of(other, first, duplicate),
        ProjectService.search(List.of(duplicate, first, other), query));
  }

  @Test
  void preservesDuplicateIdentityAndDoesNotModifyInput() {
    List<Project> input = List.of(duplicate, other, first);
    assertEquals(List.of(first, duplicate), ProjectService.search(input, "Projekte"));
    assertEquals(List.of(duplicate, other, first), input);
    assertTrue(ProjectService.search(input, "nicht vorhanden").isEmpty());
  }

  @Test
  void propagatesFailuresWithoutReportingSuccessfulSaveOrDelete() {
    ProjectService service = new ProjectService(new FailingRepository());
    ProjectDraft draft =
        draft("Unveränderte Eingabe", "Stoff / Material", "Ort", "Mehrzeilig\nbleibt erhalten");
    assertThrows(StorageException.class, service::load);
    assertThrows(StorageException.class, () -> service.save(null, draft));
    assertThrows(StorageException.class, () -> service.save(1L, draft));
    assertThrows(StorageException.class, () -> service.delete(1));
    assertEquals("Mehrzeilig\nbleibt erhalten", draft.note());
  }

  private static ProjectDraft draft(String name, String material, String location, String note) {
    return new ProjectDraft(name, material, "", location, note, null);
  }

  private static final class FailingRepository implements ProjectRepository {
    @Override
    public void initialize() {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public List<Project> findAll() {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public de.appfabrik.naehplan.domain.Photo findPhoto(long id) {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public Project create(ProjectDraft draft) {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public Project update(long id, ProjectDraft draft) {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public void delete(long id) {
      throw new StorageException(StorageException.Code.OPERATION);
    }
  }
}
