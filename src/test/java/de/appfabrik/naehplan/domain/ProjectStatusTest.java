package de.appfabrik.naehplan.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectStatusTest {
  @Test
  void threeStableValuesHaveSeparateGermanLabels() {
    assertEquals(
        List.of("planned", "started", "finished"),
        Arrays.stream(ProjectStatus.values()).map(ProjectStatus::storedValue).toList());
    assertEquals(
        List.of("Geplant", "Angefangen", "Fertig"),
        Arrays.stream(ProjectStatus.values()).map(ProjectStatus::displayName).toList());
    for (var status : ProjectStatus.values()) {
      assertSame(status, ProjectStatus.fromStoredValue(status.storedValue()));
    }
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"0", "PLANNED", "Geplant", "archived", " planned "})
  void rejectsUnknownStorageValues(String value) {
    assertThrows(IllegalArgumentException.class, () -> ProjectStatus.fromStoredValue(value));
  }

  @Test
  void rejectsMissingStatusInDraft() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProjectDraft("Schal", "", "", "", "", null, null));
  }
}
