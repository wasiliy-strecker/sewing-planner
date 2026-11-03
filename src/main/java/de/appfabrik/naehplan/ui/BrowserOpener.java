package de.appfabrik.naehplan.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/** Hands an explicit user action to the system browser without reading collection data. */
@FunctionalInterface
interface BrowserOpener {
  void open(URI uri) throws IOException;

  static BrowserOpener system() {
    return uri -> {
      if (!Desktop.isDesktopSupported()
          || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        throw new IOException("No system browser is available");
      }
      Desktop.getDesktop().browse(uri);
    };
  }
}
