#!/usr/bin/env python3
"""Capture and verify the real JavaFX app on an isolated Linux X server."""

import argparse
import hashlib
import json
import os
import platform
import select
import shutil
import subprocess
import tempfile
from datetime import UTC, datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LOCALES = ("de-DE", "en-US", "es-ES", "fr-FR", "it-IT", "pt-BR")


def source_hashes():
    files = [ROOT / "pom.xml", ROOT / "mvnw"]
    for folder in ("src", "assets", "scripts", ".mvn", "store-listings"):
        files.extend(
            path
            for path in (ROOT / folder).rglob("*")
            if path.is_file() and "__pycache__" not in path.parts
        )
    return {
        str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
        for path in sorted(files)
    }


def java_command():
    java_home = Path(os.environ.get("JAVA_HOME", str(Path.home() / ".local/jdk-21")))
    java = java_home / "bin/java"
    if not java.is_file():
        raise RuntimeError("Set JAVA_HOME to a full JDK 21")
    dependencies = (
        (ROOT / "target/gui-classpath.txt").read_text().strip().split(os.pathsep)
    )
    javafx = [
        path
        for path in dependencies
        if "javafx-" in Path(path).name
        and Path(path).name.endswith(("-linux.jar", "-linux-aarch64.jar"))
    ]
    if len(javafx) != 3:
        raise RuntimeError(
            "Expected three native Linux JavaFX modules. Build on Linux first."
        )
    classpath = [str(ROOT / "target/test-classes"), str(ROOT / "target/classes")]
    classpath.extend(path for path in dependencies if "javafx-" not in Path(path).name)
    return [
        str(java),
        "-Dprism.order=sw",
        "--module-path",
        os.pathsep.join(javafx),
        "--add-modules",
        "javafx.controls",
        "-cp",
        os.pathsep.join(classpath),
        "de.appfabrik.naehplan.ui.GuiJourney",
    ]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--locales", nargs="+", choices=LOCALES, default=LOCALES)
    parser.add_argument("--mode", choices=("all", "capture", "verify"), default="all")
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    xvfb = os.environ.get("SEWINGPLANNER_XVFB") or shutil.which("Xvfb")
    if not xvfb:
        raise RuntimeError(
            "Install Xvfb or set SEWINGPLANNER_XVFB to a local Xvfb executable"
        )
    command = java_command()
    hashes = source_hashes()
    read_fd, write_fd = os.pipe()
    try:
        with (output / "xvfb.log").open("w") as server_log:
            server = subprocess.Popen(
                [
                    xvfb,
                    "-displayfd",
                    str(write_fd),
                    "-screen",
                    "0",
                    "1920x1080x24",
                    "-noreset",
                    "-nolisten",
                    "tcp",
                    "-ac",
                ],
                pass_fds=(write_fd,),
                stdout=server_log,
                stderr=server_log,
            )
        os.close(write_fd)
        write_fd = None
        try:
            ready, _, _ = select.select([read_fd], [], [], 15)
            if not ready:
                raise RuntimeError("Xvfb startup timed out")
            display = os.read(read_fd, 64).decode().strip()
            if not display.isdecimal():
                raise RuntimeError("Xvfb failed. See xvfb.log")
            env = os.environ.copy()
            env["DISPLAY"] = ":" + display
            env.pop("WAYLAND_DISPLAY", None)
            work_root = ROOT / "target/gui-work"
            work_root.mkdir(parents=True, exist_ok=True)
            with tempfile.TemporaryDirectory(
                prefix="international-", dir=work_root
            ) as work:

                def journey(locale, mode, data):
                    result = output / locale / mode
                    result.mkdir(parents=True, exist_ok=True)
                    with (result / "run.log").open("w") as log:
                        subprocess.run(
                            command + [str(data), str(result), locale, mode],
                            cwd=ROOT,
                            env=env,
                            stdout=log,
                            stderr=subprocess.STDOUT,
                            timeout=240,
                            check=True,
                        )
                    print(f"{locale}: {mode} passed", flush=True)
                    return result

                for locale in args.locales:
                    for mode in ("capture", "verify"):
                        if args.mode not in ("all", mode):
                            continue
                        data = Path(work) / locale / mode
                        data.mkdir(parents=True)
                        (data / ".synthetic-sewingplanner-demo").write_text(
                            "Fictional GUI data only\n"
                        )
                        result = journey(locale, mode, data)
                        if mode == "verify":
                            restart_locale = (
                                (result / "restart-locale.txt").read_text().strip()
                            )
                            # Keep restart evidence with the original verification language.
                            restart_output = result / "restart"
                            restart_output.mkdir()
                            with (restart_output / "run.log").open("w") as log:
                                subprocess.run(
                                    command
                                    + [
                                        str(data),
                                        str(restart_output),
                                        restart_locale,
                                        "restart",
                                    ],
                                    cwd=ROOT,
                                    env=env,
                                    stdout=log,
                                    stderr=subprocess.STDOUT,
                                    timeout=60,
                                    check=True,
                                )
                            print(
                                f"{locale}: fresh process in {restart_locale} passed",
                                flush=True,
                            )
            if source_hashes() != hashes:
                raise RuntimeError(
                    "Source files changed during capture. Rerun with a stable source tree."
                )
            manifest = {
                "platform": "Linux",
                "preview_only": True,
                "viewport": [1920, 1080],
                "renderer": "JavaFX software pipeline on isolated Xvfb",
                "source_base_commit": subprocess.check_output(
                    ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True
                ).strip(),
                "source_worktree_dirty": bool(
                    subprocess.check_output(
                        ["git", "status", "--porcelain"], cwd=ROOT, text=True
                    ).strip()
                ),
                "source_files_sha256": hashes,
                "locales": args.locales,
                "mode": args.mode,
                "created_utc": datetime.now(UTC).isoformat(),
                "host": platform.platform(),
                "evidence_sha256": {
                    str(path.relative_to(output)): hashlib.sha256(
                        path.read_bytes()
                    ).hexdigest()
                    for path in sorted(output.rglob("*"))
                    if path.is_file() and path.name != "xvfb.log"
                },
                "capture_method": "JavaFX Robot screen capture of production ProjectsApplication, with real mouse and keyboard events",
            }
            (output / "capture-manifest.json").write_text(
                json.dumps(manifest, indent=2, ensure_ascii=False) + "\n"
            )
        finally:
            server.terminate()
            try:
                server.wait(timeout=5)
            except subprocess.TimeoutExpired:
                server.kill()
                server.wait()
    finally:
        os.close(read_fd)
        if write_fd is not None:
            os.close(write_fd)


if __name__ == "__main__":
    main()
