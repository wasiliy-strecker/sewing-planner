package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.application.PhotoException;
import de.appfabrik.naehplan.domain.Photo;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/** A bounded preview. Only the latest asynchronous selection may update it. */
final class PhotoPreview extends StackPane {
  private final ImageView image = new ImageView();
  private final Label placeholder = new Label("Kein Foto");
  private long generation;

  PhotoPreview(double width, double height) {
    setAlignment(Pos.CENTER);
    getStyleClass().add("photo-preview");
    setPrefSize(width, height);
    setMinSize(width, height);
    setMaxSize(width, height);
    image.setFitWidth(width - 12);
    image.setFitHeight(height - 12);
    image.setPreserveRatio(true);
    image.setSmooth(true);
    image.setAccessibleText("Foto des Projekts");
    placeholder.setAlignment(Pos.CENTER);
    placeholder.setWrapText(true);
    placeholder.setMaxWidth(width - 24);
    getChildren().addAll(image, placeholder);
  }

  static Image decode(Photo photo) {
    Image result = new Image(photo.openStream(), 600, 440, true, true);
    if (result.isError()) {
      throw new PhotoException(
          "Das Foto kann nicht angezeigt werden. Bitte wähle ein anderes JPEG- oder PNG-Bild.");
    }
    return result;
  }

  void showImage(Image value) {
    generation++;
    image.setImage(value);
    placeholder.setText("Kein Foto");
    placeholder.setVisible(value == null);
  }

  void load(java.util.concurrent.Callable<Photo> operation, AsyncTasks tasks) {
    showImage(null);
    long request = generation;
    placeholder.setText("Foto wird geladen …");
    tasks.submit(
        () -> {
          Photo photo = operation.call();
          return photo == null ? null : decode(photo);
        },
        loaded -> {
          if (request == generation) {
            showImage(loaded);
          }
        },
        failure -> {
          if (request == generation) {
            placeholder.setText("Foto nicht lesbar. Bitte lade die Sammlung neu.");
          }
        });
  }
}
