package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.application.ExportFilename;
import de.appfabrik.naehplan.i18n.Messages;
import java.nio.file.Path;
import java.util.Locale;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Native folder consent covers the atomic writer's temporary sibling on macOS. */
final class ExportFileDialog {
  private ExportFileDialog() {}

  static boolean usesFolderSelection(String platform) {
    return platform.toLowerCase(Locale.ROOT).startsWith("mac");
  }

  static Path choose(
      Window owner,
      Messages messages,
      String title,
      String suggestedName,
      String extension,
      String filterDescription) {
    // Use this on every Mac launch, including future sandboxed Store launchers.
    if (usesFolderSelection(System.getProperty("os.name", ""))) {
      DirectoryChooser chooser = new DirectoryChooser();
      chooser.setTitle(messages.text("exportFile.folder"));
      var folder = chooser.showDialog(owner);
      return folder == null
          ? null
          : filename(owner, messages, title, folder.toPath(), suggestedName, extension);
    }
    FileChooser chooser = new FileChooser();
    chooser.setTitle(title);
    chooser.setInitialFileName(suggestedName);
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter(filterDescription, "*." + extension));
    var selected = chooser.showSaveDialog(owner);
    if (selected == null) {
      return null;
    }
    Path path = selected.toPath();
    return selected.getName().toLowerCase(Locale.ROOT).endsWith("." + extension)
        ? path
        : path.resolveSibling(selected.getName() + "." + extension);
  }

  static Path filename(
      Window owner,
      Messages messages,
      String title,
      Path folder,
      String suggestedName,
      String extension) {
    var dialog = new TextInputDialog(suggestedName);
    dialog.initOwner(owner);
    dialog.setTitle(title);
    dialog.setHeaderText(messages.text("exportFile.hint"));
    dialog.setContentText(messages.text("exportFile.filename"));
    dialog.getEditor().setId("export-filename");
    dialog.getDialogPane().setPrefWidth(560);
    UiSupport.style(dialog.getDialogPane().getScene());
    var export = new ButtonType(messages.text("exportFile.action"), ButtonType.OK.getButtonData());
    var cancel = new ButtonType(messages.text("action.cancel"), ButtonType.CANCEL.getButtonData());
    dialog.getDialogPane().getButtonTypes().setAll(export, cancel);
    var button = dialog.getDialogPane().lookupButton(export);
    button.setId("export-filename-confirm");
    Runnable validate =
        () -> {
          try {
            ExportFilename.resolve(folder, dialog.getEditor().getText(), extension);
            button.setDisable(false);
          } catch (IllegalArgumentException exception) {
            button.setDisable(true);
          }
        };
    dialog.getEditor().textProperty().addListener(observable -> validate.run());
    dialog.setResultConverter(type -> type == export ? dialog.getEditor().getText() : null);
    validate.run();
    return dialog
        .showAndWait()
        .map(name -> ExportFilename.resolve(folder, name, extension))
        .orElse(null);
  }
}
