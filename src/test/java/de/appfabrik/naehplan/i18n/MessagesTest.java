package de.appfabrik.naehplan.i18n;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.naehplan.application.StorageException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MessagesTest {
  private static PropertyResourceBundle raw(AppLanguage language) throws Exception {
    String suffix = language == AppLanguage.ENGLISH ? "" : "_" + language.tag().replace('-', '_');
    String path = "/de/appfabrik/naehplan/i18n/messages" + suffix + ".properties";
    try (var stream = MessagesTest.class.getResourceAsStream(path)) {
      assertNotNull(stream, path);
      return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
  }

  @ParameterizedTest
  @EnumSource(AppLanguage.class)
  void everyBundleHasAllTranslationsAndCompatiblePlaceholders(AppLanguage language)
      throws Exception {
    var english = raw(AppLanguage.ENGLISH);
    var translated = raw(language);
    Set<String> keys = english.keySet();
    assertEquals(keys, translated.keySet());
    for (String key : keys) {
      String value = translated.getString(key);
      assertFalse(value.isBlank(), key);
      var expected = new MessageFormat(english.getString(key).replace("'", "''"), Locale.US);
      var actual = new MessageFormat(value.replace("'", "''"), language.locale());
      assertEquals(
          expected.getFormatsByArgumentIndex().length,
          actual.getFormatsByArgumentIndex().length,
          key);
      var formats = actual.getFormatsByArgumentIndex();
      Object[] arguments = new Object[formats.length];
      for (int i = 0; i < arguments.length; i++) {
        assertEquals(
            expected.getFormatsByArgumentIndex()[i] instanceof NumberFormat,
            formats[i] instanceof NumberFormat,
            key);
        arguments[i] = formats[i] instanceof NumberFormat ? 42 : "Élise's {book}";
      }
      String output = new Messages(language).text(key, arguments);
      assertFalse(output.contains("{0}"), key);
    }
  }

  @ParameterizedTest
  @EnumSource(AppLanguage.class)
  void allStorageFailuresHaveActionableMessages(AppLanguage language) {
    var messages = new Messages(language);
    for (var code : de.appfabrik.naehplan.application.PhotoException.Code.values()) {
      assertTrue(messages.text("photoError." + code.name()).length() > 20);
    }
    for (var code : StorageException.Code.values()) {
      assertTrue(messages.text("storage." + code.name()).length() > 20);
    }
  }

  @ParameterizedTest
  @EnumSource(AppLanguage.class)
  void statusLabelsAreTranslatedWithoutChangingStorageValues(AppLanguage language) {
    var messages = new Messages(language);
    var labels = new java.util.HashSet<String>();
    for (var status : de.appfabrik.naehplan.domain.ProjectStatus.values()) {
      assertTrue(labels.add(messages.status(status)));
      assertNotEquals(status.storedValue(), messages.status(status));
      assertSame(
          status, de.appfabrik.naehplan.domain.ProjectStatus.fromStoredValue(status.storedValue()));
    }
  }

  @Test
  void formatsCountsWithRegionalGroupingAndPluralRules() {
    assertEquals("1,234 projects", new Messages(AppLanguage.ENGLISH).count(1234, 1234, false));
    assertEquals("1.234 Projekte", new Messages(AppLanguage.GERMAN).count(1234, 1234, false));
    assertEquals("0 projects", new Messages(AppLanguage.ENGLISH).count(0, 0, false));
    assertEquals("0 projet", new Messages(AppLanguage.FRENCH).count(0, 0, false));
    assertEquals("0 projeto", new Messages(AppLanguage.PORTUGUESE).count(0, 0, false));
    assertEquals("0 of 1 project", new Messages(AppLanguage.ENGLISH).count(0, 1, true));
    assertEquals("1 von 2 Projekten", new Messages(AppLanguage.GERMAN).count(1, 2, true));
  }

  @Test
  void preservesUserPunctuationAndAccentsInFormattedMessages() {
    String title = "L'été d'Álvaro {0} – ação";
    assertEquals(
        "« " + title + " » sera définitivement supprimé de votre collection.",
        new Messages(AppLanguage.FRENCH).text("delete.detail", title));
  }

  @Test
  void resolvesSystemVariantsAndUnsupportedLanguagesDeterministically() {
    assertEquals(AppLanguage.GERMAN, AppLanguage.resolve("auto", Locale.forLanguageTag("de-AT")));
    assertEquals(
        AppLanguage.PORTUGUESE, AppLanguage.resolve("auto", Locale.forLanguageTag("pt-PT")));
    assertEquals(AppLanguage.ENGLISH, AppLanguage.resolve("auto", Locale.JAPAN));
    assertEquals(AppLanguage.FRENCH, AppLanguage.resolve("fr-FR", Locale.GERMANY));
    assertEquals(AppLanguage.ENGLISH, AppLanguage.resolve("unknown", Locale.GERMANY));
    assertEquals(AppLanguage.ENGLISH, AppLanguage.resolve(null, Locale.GERMANY));
    assertFalse(AppLanguage.isChoice("de-AT"));
  }
}
