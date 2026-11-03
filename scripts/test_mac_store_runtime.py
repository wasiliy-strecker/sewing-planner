"""Validate current screenshot inputs without modifying existing app images."""

import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from zipfile import ZipFile

import mac_store_content as content


class ExistingRuntimeTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.image = self.root / "target/app-image/Sewing Planner/lib/app"
        self.image.mkdir(parents=True)
        self.old_jar = self.image / "naehplan-0.1.0.jar"
        self.old_jar.write_bytes(b"Previous app image, preserve unchanged")
        for number in range(3):
            (self.image / f"dependency-{number}.jar").write_bytes(b"Dependency")
        modules = self.root / "target/javafx-modules"
        modules.mkdir()
        for number in range(3):
            (modules / f"javafx-{number}.jar").write_bytes(b"JavaFX module")
        self.compiled = self.root / "target/classes"
        self.compiled.mkdir()
        (self.compiled / "Example.class").write_bytes(b"Verified class")
        (self.compiled / "messages.properties").write_bytes(b"name=Sewing Planner\n")
        self.resources = self.root / "src/main/resources"
        self.resources.mkdir(parents=True)
        (self.resources / "messages.properties").write_bytes(b"name=Sewing Planner\n")
        self.jar = self.root / "target/naehplan-0.1.0.jar"
        with ZipFile(self.jar, "w") as archive:
            for file in self.compiled.iterdir():
                archive.write(file, file.name)
        root_patch = patch.object(content, "ROOT", self.root)
        root_patch.start()
        self.addCleanup(root_patch.stop)
        git_patch = patch.object(content.subprocess, "check_output", return_value=b"")
        self.git = git_patch.start()
        self.addCleanup(git_patch.stop)

    def test_current_jar_is_used_and_previous_app_image_is_preserved(self):
        previous = self.old_jar.read_bytes()
        result = content.existing_runtime()
        self.assertEqual(7, len(result))
        self.assertIn("target/naehplan-0.1.0.jar", result)
        self.assertNotIn(self.old_jar.relative_to(self.root).as_posix(), result)
        self.assertEqual(previous, self.old_jar.read_bytes())

    def test_refuses_changed_compiled_classes(self):
        (self.compiled / "Example.class").write_bytes(b"Changed class")
        with self.assertRaisesRegex(ValueError, "differs from the existing compiled output"):
            content.existing_runtime()

    def test_refuses_outdated_resources(self):
        (self.resources / "messages.properties").write_bytes(b"Changed resource")
        with self.assertRaisesRegex(ValueError, "outdated resources"):
            content.existing_runtime()

    def test_refuses_sources_newer_than_the_verified_jar(self):
        source = self.root / "src/main/java/Example.java"
        source.parent.mkdir(parents=True)
        source.write_text("class Example {}\n", encoding="utf-8")
        timestamp = self.jar.stat().st_mtime + 60
        os.utime(source, (timestamp, timestamp))
        with self.assertRaisesRegex(ValueError, "newer than the existing JAR"):
            content.existing_runtime()

    def test_refuses_uncommitted_application_changes(self):
        self.git.return_value = b"src/main/java/Example.java\n"
        with self.assertRaisesRegex(ValueError, "App sources changed"):
            content.existing_runtime()

    def test_refuses_missing_dependencies(self):
        (self.image / "dependency-0.jar").unlink()
        with self.assertRaisesRegex(ValueError, "dependencies and JavaFX modules"):
            content.existing_runtime()


if __name__ == "__main__":
    unittest.main()
