[CmdletBinding()]
param(
    [ValidateSet("Debug", "Release")]
    [string]$Variant = "Debug",
    [switch]$Clean,
    [switch]$Bundle
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$gradleWrapper = Join-Path $projectRoot "gradlew.bat"

if (-not (Test-Path -LiteralPath $gradleWrapper)) {
    throw "Gradle wrapper not found at $gradleWrapper"
}

function Get-AndroidSdkPath {
    $candidates = @(
        $env:ANDROID_HOME,
        $env:ANDROID_SDK_ROOT,
        "D:\work\android",
        (Join-Path $env:LOCALAPPDATA "Android\Sdk")
    )

    $localProperties = Join-Path $projectRoot "local.properties"
    if (Test-Path -LiteralPath $localProperties) {
        $sdkLine = Get-Content -LiteralPath $localProperties |
            Where-Object { $_ -match "^\s*sdk\.dir\s*=" } |
            Select-Object -First 1
        if ($sdkLine) {
            $candidates += ($sdkLine -replace "^\s*sdk\.dir\s*=\s*", "" -replace "\\\\", "\\")
        }
    }

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    throw "Android SDK not found. Set ANDROID_HOME or add sdk.dir to local.properties."
}

$sdkPath = Get-AndroidSdkPath
if (-not (Test-Path -LiteralPath (Join-Path $sdkPath "platforms\android-36"))) {
    throw "Android SDK Platform 36 is required at $sdkPath."
}

$env:ANDROID_HOME = $sdkPath
$env:ANDROID_SDK_ROOT = $sdkPath

if ($Variant -eq "Release" -and -not (Test-Path -LiteralPath (Join-Path $projectRoot "signing.properties"))) {
    Write-Warning "No signing.properties found. Gradle will create an unsigned release artifact."
}

if ($Clean) {
    & $gradleWrapper clean
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$variantTask = $Variant.ToLowerInvariant()

if ($Bundle) {
    $task = ":app:bundle$($Variant.Substring(0, 1).ToUpperInvariant())$($Variant.Substring(1).ToLowerInvariant())"
} else {
    $task = ":app:assemble$($Variant.Substring(0, 1).ToUpperInvariant())$($Variant.Substring(1).ToLowerInvariant())"
}

& $gradleWrapper $task
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($Bundle) {
    $outputDirectory = Join-Path $projectRoot "app\build\outputs\bundle\$variantTask"
    $extension = "aab"
} else {
    $outputDirectory = Join-Path $projectRoot "app\build\outputs\apk\$variantTask"
    $extension = "apk"
}

$artifact = Get-ChildItem -LiteralPath $outputDirectory -Filter "*.$extension" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($artifact) {
    Write-Host "Built $Variant $($extension.ToUpperInvariant()): $($artifact.FullName)"
} else {
    throw "Build completed but no $extension was found in $outputDirectory"
}
