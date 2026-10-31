package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.domain.Project;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Permanent header indicators with a two-direction click cycle. */
final class ProjectTableSorting {
  private ProjectTableSorting() {}

  static void configure(TableView<Project> table) {
    for (TableColumn<Project, ?> column : table.getColumns()) {
      VBox arrows =
          new VBox(
              3,
              arrow(table, column, TableColumn.SortType.ASCENDING),
              arrow(table, column, TableColumn.SortType.DESCENDING));
      arrows.setAlignment(Pos.CENTER);
      arrows.setMouseTransparent(true);
      column.setGraphic(arrows);

      // The permanent pair replaces JavaFX's indicator for active columns only.
      Region nativeIndicator = new Region();
      nativeIndicator.setMinSize(0, 0);
      nativeIndicator.setPrefSize(0, 0);
      nativeIndicator.setMaxSize(0, 0);
      column.setSortNode(nativeIndicator);
    }
    table.addEventFilter(
        MouseEvent.MOUSE_CLICKED,
        event -> {
          if (event.getButton() != MouseButton.PRIMARY || !event.isStillSincePress()) {
            return;
          }
          if (!(event.getTarget() instanceof Node node)) {
            return;
          }
          while (node != null && !(node instanceof TableColumnHeader)) {
            node = node.getParent();
          }
          if (node instanceof TableColumnHeader header) {
            for (TableColumn<Project, ?> column : table.getColumns()) {
              if (column == header.getTableColumn() && !table.getSortOrder().contains(column)) {
                // Native sorting runs on release. Its third click removes the column.
                // Restore ascending on the ensuing click, outside the sort-list change.
                column.setSortType(TableColumn.SortType.ASCENDING);
                table.getSortOrder().setAll(List.of(column));
                break;
              }
            }
          }
        });
  }

  private static Region arrow(
      TableView<Project> table, TableColumn<Project, ?> column, TableColumn.SortType direction) {
    Region arrow = new Region();
    arrow
        .getStyleClass()
        .addAll("sort-chevron", direction == TableColumn.SortType.ASCENDING ? "up" : "down");
    arrow.setMinSize(9, 5);
    arrow.setPrefSize(9, 5);
    arrow.setMaxSize(9, 5);
    arrow
        .opacityProperty()
        .bind(
            Bindings.createDoubleBinding(
                () ->
                    table.getSortOrder().contains(column) && column.getSortType() == direction
                        ? 1.0
                        : 0.3,
                table.getSortOrder(),
                column.sortTypeProperty()));
    return arrow;
  }
}
