package de.appfabrik.naehplan.domain;

import java.util.UUID;

/** Stable identity and explicit order, independent of text and completion. */
public record ChecklistStep(UUID id, String text, boolean completed, int position) {
  public ChecklistStep {
    if (id == null) {
      throw new IllegalArgumentException("Ein Arbeitsschritt benötigt eine interne ID.");
    }
    text = text == null ? "" : text.strip();
    if (text.codePoints().noneMatch(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c))) {
      throw new IllegalArgumentException(
          "Bitte gib für jeden Arbeitsschritt einen Text ein oder entferne die leere Zeile.");
    }
    if (position < 0) {
      throw new IllegalArgumentException(
          "Die Position eines Arbeitsschritts darf nicht negativ sein.");
    }
  }
}
