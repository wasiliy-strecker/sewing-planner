#!/usr/bin/env python3
"""Package a verified Linux preview with exact source provenance after the local commit."""

import argparse
import hashlib
import json
import shutil
import struct
import subprocess
from pathlib import Path

from packaged_smoke import file_hashes
from run_gui import LOCALES, ROOT, source_hashes

SHOTS = ("01-overview.png", "02-edit.png", "03-search.png", "04-language.png")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("captures", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--packaged-checks", type=Path, required=True)
    args = parser.parse_args()
    captures = args.captures.resolve()
    destination = args.destination.resolve()
    manifest = json.loads((captures / "capture-manifest.json").read_text())
    if manifest["source_files_sha256"] != source_hashes():
        raise RuntimeError(
            "The source changed after capture. Generate new captures first."
        )
    if set(manifest["locales"]) != set(LOCALES) or manifest["mode"] != "all":
        raise RuntimeError("All six capture and verification journeys are required")
    if subprocess.check_output(
        ["git", "status", "--porcelain"], cwd=ROOT, text=True
    ).strip():
        raise RuntimeError(
            "Commit the verified app source before recording delivery provenance"
        )
    if destination.exists():
        raise RuntimeError("An existing delivery must not be overwritten")
    for locale in LOCALES:
        for phase in ("capture", "verify", "verify/restart"):
            if not (captures / locale / phase / "checks.txt").is_file():
                raise RuntimeError(f"Missing successful verification: {locale}/{phase}")
        for shot in SHOTS:
            data = (captures / locale / "capture" / shot).read_bytes()
            if data[:8] != b"\x89PNG\r\n\x1a\n" or struct.unpack(
                ">II", data[16:24]
            ) != (1920, 1080):
                raise RuntimeError(f"Invalid screenshot: {locale}/{shot}")
            if len(data) > 50 * 1024 * 1024:
                raise RuntimeError("Screenshot is too large")
    for name, digest in manifest["evidence_sha256"].items():
        if hashlib.sha256((captures / name).read_bytes()).hexdigest() != digest:
            raise RuntimeError(f"Capture evidence changed: {name}")
    packaged = args.packaged_checks.resolve()
    package_manifest = json.loads((packaged / "manifest.json").read_text())
    if package_manifest["source_files_sha256"] != source_hashes():
        raise RuntimeError("Packaged launcher verification used different sources")
    if not (packaged / "checks.txt").is_file():
        raise RuntimeError("Missing successful packaged launcher checks")
    if package_manifest["package_sha256"] != file_hashes(
        ROOT / "target/app-image/NaehPlan"
    ):
        raise RuntimeError("The package changed after verification")
    destination.mkdir(parents=True)
    for locale in LOCALES:
        target = destination / "Store" / locale
        shutil.copytree(ROOT / "store-listings" / locale, target / "text")
        (target / "screenshots").mkdir()
        for shot in SHOTS:
            shutil.copy2(
                captures / locale / "capture" / shot, target / "screenshots" / shot
            )
    shutil.copy2(
        ROOT / "store-listings/default-language.txt",
        destination / "Store/default-language.txt",
    )
    shutil.copytree(ROOT / "assets/branding", destination / "Branding")
    shutil.copytree(captures, destination / "Verification")
    shutil.copytree(packaged, destination / "Verification/packaged-launcher")
    commit = subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True
    ).strip()
    manifest["verified_source_commit"] = commit
    manifest["source_commit_confirmed_by_file_hashes"] = True
    manifest["store_ready"] = False
    manifest["screenshots"] = 24
    manifest["packaged_launcher_verified"] = True
    (destination / "manifest.json").write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False) + "\n"
    )
    (destination / "LIESMICH.txt").write_text(
        "NähPlan – Linux-Vorschau in sechs Sprachen\n\n"
        "24 echte Bildschirmaufnahmen mit synthetischen Beispieldaten.\n"
        "Plattform: Linux, JavaFX, isolierter virtueller Xvfb-Bildschirm.\n"
        "Die Aufnahmen sind eine Vorschau und keine Windows-Store-Abnahme.\n\n"
        "Store enthält je Sprache vier Bilder und direkt kopierbare Textdateien.\n"
        "Branding enthält SVG, PNG und Windows-ICO.\n"
        "Verification enthält die GUI-Prüfungen und Herkunftsnachweise.\n"
        f"Geprüfter lokaler Quellcommit: {commit}\n\n"
        "Offen: Windows-Paketierung, native Windows-Abnahme, endgültige\n"
        "Windows-Store-Bilder, Produktidentität und Store-Einreichung.\n"
    )
    checksums = []
    for path in sorted(destination.rglob("*")):
        if path.is_file():
            checksums.append(
                hashlib.sha256(path.read_bytes()).hexdigest()
                + "  "
                + str(path.relative_to(destination))
            )
    (destination / "SHA256SUMS.txt").write_text("\n".join(checksums) + "\n")
    print(destination)
    print(
        "Verified delivery: six languages, 24 screenshots, source commit " + commit[:7]
    )


if __name__ == "__main__":
    main()
