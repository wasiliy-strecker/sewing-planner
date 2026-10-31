#!/usr/bin/env python3
"""Check, capture and deliver German Store content. No Mac builds or uploads."""

import argparse
import base64
import hashlib
import html
import json
import os
import re
import select
import shutil
import subprocess
import tempfile
import urllib.request
from datetime import UTC, datetime
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
LISTING = ROOT / "store-listings/mac-app-store/de-DE"
DEMO = ROOT / "assets/store_demo/sewing-planner-de-DE.json"
GUIDE = ROOT / "docs/mac-app-store-fields.md"
IMAGES = (
    "01-overview.png",
    "02-edit-checklist.png",
    "03-search.png",
    "04-json-export.png",
)
FIELDS = (
    "name",
    "subtitle",
    "promotional-text",
    "description",
    "keywords",
    "support-url",
    "marketing-url",
    "copyright",
    "version",
    "bundle-id",
    "review-notes",
    "captions",
    "release-notes",
)
EXPECTED_CHECKS = {
    "syntheticProjects": 5,
    "photos": 3,
    "checklistSteps": 15,
    "allStatuses": True,
    "allColors": True,
    "exactImport": True,
    "duplicateImportSkipped": 5,
    "uiExportWithActiveSearch": True,
    "exactExport": True,
}


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_files():
    files = [ROOT / "pom.xml", ROOT / "mvnw", GUIDE]
    for folder in (
        "src/main",
        "scripts",
        "assets/store_demo",
        "store-listings/mac-app-store",
        ".mvn",
    ):
        files.extend(
            path
            for path in (ROOT / folder).rglob("*")
            if path.is_file() and "__pycache__" not in path.parts
        )
    return {path.relative_to(ROOT).as_posix(): sha256(path) for path in sorted(files)}


def source_digest():
    encoded = json.dumps(source_files(), sort_keys=True).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def java_home():
    home = Path(
        os.environ.get(
            "JAVA_HOME", str(Path.home() / ".local/share/naehplan/jdk-21.0.13+9")
        )
    )
    if not (home / "bin/javac").is_file():
        raise ValueError("Set JAVA_HOME to the app's Eclipse Temurin JDK 21.0.13+9")
    return home


def check_content(check_links=False):
    values = {}
    for name in FIELDS:
        raw = (LISTING / (name + ".txt")).read_text(encoding="utf-8")
        value = raw.strip()
        if (
            not value
            or ";" in value
            or re.search(r"(?m)^\s*(?:[-*#]|\d+[.)])\s", value)
        ):
            raise ValueError("Empty or formatted field: " + name)
        if raw != value + "\n":
            raise ValueError("Unexpected surrounding whitespace in " + name)
        values[name] = value
    for name, maximum in {
        "name": 30,
        "subtitle": 30,
        "promotional-text": 170,
        "description": 4000,
        "copyright": 200,
        "release-notes": 4000,
    }.items():
        if len(values[name]) > maximum:
            raise ValueError(f"{name} exceeds {maximum} characters")
    for name, maximum in {"keywords": 100, "review-notes": 4000}.items():
        if len(values[name].encode("utf-8")) > maximum:
            raise ValueError(f"{name} exceeds {maximum} UTF-8 bytes")
    for name, expected in {
        "name": "Sewing Planner",
        "version": "1.0",
        "bundle-id": "de.appfabrik.sewingplanner",
    }.items():
        if values[name] != expected:
            raise ValueError("Unexpected app identity in " + name)
    if len(values["captions"].splitlines()) != len(IMAGES):
        raise ValueError("Exactly four captions required")
    if any(len(word.strip()) < 3 for word in values["keywords"].split(",")):
        raise ValueError("Each keyword must have at least three characters")
    sample = json.loads(DEMO.read_text(encoding="utf-8"))
    if (
        sample.get("format") != "sewing-planner.collection"
        or sample.get("version") != 1
    ):
        raise ValueError("Wrong demonstration format")
    projects = sample["projects"]
    if len(projects) != 5 or {p["status"] for p in projects} != {
        "planned",
        "started",
        "finished",
    }:
        raise ValueError("Five projects with all three statuses required")
    if any(not re.fullmatch(r"#[0-9A-F]{6}", p["color"]) for p in projects):
        raise ValueError("Demonstration colors must be hex values")
    steps = [step for project in projects for step in project["steps"]]
    if len(steps) != 15 or {step["completed"] for step in steps} != {True, False}:
        raise ValueError("Fifteen open and completed checklist steps required")
    if len({step["id"] for step in steps}) != 15:
        raise ValueError("Checklist identifiers must be unique")
    photos = [
        base64.b64decode(p["photoBase64"], validate=True)
        for p in projects
        if p["photoBase64"] is not None
    ]
    originals = {
        (ROOT / "assets/store_demo/images" / name).read_bytes()
        for name in ("lilac.jpg", "rose.png", "sage.png")
    }
    if len(photos) != 3 or set(photos) != originals:
        raise ValueError("Use exactly the three app-owned demonstration illustrations")
    links = []
    for name in ("support-url", "marketing-url"):
        if not values[name].startswith("https://www.appfabrik-ai.de/"):
            raise ValueError("Unexpected public URL: " + values[name])
        if check_links:
            request = urllib.request.Request(
                values[name],
                headers={"User-Agent": "SewingPlanner-ContentCheck/1.0"},
            )
            with urllib.request.urlopen(request, timeout=20) as response:
                if response.status != 200 or "text/html" not in response.headers.get(
                    "Content-Type", ""
                ):
                    raise ValueError("Public HTML page unavailable: " + values[name])
                links.append(
                    {
                        "url": values[name],
                        "status": response.status,
                        "resolvedUrl": response.geturl(),
                    }
                )
    return {
        "checkedUtc": datetime.now(UTC).isoformat(),
        "fieldLengths": {
            name: {"characters": len(value), "utf8Bytes": len(value.encode("utf-8"))}
            for name, value in values.items()
        },
        "syntheticProjects": len(projects),
        "originalIllustrations": len(photos),
        "publicLinks": links,
    }


def validate_png(path):
    with Image.open(path) as image:
        if image.format != "PNG" or image.size != (2560, 1600) or image.mode != "RGB":
            raise ValueError("Expected a 2560 × 1600 RGB PNG: " + str(path))
        image.verify()


def existing_runtime():
    """Read existing artifacts only. Never invoke Maven or build the application."""
    app = ROOT / "target/app-image/Sewing Planner/lib/app"
    modules = ROOT / "target/javafx-modules"
    files = sorted(app.glob("*.jar")) + sorted(modules.glob("*.jar"))
    if len(list(app.glob("*.jar"))) != 4 or len(list(modules.glob("*.jar"))) != 3:
        raise ValueError(
            "The existing Sewing Planner image and JavaFX modules are required"
        )
    jar = app / "naehplan-0.1.0.jar"
    if not jar.is_file() or sha256(jar) != sha256(ROOT / "target/naehplan-0.1.0.jar"):
        raise ValueError(
            "The existing application JAR must match the last verified JAR"
        )
    changed = subprocess.check_output(
        ["git", "diff", "HEAD", "--name-only", "src/main", "pom.xml"],
        cwd=ROOT,
        text=True,
    )
    if changed.strip():
        raise ValueError(
            "App sources changed. This asset workflow does not rebuild them"
        )
    return {path.relative_to(ROOT).as_posix(): sha256(path) for path in files}


def capture(output):
    report = check_content(check_links=True)
    xvfb = os.environ.get("SEWINGPLANNER_XVFB") or shutil.which("Xvfb")
    if not xvfb or not shutil.which("xdotool"):
        raise ValueError(
            "Xvfb and xdotool are required. SEWINGPLANNER_XVFB may select Xvfb"
        )
    output.mkdir(parents=True, exist_ok=False)
    home = java_home()
    env = os.environ.copy()
    env["JAVA_HOME"] = str(home)
    env["PATH"] = str(home / "bin") + os.pathsep + env.get("PATH", "")
    sources = source_files()
    runtime = existing_runtime()
    javafx = [str(ROOT / path) for path in runtime if "javafx-modules/" in path]
    classpath = os.pathsep.join(
        str(ROOT / path) for path in runtime if "lib/app/" in path
    )
    write_json(
        output / "runtime-info.json", {"appBuildExecuted": False, "files": runtime}
    )
    read_fd, write_fd = os.pipe()
    server = None
    try:
        with (output / "xvfb.log").open("w", encoding="utf-8") as log:
            server = subprocess.Popen(
                [
                    xvfb,
                    "-displayfd",
                    str(write_fd),
                    "-screen",
                    "0",
                    "2560x1600x24",
                    "-noreset",
                    "-nolisten",
                    "tcp",
                    "-ac",
                ],
                pass_fds=(write_fd,),
                stdout=log,
                stderr=log,
            )
        os.close(write_fd)
        write_fd = None
        if not select.select([read_fd], [], [], 15)[0]:
            raise RuntimeError("Xvfb startup timed out")
        display = os.read(read_fd, 64).decode().strip()
        if not display.isdecimal():
            raise RuntimeError("Invalid Xvfb display. See xvfb.log")
        env["DISPLAY"] = ":" + display
        env.pop("WAYLAND_DISPLAY", None)
        with tempfile.TemporaryDirectory(
            prefix="sewing-planner-store-content-"
        ) as temporary:
            data = Path(temporary)
            (data / ".synthetic-sewingplanner-demo").write_text(
                "Synthetic asset data only\n"
            )
            command = [
                str(home / "bin/java"),
                "-Dprism.order=sw",
                "-Dglass.gtk.uiScale=1.6",
                "--module-path",
                os.pathsep.join(javafx),
                "--add-modules=javafx.controls",
                "-cp",
                classpath,
                str(ROOT / "scripts/store_assets/MacStoreContentCapture.java"),
                str(data),
                str(output),
            ]
            with (output / "capture.log").open("w", encoding="utf-8") as log:
                subprocess.run(
                    command,
                    cwd=ROOT,
                    env=env,
                    stdout=log,
                    stderr=subprocess.STDOUT,
                    timeout=180,
                    check=True,
                )
    finally:
        os.close(read_fd)
        if write_fd is not None:
            os.close(write_fd)
        if server is not None:
            server.terminate()
            try:
                server.wait(timeout=5)
            except subprocess.TimeoutExpired:
                server.kill()
                server.wait()
    if runtime != existing_runtime():
        raise ValueError("Application artifacts changed during capture")
    if sources != source_files():
        raise ValueError("Inputs changed during capture. Repeat with stable sources")
    for name in IMAGES:
        validate_png(output / name)
    checks = json.loads((output / "asset-checks.json").read_text())
    if checks != EXPECTED_CHECKS:
        raise ValueError("Incomplete synthetic data or export verification")
    write_json(output / "content-checks.json", report)
    evidence = (
        *IMAGES,
        "asset-checks.json",
        "privacy-de-DE.txt",
        "capture.log",
        "runtime-info.json",
        "content-checks.json",
    )
    write_json(
        output / "capture-manifest.json",
        {
            "platform": "Linux",
            "previewOnly": True,
            "viewport": [2560, 1600],
            "locale": "de-DE",
            "sourceDigest": source_digest(),
            "sourceFiles": sources,
            "createdUtc": datetime.now(UTC).isoformat(),
            "captureMethod": "Production JavaFX application on isolated Xvfb with synthetic JSON data",
            "exportView": "Actual successful UI export with all projects despite an active search",
            "appBuildExecuted": False,
            "fullTestSuiteExecuted": False,
            "macAcceptance": False,
            "files": {name: sha256(output / name) for name in evidence},
        },
    )
    validate_capture(output)
    print(output)


def write_json(path, value):
    path.write_text(
        json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )


def validate_capture(folder):
    manifest = json.loads((folder / "capture-manifest.json").read_text())
    if (
        manifest.get("sourceDigest") != source_digest()
        or manifest.get("sourceFiles") != source_files()
        or manifest.get("platform") != "Linux"
        or manifest.get("previewOnly") is not True
        or manifest.get("viewport") != [2560, 1600]
        or manifest.get("locale") != "de-DE"
    ):
        raise ValueError(
            "Drafts must be marked as Linux previews and match the current sources"
        )
    expected = set(IMAGES) | {
        "asset-checks.json",
        "privacy-de-DE.txt",
        "capture.log",
        "runtime-info.json",
        "content-checks.json",
    }
    if set(manifest["files"]) != expected:
        raise ValueError("Capture evidence is incomplete")
    for name, digest in manifest["files"].items():
        if sha256(folder / name) != digest:
            raise ValueError("Capture artifact changed: " + name)
    for name in IMAGES:
        validate_png(folder / name)
    if json.loads((folder / "asset-checks.json").read_text()) != EXPECTED_CHECKS:
        raise ValueError("Synthetic data checks did not pass")
    runtime = json.loads((folder / "runtime-info.json").read_text())
    if runtime != {"appBuildExecuted": False, "files": existing_runtime()}:
        raise ValueError("The existing application artifacts changed after capture")
    return manifest


def assemble(captures, output):
    report = check_content(check_links=True)
    manifest = validate_capture(captures)
    changed = subprocess.check_output(
        ["git", "diff", "HEAD", "--name-only"], cwd=ROOT, text=True
    )
    if changed.strip():
        raise ValueError(
            "Commit the checked Store content before assembling the handoff"
        )
    tracked = set(
        subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT, text=True).split(
            "\0"
        )
    )
    if set(source_files()) - tracked:
        raise ValueError(
            "All capture inputs must be tracked. Personal exports are never included"
        )
    revision = subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True
    ).strip()
    output.mkdir(parents=True, exist_ok=False)
    store = output / "Store/de-DE"
    store.mkdir(parents=True)
    for name in FIELDS:
        shutil.copy2(LISTING / (name + ".txt"), store)
    demo = output / "Demo"
    demo.mkdir()
    shutil.copy2(DEMO, demo)
    shutil.copy2(ROOT / "assets/store_demo/mac-content-demo.md", demo / "README.md")
    drafts = output / "Screenshot-Drafts-Linux/de-DE"
    drafts.mkdir(parents=True)
    for name in IMAGES:
        shutil.copy2(captures / name, drafts)
    (drafts.parent / "ENTWUERFE-NICHT-HOCHLADEN.txt").write_text(
        "Echte Linux-Aufnahmen der aktuellen App mit synthetischen Daten.\n"
        "Noch keine Mac-Aufnahmen und keine native Mac-Abnahme.\n"
        "Die vier Ansichten auf dem späteren Mac-Build neu aufnehmen.\n",
        encoding="utf-8",
    )
    (output / "Screenshots-Final-Mac/de-DE").mkdir(parents=True)
    privacy = output / "Privacy"
    privacy.mkdir()
    policy = (captures / "privacy-de-DE.txt").read_text(encoding="utf-8")
    (privacy / "de-DE.txt").write_text(policy, encoding="utf-8")
    rendered = (
        '<!doctype html><html lang="de"><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1">'
        "<title>Datenschutz – Sewing Planner</title><style>"
        "body{max-width:52rem;margin:3rem auto;padding:0 1rem;font:18px/1.6 system-ui}"
        "p{white-space:pre-line}</style><main>"
    )
    rendered += "".join(
        "<p>" + html.escape(part) + "</p>" for part in policy.split("\n\n")
    )
    (privacy / "index.html").write_text(rendered + "</main></html>\n", encoding="utf-8")
    (privacy / "URL-NOCH-OFFEN.txt").write_text(
        "Die vollständige Erklärung vor Einreichung öffentlich ohne Anmeldung bereitstellen.\n"
        "Eine lokale HTML-Datei ist keine öffentliche Datenschutz-URL.\n"
        "Beim technischen Mac-Release die Bezugskanal-Hinweise auf Apple erweitern.\n"
        "App-Dialog und veröffentlichte Erklärung weiterhin aus derselben Quelle erzeugen.\n",
        encoding="utf-8",
    )
    shutil.copy2(GUIDE, output / "JETZT-IN-APP-STORE-CONNECT.md")
    verification = output / "Verification"
    verification.mkdir()
    for name in (
        "capture-manifest.json",
        "asset-checks.json",
        "capture.log",
        "runtime-info.json",
        "content-checks.json",
    ):
        shutil.copy2(captures / name, verification)
    write_json(verification / "delivery-content-checks.json", report)
    write_json(
        output / "content-info.json",
        {
            "name": "Sewing Planner",
            "version": "1.0",
            "bundleId": "de.appfabrik.sewingplanner",
            "storeLocale": "de-DE",
            "sourceCommit": revision,
            "sourceDigest": manifest["sourceDigest"],
            "createdUtc": datetime.now(UTC).isoformat(),
            "scope": "Store content only",
            "screenshots": "Linux drafts",
            "publicPrivacyURL": "pending",
            "appBuildExecuted": False,
            "macBuild": False,
            "bundleIdRegistration": "not verified",
            "macAcceptance": False,
            "signing": False,
            "upload": False,
            "storeReady": False,
        },
    )
    (output / "ZUERST-LESEN.txt").write_text(
        "Sewing Planner – deutsche Store-Inhalte für macOS 1.0\n\n"
        "JETZT-IN-APP-STORE-CONNECT.md erklärt die Felder der Versionsseite.\n"
        "Store/de-DE enthält direkt kopierbare Feldtexte ohne Listenmarker.\n"
        "Screenshot-Drafts-Linux enthält vier echte Entwürfe in 2560 × 1600 RGB.\n"
        "Die endgültigen Mac-Screenshots fehlen noch. Kein Vorschauvideo.\n"
        "Demo enthält ausschließlich fünf erfundene Projekte mit eigenen Illustrationen.\n"
        "Privacy enthält die bestehende deutsche In-App-Erklärung als TXT und HTML.\n"
        "Die öffentliche Datenschutz-URL und persönliche Prüferkontakte bleiben offen.\n"
        "Bundle-ID ist vorgesehen, Registrierung nicht geprüft.\n"
        "Mac-Paketierung, Bundle-Konfiguration, Signierung und Upload folgen separat.\n"
        "Die vorhandene App wurde verwendet. Kein App-Build und kein Maven-Aufruf.\n"
        "Synthetischer Import, Fotos, Farben, Status, Checklisten und UI-Export wurden geprüft.\n"
        "Es wurde keine vollständige Testsuite und keine native Mac-Abnahme ausgeführt.\n"
        f"Quellcommit: {revision}\n",
        encoding="utf-8",
    )
    sums = [
        f"{sha256(path)}  {path.relative_to(output).as_posix()}\n"
        for path in sorted(output.rglob("*"))
        if path.is_file()
    ]
    (output / "SHA256SUMS.txt").write_text("".join(sums), encoding="utf-8")
    print(output)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    subcommands = parser.add_subparsers(dest="command", required=True)
    check = subcommands.add_parser("check")
    check.add_argument("--check-links", action="store_true")
    capture_parser = subcommands.add_parser("capture")
    capture_parser.add_argument("--output", type=Path, required=True)
    assemble_parser = subcommands.add_parser("assemble")
    assemble_parser.add_argument("--captures", type=Path, required=True)
    assemble_parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.command == "check":
        print(json.dumps(check_content(args.check_links), indent=2, ensure_ascii=False))
    elif args.command == "capture":
        capture(args.output.resolve())
    else:
        assemble(args.captures.resolve(), args.output.resolve())


if __name__ == "__main__":
    main()
