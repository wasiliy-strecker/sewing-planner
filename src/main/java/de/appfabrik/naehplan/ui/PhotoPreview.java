package de.appfabrik.naehplan.ui;

import de.appfabrik.naehplan.application.PhotoException;
import de.appfabrik.naehplan.domain.Photo;
import de.appfabrik.naehplan.i18n.Messages;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/** A bounded preview. Only the latest asynchronous selection may update it. */
final class PhotoPreview extends StackPane {
  private final Messages messages;
  private final ImageView image = new ImageView();
  private final Label placeholder = new Label();
  private long generation;

  PhotoPreview(double width, double height, Messages messages) {
    this.messages = messages;
    placeholder.setText(messages.text("photo.empty"));
    setAlignment(Pos.CENTER);
    getStyleClass().add("photo-preview");
    setPrefSize(width, height);
    setMinSize(width, height);
    setMaxSize(width, height);
    image.setId("photo-image");
    placeholder.setId("photo-placeholder");
    image.setFitWidth(width - 12);
    image.setFitHeight(height - 12);
    image.setPreserveRatio(true);
    image.setSmooth(true);
    image.setAccessibleText(messages.text("photo.accessible"));
    placeholder.setAlignment(Pos.CENTER);
    placeholder.setWrapText(true);
    placeholder.setMaxWidth(width - 24);
    getChildren().addAll(image, placeholder);
  }

  static Image decode(Photo photo) {
    Image result = new Image(photo.openStream(), 600, 440, true, true);
    if (result.isError()) {
      throw new PhotoException(PhotoException.Code.DISPLAY);
    }
    return result;
  }

  void showImage(Image value) {
    generation++;
    image.setImage(value);
    placeholder.setText(messages.text("photo.empty"));
    placeholder.setVisible(value == null);
  }

  void load(java.util.concurrent.Callable<Photo> operation, AsyncTasks tasks) {
    showImage(null);
    long request = generation;
    placeholder.setText(messages.text("photo.loading"));
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
            placeholder.setText(messages.text("photo.unreadable"));
          }
        });
  }
}
