package de.appfabrik.naehplan.persistence;

import de.appfabrik.naehplan.application.PhotoException;
import de.appfabrik.naehplan.application.PhotoImporter;
import de.appfabrik.naehplan.application.ProjectRepository;
import de.appfabrik.naehplan.application.StorageException;
import de.appfabrik.naehplan.domain.ChecklistStep;
import de.appfabrik.naehplan.domain.Photo;
import de.appfabrik.naehplan.domain.Project;
import de.appfabrik.naehplan.domain.ProjectDraft;
import de.appfabrik.naehplan.domain.ProjectStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.sqlite.SQLiteConfig;

/** Opens and closes one JDBC connection per operation. No connection escapes this class. */
public final class SqliteProjectRepository implements ProjectRepository {
  private final Path database;

  public SqliteProjectRepository(Path database) {
    this.database = database.toAbsolutePath().normalize();
  }

  private Connection connect() throws SQLException {
    SQLiteConfig config = new SQLiteConfig();
    config.setBusyTimeout(2000);
    config.enforceForeignKeys(true);
    return config.createConnection("jdbc:sqlite:" + database);
  }

  @Override
  public void initialize() {
    try {
      Path parent = database.getParent();
      if (parent == null) {
        throw new StorageException(StorageException.Code.PATH);
      }
      Files.createDirectories(parent);
      try (Connection connection = connect()) {
        connection.setAutoCommit(false);
        try (var statement = connection.createStatement()) {
          int version;
          try (var result = statement.executeQuery("PRAGMA user_version")) {
            result.next();
            version = result.getInt(1);
          }
          if (version > 1) {
            throw new StorageException(StorageException.Code.NEWER_SCHEMA);
          }
          if (version == 0) {
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS projects (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  name TEXT NOT NULL CHECK (length(trim(name)) > 0),
                  material TEXT NOT NULL DEFAULT '',
                  color TEXT NOT NULL DEFAULT '',
                  location TEXT NOT NULL DEFAULT '',
                  note TEXT NOT NULL DEFAULT '',
                  photo BLOB CHECK (photo IS NULL OR length(photo) <= 10485760),
                  status TEXT NOT NULL DEFAULT 'planned'
                    CHECK (status IN ('planned', 'started', 'finished'))
                )
                """);
            statement.executeUpdate(
                """
                CREATE TABLE checklist_steps (
                  id TEXT PRIMARY KEY NOT NULL,
                  project_id INTEGER NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                  text TEXT NOT NULL CHECK (length(trim(text)) > 0),
                  completed INTEGER NOT NULL DEFAULT 0 CHECK (completed IN (0, 1)),
                  position INTEGER NOT NULL CHECK (typeof(position) = 'integer' AND position >= 0),
                  UNIQUE (project_id, position)
                )
                """);
            statement.executeUpdate("PRAGMA user_version = 1");
          }
          connection.commit();
        } catch (SQLException | RuntimeException exception) {
          connection.rollback();
          throw exception;
        }
      }
    } catch (IOException | SQLException exception) {
      throw new StorageException(StorageException.Code.OPEN, exception);
    }
  }

  @Override
  public List<Project> findAll() {
    try (Connection connection = connect()) {
      // Both tables must come from the same snapshot. Photos stay lazy.
      connection.setAutoCommit(false);
      Map<Long, List<ChecklistStep>> steps = readSteps(connection);
      try (var statement = connection.createStatement();
          var result =
              statement.executeQuery(
                  "SELECT id, name, material, color, location, note, status, photo IS NOT NULL AS has_photo FROM projects")) {
        List<Project> projects = new ArrayList<>();
        while (result.next()) {
          projects.add(
              new Project(
                  result.getLong("id"),
                  result.getString("name"),
                  result.getString("material"),
                  result.getString("color"),
                  result.getString("location"),
                  result.getString("note"),
                  result.getBoolean("has_photo"),
                  ProjectStatus.fromStoredValue(result.getString("status")),
                  steps.getOrDefault(result.getLong("id"), List.of())));
        }
        return List.copyOf(projects);
      }
    } catch (IllegalArgumentException exception) {
      throw new StorageException(StorageException.Code.STATUS, exception);
    } catch (SQLException exception) {
      throw failure(exception);
    }
  }

  private static Map<Long, List<ChecklistStep>> readSteps(Connection connection)
      throws SQLException {
    Map<Long, List<ChecklistStep>> steps = new HashMap<>();
    try (var statement = connection.createStatement();
        var result =
            statement.executeQuery(
                "SELECT id, project_id, text, completed, position FROM checklist_steps ORDER BY project_id, position")) {
      while (result.next()) {
        int completed = result.getInt("completed");
        if (completed != 0 && completed != 1) {
          throw new IllegalArgumentException("Invalid stored completion value");
        }
        steps
            .computeIfAbsent(result.getLong("project_id"), key -> new ArrayList<>())
            .add(
                new ChecklistStep(
                    UUID.fromString(result.getString("id")),
                    result.getString("text"),
                    completed == 1,
                    result.getInt("position")));
      }
    }
    return steps;
  }

  @Override
  public Photo findPhoto(long id) {
    try (Connection connection = connect();
        var statement = connection.prepareStatement("SELECT photo FROM projects WHERE id = ?")) {
      statement.setLong(1, id);
      try (var result = statement.executeQuery()) {
        if (!result.next()) {
          throw new StorageException(StorageException.Code.MISSING);
        }
        byte[] bytes = result.getBytes(1);
        return bytes == null ? null : PhotoImporter.decode(bytes);
      }
    } catch (SQLException exception) {
      throw failure(exception);
    } catch (PhotoException exception) {
      throw new StorageException(StorageException.Code.PHOTO, exception);
    }
  }

  @Override
  public Project create(ProjectDraft draft) {
    return save(null, draft);
  }

  @Override
  public Project update(long id, ProjectDraft draft) {
    return save(id, draft);
  }

  private Project save(Long id, ProjectDraft draft) {
    try (Connection connection = connect()) {
      connection.setAutoCommit(false);
      try {
        long projectId;
        String sql =
            id == null
                ? "INSERT INTO projects (name, material, color, location, note, photo, status) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id"
                : "UPDATE projects SET name = ?, material = ?, color = ?, location = ?, note = ?, photo = ?, status = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
          bind(statement, draft);
          if (id == null) {
            try (ResultSet result = statement.executeQuery()) {
              if (!result.next()) {
                throw new StorageException(StorageException.Code.CREATE);
              }
              projectId = result.getLong(1);
            }
          } else {
            projectId = id;
            statement.setLong(8, id);
            requireExisting(statement.executeUpdate());
          }
        }
        replaceSteps(connection, projectId, draft.steps());
        connection.commit();
        return draft.withId(projectId);
      } catch (SQLException | RuntimeException exception) {
        connection.rollback();
        throw exception;
      }
    } catch (SQLException exception) {
      throw failure(exception);
    }
  }

  private static void replaceSteps(Connection connection, long projectId, List<ChecklistStep> steps)
      throws SQLException {
    try (var delete =
        connection.prepareStatement("DELETE FROM checklist_steps WHERE project_id = ?")) {
      delete.setLong(1, projectId);
      delete.executeUpdate();
    }
    try (var insert =
        connection.prepareStatement(
            "INSERT INTO checklist_steps (id, project_id, text, completed, position) VALUES (?, ?, ?, ?, ?)")) {
      for (ChecklistStep step : steps) {
        insert.setString(1, step.id().toString());
        insert.setLong(2, projectId);
        insert.setString(3, step.text());
        insert.setBoolean(4, step.completed());
        insert.setInt(5, step.position());
        insert.addBatch();
      }
      insert.executeBatch();
    }
  }

  @Override
  public void delete(long id) {
    try (Connection connection = connect();
        PreparedStatement statement =
            connection.prepareStatement("DELETE FROM projects WHERE id = ?")) {
      statement.setLong(1, id);
      requireExisting(statement.executeUpdate());
    } catch (SQLException exception) {
      throw failure(exception);
    }
  }

  private static void bind(PreparedStatement statement, ProjectDraft draft) throws SQLException {
    statement.setString(1, draft.name());
    statement.setString(2, draft.material());
    statement.setString(3, draft.color());
    statement.setString(4, draft.location());
    statement.setString(5, draft.note());
    statement.setBytes(6, draft.photo() == null ? null : draft.photo().bytes());
    statement.setString(7, draft.status().storedValue());
  }

  private static void requireExisting(int affected) {
    if (affected != 1) {
      throw new StorageException(StorageException.Code.MISSING);
    }
  }

  private static StorageException failure(SQLException exception) {
    return new StorageException(StorageException.Code.OPERATION, exception);
  }
}
