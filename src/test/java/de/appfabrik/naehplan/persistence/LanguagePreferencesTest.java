package de.appfabrik.naehplan.persistence;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.domain.ProjectDraft;
import de.appfabrik.naehplan.i18n.AppLanguage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LanguagePreferencesTest {
  @TempDir Path directory;

  @Test
  void absentPreferencesStartInEnglishWithoutCreatingFiles() throws Exception {
    assertEquals("en-US", new LanguagePreferences(directory).load());
    assertFalse(Files.exists(directory.resolve("language.txt")));
  }

  @ParameterizedTest
  @EnumSource(AppLanguage.class)
  void savedChoiceSurvivesNewInstancesWithoutTouchingExistingCollection(AppLanguage language)
      throws Exception {
    Path database = directory.resolve("naehplan.db");
    var repository = new SqliteProjectRepository(database);
    repository.initialize();
    var project =
        repository.create(
            new ProjectDraft(
                "L'été – Bücher – ação",
                "Élise",
                "Sauge",
                "São Paulo",
                "one\ntwo",
                new de.appfabrik.naehplan.domain.Photo(
                    de.appfabrik.naehplan.support.TestImages.image("png", 16, 16, 0x88aa88)),
                de.appfabrik.naehplan.domain.ProjectStatus.FINISHED));
    byte[] photoBefore = repository.findPhoto(project.id()).bytes();
    byte[] before = Files.readAllBytes(database);
    new LanguagePreferences(directory).save(language.tag());
    assertEquals(language.tag(), new LanguagePreferences(directory).load());
    assertArrayEquals(before, Files.readAllBytes(database));
    assertEquals(List.of(project), new SqliteProjectRepository(database).findAll());
    assertArrayEquals(
        photoBefore, new SqliteProjectRepository(database).findPhoto(project.id()).bytes());
    new LanguagePreferences(directory).save("auto");
    assertEquals("auto", new LanguagePreferences(directory).load());
    try (var files = Files.list(directory)) {
      assertTrue(files.noneMatch(path -> path.toString().endsWith(".tmp")));
    }
  }

  @Test
  void unsupportedOrOversizedSettingFallsBackWithoutOverwritingIt() throws Exception {
    Path file = directory.resolve("language.txt");
    for (String content : List.of("xx-YY", "", "x".repeat(200))) {
      Files.writeString(file, content);
      assertEquals("en-US", new LanguagePreferences(directory).load());
      assertEquals(content, Files.readString(file));
    }
  }

  @Test
  void rejectsInvalidChoiceAndPreservesLastPreference() throws Exception {
    var preferences = new LanguagePreferences(directory);
    preferences.save("fr-FR");
    assertThrows(IllegalArgumentException.class, () -> preferences.save("unsupported"));
    assertEquals("fr-FR", preferences.load());
  }

  @Test
  void blockedDestinationReportsFailureAndCleansTemporaryFiles() throws Exception {
    Path blocked = directory.resolve("language.txt");
    Files.createDirectory(blocked);
    Files.writeString(blocked.resolve("keep.txt"), "preserve");
    assertThrows(IOException.class, () -> new LanguagePreferences(directory).save("fr-FR"));
    assertEquals("preserve", Files.readString(blocked.resolve("keep.txt")));
    try (var files = Files.list(directory)) {
      assertEquals(List.of(blocked), files.toList());
    }
  }
}
