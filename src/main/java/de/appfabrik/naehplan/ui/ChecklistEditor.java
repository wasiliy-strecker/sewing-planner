package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.application.CollectionLimits;
import de.appfabrik.naehplan.domain.ChecklistStep;
import de.appfabrik.naehplan.i18n.Messages;
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
  private final Label empty = new Label();
  private final Label summary = new Label();
  private final Label limit = new Label();
  private final Button add = new Button();
  private final Messages messages;

  ChecklistEditor(List<ChecklistStep> steps, Messages messages) {
    super(10);
    this.messages = messages;
    empty.setText(messages.text("checklist.empty"));
    setId("checklist-editor");
    Label heading = new Label(messages.text("checklist.heading"));
    heading.getStyleClass().add("field-label");
    Label hint = new Label(messages.text("checklist.hint"));
    heading.setWrapText(true);
    hint.setWrapText(true);
    hint.getStyleClass().add("muted");
    empty.setWrapText(true);
    empty.getStyleClass().add("muted");
    summary.setId("checklist-summary");
    summary.setWrapText(true);
    summary.getStyleClass().add("field-label");
    limit.setId("checklist-limit");
    limit.setWrapText(true);
    limit.getStyleClass().add("error-text");
    limit.managedProperty().bind(limit.visibleProperty());
    add.setText(messages.text("checklist.add"));
    add.setId("add-step");
    add.setOnAction(event -> addStep());
    getChildren().addAll(heading, hint, summary, empty, list, add, limit);
    for (ChecklistStep step : steps) {
      addRow(new StepInput(step.id(), step.text(), step.completed(), step.position()));
    }
    updateState();
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
    return row;
  }

  private void addStep() {
    if (rows.size() >= CollectionLimits.MAX_STEPS || positionExhausted()) {
      updateState();
      return;
    }
    int position = rows.isEmpty() ? 0 : rows.getLast().position + 1;
    StepRow row = addRow(new StepInput(UUID.randomUUID(), "", false, position));
    updateState();
    row.text.requestFocus();
  }

  private boolean positionExhausted() {
    return !rows.isEmpty() && rows.getLast().position == Integer.MAX_VALUE;
  }

  private void updateState() {
    empty.setVisible(rows.isEmpty());
    empty.setManaged(rows.isEmpty());
    summary.setText(
        messages.text(
            "checklist.summary",
            rows.stream().filter(row -> row.completed.isSelected()).count(),
            rows.size()));
    boolean full = rows.size() >= CollectionLimits.MAX_STEPS;
    boolean exhausted = positionExhausted();
    add.setDisable(full || exhausted);
    limit.setVisible(full || exhausted);
    limit.setText(
        full
            ? messages.text("checklist.limit", CollectionLimits.MAX_STEPS)
            : exhausted ? messages.text("checklist.positionLimit") : "");
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
      completed.setAccessibleText(messages.text("checklist.done"));
      completed.selectedProperty().addListener(observable -> updateState());
      text.setId("step-text-" + id);
      text.setText(input.text());
      UiSupport.textMenu(text, messages);
      text.setPromptText(messages.text("checklist.prompt"));
      text.setAccessibleText(messages.text("checklist.text"));
      text.setOnAction(
          event -> {
            // Keep Enter inside the checklist instead of triggering the dialog's Save button.
            event.consume();
            if (text.getText()
                .codePoints()
                .noneMatch(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c))) {
              return;
            }
            int index = rows.indexOf(this);
            if (index + 1 < rows.size()) {
              rows.get(index + 1).text.requestFocus();
            } else {
              addStep();
            }
          });
      HBox.setHgrow(text, Priority.ALWAYS);
      Button remove = new Button(messages.text("checklist.remove"));
      remove.setId("remove-step-" + id);
      remove.setMinWidth(USE_PREF_SIZE);
      remove.getStyleClass().add("danger");
      remove.setOnAction(
          event -> {
            // The UUID identifies the step. Duplicate text and changed positions do not.
            int index = rows.indexOf(this);
            rows.removeIf(row -> row.id.equals(id));
            list.getChildren().remove(this);
            updateState();
            if (rows.isEmpty()) {
              add.requestFocus();
            } else {
              rows.get(Math.min(index, rows.size() - 1)).text.requestFocus();
            }
          });
      getChildren().addAll(completed, text, remove);
    }

    StepInput input() {
      return new StepInput(id, text.getText(), completed.isSelected(), position);
    }
  }
}
