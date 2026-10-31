package de.appfabrik.naehplan.i18n;

/** The offline dialog and Store handoff use exactly the same localized text. */
public final class PrivacyNotice {
  private PrivacyNotice() {}

  public static String text(Messages messages) {
    return messages.text("privacy.title")
        + "\n\n"
        + messages.text("privacy.date")
        + "\n\n"
        + messages.text("privacy.body")
        + "\n";
  }
}
