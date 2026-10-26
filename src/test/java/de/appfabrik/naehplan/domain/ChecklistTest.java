package de.appfabrik.naehplan.domain;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.application.ProjectService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ChecklistTest {
  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n", "\u00a0", "\u2003", "\u202f"})
  void rejectsEmptyStepText(String text) {
    assertThrows(
        IllegalArgumentException.class, () -> new ChecklistStep(UUID.randomUUID(), text, false, 0));
  }

  @Test
  void independentIdentityAndExplicitPositionAreRequired() {
    var id = UUID.randomUUID();
    assertThrows(IllegalArgumentException.class, () -> new ChecklistStep(null, "Nähen", false, 0));
    assertThrows(IllegalArgumentException.class, () -> new ChecklistStep(id, "Nähen", false, -1));
    var first = new ChecklistStep(id, " Nähen ", false, 0);
    var second = new ChecklistStep(UUID.randomUUID(), "Nähen", true, 1);
    assertEquals("Nähen", first.text());
    assertNotEquals(first.id(), second.id());
    assertEquals(2, draft(List.of(first, second)).steps().size());
    assertThrows(IllegalArgumentException.class, () -> draft(List.of(first, first)));
    assertThrows(
        IllegalArgumentException.class,
        () -> draft(List.of(first, new ChecklistStep(id, "Anders", true, 1))));
    assertThrows(
        IllegalArgumentException.class,
        () -> draft(List.of(first, new ChecklistStep(second.id(), "Anders", false, 0))));
    assertThrows(IllegalArgumentException.class, () -> draft(List.of(second, first)));
  }

  @Test
  void emptyDefaultAndDefensiveSnapshotsProtectTheSavedState() {
    assertTrue(new ProjectDraft("Tasche", "", "", "", "", null).steps().isEmpty());
    List<ChecklistStep> input = new ArrayList<>();
    input.add(new ChecklistStep(UUID.randomUUID(), "Zuschneiden", false, 2));
    var draft = draft(input);
    var project = draft.withId(1);
    input.clear();
    assertEquals(1, project.steps().size());
    assertThrows(UnsupportedOperationException.class, () -> draft.steps().clear());
    assertThrows(UnsupportedOperationException.class, () -> project.steps().clear());
    var changed =
        draft(List.of(new ChecklistStep(project.steps().getFirst().id(), "Zuschneiden", true, 2)));
    assertNotEquals(draft, changed);
    assertEquals(ProjectStatus.PLANNED, changed.status());
    assertFalse(draft.steps().getFirst().completed());
  }

  @ParameterizedTest
  @ValueSource(strings = {"NÄHEN", "nähen", "NA\u0308HEN", "ÖFFNEN", "öffnen", "100%_"})
  void stepSearchFindsEachProjectOnlyOnceRegardlessOfDuplicateMatches(String query) {
    var matching =
        draft(
                List.of(
                    new ChecklistStep(UUID.randomUUID(), "Nähen und öffnen 100%_", false, 0),
                    new ChecklistStep(UUID.randomUUID(), "Nähen und öffnen 100%_", true, 1)))
            .withId(1);
    var duplicateName = draft(List.of()).withId(2);
    assertEquals(List.of(matching), ProjectService.search(List.of(matching, duplicateName), query));
    assertEquals(
        List.of(matching, duplicateName),
        ProjectService.search(List.of(matching, duplicateName), ""));
  }

  private static ProjectDraft draft(List<ChecklistStep> steps) {
    return new ProjectDraft("Tasche", "", "", "", "", null, ProjectStatus.PLANNED, steps);
  }
}
