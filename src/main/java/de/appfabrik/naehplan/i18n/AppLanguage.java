package de.appfabrik.naehplan.i18n;

import java.util.Arrays;
import java.util.Locale;

public enum AppLanguage {
  GERMAN("de-DE", "Deutsch"),
  ENGLISH("en-US", "English (United States)"),
  SPANISH("es-ES", "Español"),
  FRENCH("fr-FR", "Français"),
  ITALIAN("it-IT", "Italiano"),
  PORTUGUESE("pt-BR", "Português (Brasil)");

  public static final String AUTOMATIC = "auto";
  private final String tag;
  private final String nativeName;

  AppLanguage(String tag, String nativeName) {
    this.tag = tag;
    this.nativeName = nativeName;
  }

  public String tag() {
    return tag;
  }

  public String nativeName() {
    return nativeName;
  }

  public Locale locale() {
    return Locale.forLanguageTag(tag);
  }

  public static AppLanguage detect(Locale systemLocale) {
    return Arrays.stream(values())
        .filter(language -> language.locale().getLanguage().equals(systemLocale.getLanguage()))
        .findFirst()
        .orElse(ENGLISH);
  }

  public static boolean isChoice(String choice) {
    return AUTOMATIC.equals(choice)
        || Arrays.stream(values()).anyMatch(language -> language.tag.equals(choice));
  }

  public static AppLanguage resolve(String choice, Locale systemLocale) {
    if (AUTOMATIC.equals(choice)) {
      return detect(systemLocale);
    }
    return Arrays.stream(values())
        .filter(language -> language.tag.equals(choice))
        .findFirst()
        .orElse(ENGLISH);
  }
}
