param(
    [ValidateSet("app-image", "exe")]
    [string]$PackageType = "app-image"
)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\javac.exe") -or
    -not (Test-Path "$env:JAVA_HOME\bin\jpackage.exe")) {
    throw "JAVA_HOME muss auf ein vollständiges JDK 21 mit jpackage zeigen."
}
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Push-Location $Root
try {
    & .\mvnw.cmd --batch-mode --no-transfer-progress package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Maven-Build fehlgeschlagen." }
    [xml]$Pom = Get-Content pom.xml -Raw
    $Version = [string]$Pom.project.version
    $InputDirectory = Join-Path $Root "target\package-input"
    $Modules = Join-Path $Root "target\javafx-modules"
    $ImageDestination = Join-Path $Root "target\app-image"
    $Distribution = Join-Path $Root "target\dist"
    # Only generated staging paths are removed, never the user's collection.
    foreach ($GeneratedPath in @($InputDirectory, $Modules, $ImageDestination)) {
        if (Test-Path $GeneratedPath) { Remove-Item $GeneratedPath -Recurse -Force }
    }
    New-Item "$InputDirectory\legal", $Modules, $ImageDestination, $Distribution -ItemType Directory -Force | Out-Null
    & .\mvnw.cmd --batch-mode --no-transfer-progress dependency:copy-dependencies `
        -DincludeScope=runtime "-DoutputDirectory=$InputDirectory"
    if ($LASTEXITCODE -ne 0) { throw "Abhängigkeiten konnten nicht zusammengestellt werden." }
    Copy-Item "target\naehplan-$Version.jar" $InputDirectory
    Copy-Item THIRD_PARTY_NOTICES.md "$InputDirectory\legal"
    Copy-Item README.md "$InputDirectory\legal\USER_GUIDE.md"
    Copy-Item "$env:JAVA_HOME\release" "$InputDirectory\legal\BUILD_JAVA_RELEASE.txt"
    Copy-Item "third-party-licenses\*" "$InputDirectory\legal" -Recurse
    Get-ChildItem $InputDirectory -Filter "javafx-*.jar" | ForEach-Object {
        if ($_.Name -match '-win\.jar$') { Move-Item $_.FullName $Modules }
        else { Remove-Item $_.FullName }
    }
    $Common = @(
        "--name", "NaehPlan", "--app-version", $Version,
        "--vendor", "Wasiliy Strecker", "--description", "NähPlan - Nähprojekte mit Checkliste",
        "--icon", "$Root\assets\branding\icon.ico",
        "--input", $InputDirectory, "--main-jar", "naehplan-$Version.jar",
        "--main-class", "de.appfabrik.naehplan.Launcher",
        "--module-path", "$env:JAVA_HOME\jmods;$Modules",
        "--add-modules", "java.se,javafx.controls,jdk.unsupported,jdk.localedata",
        "--java-options", "--add-modules=javafx.controls",
        "--java-options", "-Dfile.encoding=UTF-8"
    )
    & "$env:JAVA_HOME\bin\jpackage.exe" @Common --type app-image --dest $ImageDestination
    if ($LASTEXITCODE -ne 0) { throw "App-Image konnte nicht gebaut werden." }
    $AppImage = Join-Path $ImageDestination "NaehPlan"
    $Launcher = Join-Path $AppImage "NaehPlan.exe"
    # GUI launchers on Windows do not reliably expose stdout. Wait for the process.
    $Check = Start-Process -FilePath $Launcher -ArgumentList "--version" -Wait -PassThru
    if ($Check.ExitCode -ne 0) { throw "Paketierter Launcher ist fehlgeschlagen." }
    if ($PackageType -eq "exe") {
        $Staging = Join-Path $Root "target\installer-staging"
        if (Test-Path $Staging) { Remove-Item $Staging -Recurse -Force }
        & "$env:JAVA_HOME\bin\jpackage.exe" --type exe --name NaehPlan `
            --app-image $AppImage --app-version $Version --vendor "Wasiliy Strecker" `
            --icon "$Root\assets\branding\icon.ico" `
            --dest $Staging --win-per-user-install --win-menu --win-shortcut `
            --win-menu-group "NaehPlan" --win-upgrade-uuid "777707fe-ad17-42ce-bc60-95ac92520de7"
        if ($LASTEXITCODE -ne 0) { throw "EXE-Paket fehlgeschlagen. Für JDK 21 wird WiX 3 benötigt." }
        $Output = Join-Path $Distribution "naehplan-$Version-windows-x64.exe"
        if (Test-Path $Output) { throw "Vorhandenes Paket wird nicht überschrieben: $Output" }
        Move-Item "$Staging\NaehPlan-$Version.exe" $Output
        Write-Host "EXE erstellt, native Abnahme separat durchführen: $Output"
    }
    Write-Host "App-Image erstellt und Launcher gestartet: $AppImage"
    Write-Host "GUI-Abnahme mit synthetischen Daten separat durchführen."
} finally {
    Pop-Location
}
