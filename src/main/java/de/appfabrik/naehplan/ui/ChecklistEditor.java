package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.domain.ChecklistStep;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Raw input stays in the dialog, including temporarily empty step texts. */
final class ChecklistEditor extends VBox {
  private final List<StepRow> rows = new ArrayList<>();
  private final VBox list = new VBox(8);
  private final Label empty =
      new Label("Noch keine Arbeitsschritte. Eine leere Checkliste ist in Ordnung.");

  ChecklistEditor(List<ChecklistStep> steps) {
    super(10);
    setId("checklist-editor");
    Label heading = new Label("Arbeitsschritte");
    heading.getStyleClass().add("field-label");
    Label hint =
        new Label(
            "Abhaken und Bearbeiten werden erst mit „Speichern“ übernommen. Der Projektstatus bleibt unabhängig.");
    hint.setWrapText(true);
    hint.getStyleClass().add("muted");
    empty.setWrapText(true);
    empty.getStyleClass().add("muted");
    Button add = new Button("+ Arbeitsschritt hinzufügen");
    add.setId("add-step");
    add.setOnAction(
        event -> {
          int position = rows.isEmpty() ? 0 : Math.addExact(rows.getLast().position, 1);
          StepRow row = addRow(new StepInput(UUID.randomUUID(), "", false, position));
          row.text.requestFocus();
        });
    getChildren().addAll(heading, hint, empty, list, add);
    for (ChecklistStep step : steps) {
      addRow(new StepInput(step.id(), step.text(), step.completed(), step.position()));
    }
    updateEmpty();
  }

  List<StepInput> input() {
    return rows.stream().map(StepRow::input).toList();
  }

  List<ChecklistStep> validatedSteps() {
    List<ChecklistStep> result = new ArrayList<>();
    for (StepRow row : rows) {
      try {
        result.add(row.input().validated());
      } catch (IllegalArgumentException exception) {
        row.text.requestFocus();
        throw exception;
      }
    }
    return List.copyOf(result);
  }

  private StepRow addRow(StepInput input) {
    StepRow row = new StepRow(input);
    rows.add(row);
    list.getChildren().add(row);
    updateEmpty();
    return row;
  }

  private void updateEmpty() {
    empty.setVisible(rows.isEmpty());
    empty.setManaged(rows.isEmpty());
  }

  /** Snapshot equality includes raw text, IDs, completion and explicit position. */
  record StepInput(UUID id, String text, boolean completed, int position) {
    ChecklistStep validated() {
      return new ChecklistStep(id, text, completed, position);
    }
  }

  private final class StepRow extends HBox {
    private final UUID id;
    private final int position;
    private final TextField text = new TextField();
    private final CheckBox completed = new CheckBox();

    StepRow(StepInput input) {
      super(10);
      id = input.id();
      position = input.position();
      setId("step-" + id);
      setAlignment(Pos.CENTER_LEFT);
      getStyleClass().add("step-row");
      completed.setId("step-completed-" + id);
      completed.setAllowIndeterminate(false);
      completed.setSelected(input.completed());
      completed.setAccessibleText("Arbeitsschritt erledigt");
      text.setId("step-text-" + id);
      text.setText(input.text());
      text.setPromptText("Zum Beispiel Zuschneiden");
      text.setAccessibleText("Text des Arbeitsschritts");
      HBox.setHgrow(text, Priority.ALWAYS);
      Button remove = new Button("Entfernen");
      remove.setId("remove-step-" + id);
      remove.setMinWidth(USE_PREF_SIZE);
      remove.setOnAction(
          event -> {
            // The UUID identifies the step. Duplicate text and changed positions do not.
            rows.removeIf(row -> row.id.equals(id));
            list.getChildren().remove(this);
            updateEmpty();
          });
      getChildren().addAll(completed, text, remove);
    }

    StepInput input() {
      return new StepInput(id, text.getText(), completed.isSelected(), position);
    }
  }
}
