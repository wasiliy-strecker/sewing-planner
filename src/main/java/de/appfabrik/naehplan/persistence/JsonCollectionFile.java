package de.appfabrik.naehplan.persistence;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import de.appfabrik.naehplan.application.CollectionFile;
import de.appfabrik.naehplan.application.CollectionLimitException;
import de.appfabrik.naehplan.application.CollectionLimits;
import de.appfabrik.naehplan.application.ImportException;
import de.appfabrik.naehplan.application.PhotoException;
import de.appfabrik.naehplan.application.PhotoImporter;
import de.appfabrik.naehplan.domain.ChecklistStep;
import de.appfabrik.naehplan.domain.Photo;
import de.appfabrik.naehplan.domain.ProjectDraft;
import de.appfabrik.naehplan.domain.ProjectStatus;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Versioned UTF-8 JSON, including selected colors and exact stored photo bytes. */
public final class JsonCollectionFile implements CollectionFile {
  private final long maxFileBytes;
  private static final Set<String> ENTRY_FIELDS =
      Set.of("name", "material", "color", "location", "note", "photoBase64", "status", "steps");

  public JsonCollectionFile() {
    this(CollectionLimits.MAX_FILE_BYTES);
  }

  JsonCollectionFile(long maxFileBytes) {
    if (maxFileBytes < 1 || maxFileBytes > CollectionLimits.MAX_FILE_BYTES) {
      throw new IllegalArgumentException("Invalid collection file budget");
    }
    this.maxFileBytes = maxFileBytes;
  }

  @Override
  public void read(Path source, Consumer<ProjectDraft> accept) {
    try {
      if (!Files.isRegularFile(source)) {
        throw new ImportException(ImportException.Code.READ);
      }
      if (Files.size(source) > maxFileBytes) {
        throw new ImportException(ImportException.Code.LIMIT);
      }
      var decoder =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT);
      try (var input = new LimitedInput(Files.newInputStream(source), maxFileBytes);
          var reader = new JsonReader(new InputStreamReader(input, decoder))) {
        reader.setStrictness(Strictness.STRICT);
        reader.beginObject();
        Set<String> keys = new HashSet<>();
        while (reader.hasNext()) {
          String key = reader.nextName();
          require(keys.add(key));
          switch (key) {
            case "format" -> require(readText(reader).equals("sewing-planner.collection"));
            case "version" -> {
              require(reader.peek() == JsonToken.NUMBER);
              if (!reader.nextString().equals("1")) {
                throw new ImportException(ImportException.Code.VERSION);
              }
            }
            case "projects" -> readEntries(reader, accept);
            default -> throw new ImportException(ImportException.Code.FORMAT);
          }
        }
        reader.endObject();
        require(keys.equals(Set.of("format", "version", "projects")));
        require(reader.peek() == JsonToken.END_DOCUMENT);
      }
    } catch (MalformedJsonException | EOFException | CharacterCodingException exception) {
      throw new ImportException(ImportException.Code.FORMAT, exception);
    } catch (IOException | SecurityException exception) {
      throw new ImportException(ImportException.Code.READ, exception);
    } catch (IllegalArgumentException | IllegalStateException exception) {
      throw new ImportException(ImportException.Code.FORMAT, exception);
    }
  }

  private static void readEntries(JsonReader reader, Consumer<ProjectDraft> accept)
      throws IOException {
    reader.beginArray();
    int count = 0;
    while (reader.hasNext()) {
      if (++count > CollectionLimits.MAX_ENTRIES) {
        throw new ImportException(ImportException.Code.LIMIT);
      }
      reader.beginObject();
      Set<String> keys = new HashSet<>();
      var values = new HashMap<String, String>();
      Photo photo = null;
      List<ChecklistStep> steps = List.of();
      while (reader.hasNext()) {
        String key = reader.nextName();
        require(ENTRY_FIELDS.contains(key) && keys.add(key));
        if (key.equals("photoBase64")) {
          photo = readPhoto(reader);
        } else if (key.equals("steps")) {
          steps = readChecklist(reader);
        } else {
          values.put(key, readText(reader));
        }
      }
      reader.endObject();
      require(keys.equals(ENTRY_FIELDS));
      accept.accept(
          new ProjectDraft(
              values.get("name"),
              values.get("material"),
              values.get("color"),
              values.get("location"),
              values.get("note"),
              photo,
              ProjectStatus.fromStoredValue(values.get("status")),
              steps));
    }
    reader.endArray();
  }

  private static List<ChecklistStep> readChecklist(JsonReader reader) throws IOException {
    reader.beginArray();
    var steps = new ArrayList<ChecklistStep>();
    while (reader.hasNext()) {
      if (steps.size() >= CollectionLimits.MAX_STEPS) {
        throw new ImportException(ImportException.Code.LIMIT);
      }
      reader.beginObject();
      Set<String> keys = new HashSet<>();
      UUID id = null;
      String text = null;
      boolean completed = false;
      int position = -1;
      while (reader.hasNext()) {
        String key = reader.nextName();
        require(keys.add(key));
        switch (key) {
          case "id" -> {
            String value = readText(reader);
            id = UUID.fromString(value);
            require(id.toString().equalsIgnoreCase(value));
          }
          case "text" -> text = readText(reader);
          case "completed" -> {
            require(reader.peek() == JsonToken.BOOLEAN);
            completed = reader.nextBoolean();
          }
          case "position" -> {
            require(reader.peek() == JsonToken.NUMBER);
            String value = reader.nextString();
            require(value.matches("0|[1-9][0-9]*"));
            position = Integer.parseInt(value);
          }
          default -> throw new ImportException(ImportException.Code.FORMAT);
        }
      }
      reader.endObject();
      require(keys.equals(Set.of("id", "text", "completed", "position")));
      steps.add(new ChecklistStep(id, text, completed, position));
    }
    reader.endArray();
    return List.copyOf(steps);
  }

  private static String readText(JsonReader reader) throws IOException {
    require(reader.peek() == JsonToken.STRING);
    String value = reader.nextString();
    if (value.length() > CollectionLimits.MAX_TEXT_LENGTH) {
      throw new ImportException(ImportException.Code.LIMIT);
    }
    for (int i = 0; i < value.length(); i++) {
      char character = value.charAt(i);
      if (Character.isHighSurrogate(character)) {
        require(i + 1 < value.length() && Character.isLowSurrogate(value.charAt(++i)));
      } else {
        require(!Character.isLowSurrogate(character));
      }
    }
    return value;
  }

  private static Photo readPhoto(JsonReader reader) throws IOException {
    if (reader.peek() == JsonToken.NULL) {
      reader.nextNull();
      return null;
    }
    require(reader.peek() == JsonToken.STRING);
    String encoded = reader.nextString();
    if (encoded.length() > ((PhotoImporter.MAX_BYTES + 2) / 3) * 4) {
      throw new ImportException(ImportException.Code.LIMIT);
    }
    try {
      return PhotoImporter.decode(Base64.getDecoder().decode(encoded));
    } catch (IllegalArgumentException | PhotoException exception) {
      throw new ImportException(ImportException.Code.PHOTO, exception);
    }
  }

  private static void require(boolean valid) {
    if (!valid) {
      throw new ImportException(ImportException.Code.FORMAT);
    }
  }

  private static final class LimitedInput extends FilterInputStream {
    private long count;
    private final long limit;

    private LimitedInput(InputStream input, long limit) {
      super(input);
      this.limit = limit;
    }

    @Override
    public int read() throws IOException {
      int value = in.read();
      if (value != -1) {
        reserve(1);
      }
      return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
      int result = in.read(bytes, offset, (int) Math.min(length, limit - count + 1));
      if (result > 0) {
        reserve(result);
      }
      return result;
    }

    private void reserve(int bytes) {
      count += bytes;
      if (count > limit) {
        throw new ImportException(ImportException.Code.LIMIT);
      }
    }
  }

  private static final class LimitedOutput extends FilterOutputStream {
    private final long limit;
    private long count;

    private LimitedOutput(OutputStream output, long limit) {
      super(output);
      this.limit = limit;
    }

    @Override
    public void write(int value) throws IOException {
      reserve(1);
      out.write(value);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
      reserve(length);
      out.write(bytes, offset, length);
    }

    private void reserve(int length) {
      if (length > limit - count) {
        throw new CollectionLimitException(CollectionLimitException.Code.FILE_SIZE);
      }
      count += length;
    }
  }

  @Override
  public void write(Path target, Iterable<ProjectDraft> projects) throws IOException {
    Path destination = target.toAbsolutePath().normalize();
    Path parent = destination.getParent();
    Path filename = destination.getFileName();
    if (parent == null
        || filename == null
        || !filename.toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
      throw new IOException("Choose a JSON file in an existing directory");
    }
    if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
      throw new FileAlreadyExistsException(destination.toString());
    }
    Path temporary = Files.createTempFile(parent, ".sewing-planner-", ".json.tmp");
    try {
      try (var output = new LimitedOutput(Files.newOutputStream(temporary), maxFileBytes);
          var writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
        writer.write(
            "{\n  \"format\": \"sewing-planner.collection\",\n  \"version\": 1,\n  \"projects\": [");
        boolean first = true;
        int count = 0;
        for (ProjectDraft project : projects) {
          if (++count > CollectionLimits.MAX_ENTRIES) {
            throw new CollectionLimitException(CollectionLimitException.Code.ENTRIES);
          }
          CollectionLimits.validate(project);
          writer.write(first ? "\n" : ",\n");
          first = false;
          writer.write("    {\n      \"name\": ");
          string(writer, project.name());
          writer.write(",\n      \"material\": ");
          string(writer, project.material());
          writer.write(",\n      \"color\": ");
          string(writer, project.color());
          writer.write(",\n      \"location\": ");
          string(writer, project.location());
          writer.write(",\n      \"note\": ");
          string(writer, project.note());
          writer.write(",\n      \"photoBase64\": ");
          if (project.photo() == null) {
            writer.write("null");
          } else {
            byte[] photo = project.photo().bytes();
            PhotoImporter.decode(photo);
            string(writer, Base64.getEncoder().encodeToString(photo));
          }
          writer.write(",\n      \"status\": ");
          string(writer, project.status().storedValue());
          writer.write(",\n      \"steps\": [");
          boolean firstStep = true;
          for (var step : project.steps()) {
            writer.write(firstStep ? "\n" : ",\n");
            firstStep = false;
            writer.write("        {\"id\": ");
            string(writer, step.id().toString());
            writer.write(", \"text\": ");
            string(writer, step.text());
            writer.write(", \"completed\": " + step.completed());
            writer.write(", \"position\": " + step.position() + "}");
          }
          writer.write("\n      ]");
          writer.write("\n    }");
        }
        writer.write("\n  ]\n}\n");
      }
      // Publish only the complete file, without replacing an existing target.
      Files.move(temporary, destination);
    } finally {
      try {
        Files.deleteIfExists(temporary);
      } catch (IOException | SecurityException ignored) {
        // Cleanup must not turn an already completed export into an error.
      }
    }
  }

  private static void string(Writer writer, String value) throws IOException {
    writer.write('"');
    for (int i = 0; i < value.length(); i++) {
      char character = value.charAt(i);
      switch (character) {
        case '"' -> writer.write("\\\"");
        case '\\' -> writer.write("\\\\");
        case '\n' -> writer.write("\\n");
        case '\r' -> writer.write("\\r");
        case '\t' -> writer.write("\\t");
        default -> {
          if (character < 0x20 || Character.isSurrogate(character)) {
            writer.write(String.format(Locale.ROOT, "\\u%04x", (int) character));
          } else {
            writer.write(character);
          }
        }
      }
    }
    writer.write('"');
  }
}
