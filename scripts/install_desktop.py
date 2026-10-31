#!/usr/bin/env python3
"""Register a Linux app image for this user without touching application data."""

import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

APP_ID = "de.appfabrik.naehplan"
APP_NAME = "Sewing Planner"
LAUNCHER = "Sewing Planner"
WINDOW_CLASS = "de.appfabrik.naehplan.ui.ProjectsApplication"
MARKER = "X-AppFabrik-Managed=true"


def desktop_value(value):
    return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")


def exec_argument(value):
    value = value.replace("%", "%%")
    value = "".join("\\" + char if char in '\\"`$' else char for char in value)
    return desktop_value('"' + value + '"')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app-image", type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument("--data-dir", type=Path, help="Optional existing collection location for this launcher")
    args = parser.parse_args()
    if not sys.platform.startswith("linux"):
        parser.error("This desktop integration is for Linux only")
    app_image = args.app_image.expanduser().resolve()
    launcher = app_image / "bin" / LAUNCHER
    icons = app_image / "lib/app/desktop-icons"
    if not launcher.is_file() or not os.access(launcher, os.X_OK):
        parser.error(f"No executable app launcher at {launcher}")
    if "=" in str(launcher):
        parser.error("The desktop specification does not allow '=' in the executable path")
    sizes = (16, 32, 48, 64, 128, 256, 512, 1024)
    for name in [*(f"icon-{size}.png" for size in sizes), "icon.svg"]:
        if not (icons / name).is_file():
            parser.error(f"Missing packaged icon: {icons / name}")
    data_home = Path(os.environ.get("XDG_DATA_HOME") or Path.home() / ".local/share")
    if not data_home.is_absolute():
        data_home = Path.home() / ".local/share"
    applications = data_home / "applications"
    desktop = applications / f"{APP_ID}.desktop"
    if desktop.exists() and MARKER not in desktop.read_text(encoding="utf-8").splitlines():
        parser.error(f"Existing unmanaged application entry is preserved: {desktop}")
    command = [str(launcher)]
    if args.data_dir is not None:
        command.append("--data-dir=" + str(args.data_dir.expanduser().resolve()))
    entry = "\n".join([
        "[Desktop Entry]",
        "Type=Application",
        "Name=" + APP_NAME,
        "Exec=" + " ".join(exec_argument(value) for value in command),
        "TryExec=" + desktop_value(str(launcher)),
        "Icon=" + APP_ID,
        "Terminal=false",
        "Categories=Office;",
        "StartupNotify=false",
        "StartupWMClass=" + WINDOW_CLASS,
        MARKER,
        "",
    ])
    theme = data_home / "icons/hicolor"
    for size in sizes:
        destination = theme / f"{size}x{size}/apps/{APP_ID}.png"
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(icons / f"icon-{size}.png", destination)
    vector = theme / f"scalable/apps/{APP_ID}.svg"
    vector.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(icons / "icon.svg", vector)
    applications.mkdir(parents=True, exist_ok=True)
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=applications, delete=False) as handle:
            temporary = Path(handle.name)
            handle.write(entry)
        temporary.chmod(0o644)
        temporary.replace(desktop)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)
    for tool, arguments in (("update-desktop-database", [str(applications)]),
                            ("gtk-update-icon-cache", ["-f", "-t", str(theme)])):
        executable = shutil.which(tool)
        if executable:
            result = subprocess.run([executable, *arguments], capture_output=True, text=True)
            if result.returncode:
                print(f"Optional cache update failed: {tool}: {result.stderr.strip()}", file=sys.stderr)
    print(f"Registered {APP_NAME}: {desktop}")


if __name__ == "__main__":
    main()
