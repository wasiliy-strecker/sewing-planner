package de.appfabrik.naehplan.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/** One immutable language for the lifetime of a window and its asynchronous operations. */
public final class Messages {
  public static final String BASE_NAME = "de.appfabrik.naehplan.i18n.messages";
  private final AppLanguage language;
  private final ResourceBundle bundle;

  public Messages(AppLanguage language) {
    this.language = language;
    bundle =
        ResourceBundle.getBundle(
            BASE_NAME,
            language.locale(),
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
  }

  public Locale locale() {
    return language.locale();
  }

  public String text(String key, Object... arguments) {
    return new MessageFormat(bundle.getString(key).replace("'", "''"), locale()).format(arguments);
  }

  public String status(de.appfabrik.naehplan.domain.ProjectStatus status) {
    return text("status." + status.storedValue());
  }

  public String count(int visible, int total, boolean filtered) {
    boolean singular =
        total == 1
            || (total == 0
                && (language == AppLanguage.FRENCH || language == AppLanguage.PORTUGUESE));
    String suffix = singular ? "one" : "many";
    return filtered
        ? text("count.filtered." + suffix, visible, total)
        : text("count." + suffix, total);
  }
}
