param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArguments = @(':app:assembleDebug', ':app:testDebugUnitTest')
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$javaHomeBefore = $env:JAVA_HOME
$javaOptionsBefore = $env:JAVA_TOOL_OPTIONS
$properties = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $projectRoot 'gradle.properties') -Raw)

try {
    if ($properties['org.gradle.java.home']) {
        $env:JAVA_HOME = $properties['org.gradle.java.home']
    }
    # An unavailable Unix socket directory makes this Windows JDK use TCP for its NIO pipe.
    $socketDirectory = Join-Path $projectRoot '.gradle/tcp-only/not-created'
    $env:JAVA_TOOL_OPTIONS = ($javaOptionsBefore + ' "-Djdk.net.unixdomain.tmpdir=' + $socketDirectory + '"').Trim()
    Push-Location $projectRoot
    try {
        & .\gradlew.bat @GradleArguments --no-daemon --console=plain
        $buildExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} finally {
    $env:JAVA_HOME = $javaHomeBefore
    $env:JAVA_TOOL_OPTIONS = $javaOptionsBefore
}
exit $buildExitCode
