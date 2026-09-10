$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputDirectory = Join-Path $projectRoot 'output'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

$apkSource = Join-Path $projectRoot 'app/build/outputs/apk/debug/app-debug.apk'
if (-not (Test-Path -LiteralPath $apkSource)) {
    throw 'Build the debug APK before packaging.'
}
Copy-Item -LiteralPath $apkSource -Destination (Join-Path $outputDirectory 'KeBiao-Java-debug.apk') -Force

Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$zipPath = Join-Path $outputDirectory 'KeBiao-Java-source.zip'
$zipStream = [System.IO.File]::Open($zipPath, [System.IO.FileMode]::Create)
$zip = New-Object System.IO.Compression.ZipArchive($zipStream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    $rootFiles = @('README.md', '.gitignore', 'build.gradle', 'settings.gradle', 'gradlew', 'gradlew.bat',
        'app/build.gradle', 'app/proguard-rules.pro')
    $files = @($rootFiles | ForEach-Object { Get-Item -LiteralPath (Join-Path $projectRoot $_) })
    foreach ($directory in @('app/src', 'gradle/wrapper', 'tools')) {
        $files += Get-ChildItem -LiteralPath (Join-Path $projectRoot $directory) -Recurse -File
    }
    foreach ($file in $files) {
        $relativePath = $file.FullName.Substring($projectRoot.Length + 1).Replace('\', '/')
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zip, $file.FullName, 'KeBiao-Java/' + $relativePath,
            [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
    $portableProperties = (Get-Content -LiteralPath (Join-Path $projectRoot 'gradle.properties') |
        Where-Object { $_ -notmatch '^org\.gradle\.java\.home=' }) -join "`n"
    $entry = $zip.CreateEntry('KeBiao-Java/gradle.properties')
    $writer = New-Object System.IO.StreamWriter($entry.Open(), (New-Object System.Text.UTF8Encoding($false)))
    try { $writer.Write($portableProperties + "`n") } finally { $writer.Dispose() }
} finally {
    $zip.Dispose()
    $zipStream.Dispose()
}
Get-Item -LiteralPath $zipPath, (Join-Path $outputDirectory 'KeBiao-Java-debug.apk') |
    Select-Object Name, Length
