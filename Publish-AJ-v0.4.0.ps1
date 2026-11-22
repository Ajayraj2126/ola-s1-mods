#requires -Version 5.1
<#
.SYNOPSIS
  Verify and optionally publish S1 Control by AJ v0.4.0 using the ORIGINAL local AJ key.
.DESCRIPTION
  This script does not upload your keystore to GitHub or generate a replacement key.
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .\Publish-AJ-v0.4.0.ps1 -KeyStorePath C:\Keys\AJ.jks -Publish
#>
[CmdletBinding()]
param(
    [string]$KeyStorePath,
    [switch]$Publish,
    [switch]$SkipBuild
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$Repo = 'Ajayraj2126/ola-s1-mods'
$Tag = 'v0.4.0-aj'
$AppId = 'com.aj.s1control'
$ExpectedCert = '77ed171c551be82ca8b74e0ad664a60caa081e943fbd8588c2b9201217f2f4ee'

function Assert-Exit([string]$action) {
    if ($LASTEXITCODE -ne 0) {
        throw "$action failed (exit code $LASTEXITCODE)."
    }
}

function Read-SecureText([string]$prompt) {
    $secure = Read-Host -Prompt $prompt -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
        $secure.Dispose()
    }
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$androidDir = Join-Path $root 'android'
$buildDir = Join-Path $androidDir 'app\build\outputs\apk\release'
$apk = Join-Path $buildDir 'app-release.apk'
$namedApk = Join-Path $buildDir 'S1-Control-by-AJ-v0.4.0.apk'
$sums = Join-Path $buildDir 'SHA256SUMS.txt'

if (-not $env:AJ_RELEASE_STORE_FILE) {
    if (-not $KeyStorePath) { $KeyStorePath = Read-Host 'Existing AJ release keystore (.jks) path' }
    $env:AJ_RELEASE_STORE_FILE = (Resolve-Path -LiteralPath $KeyStorePath).Path
}
if (-not (Test-Path -LiteralPath $env:AJ_RELEASE_STORE_FILE -PathType Leaf)) {
    throw 'Original AJ keystore is missing. No signing key will be generated.'
}
if (-not $env:AJ_RELEASE_KEY_ALIAS) {
    $env:AJ_RELEASE_KEY_ALIAS = Read-Host 'Existing AJ signing key alias'
}
if (-not $env:AJ_RELEASE_STORE_PASSWORD -and -not $env:AJ_RELEASE_STORE_PASSWORD_FILE) {
    $env:AJ_RELEASE_STORE_PASSWORD = Read-SecureText 'Keystore password'
}
if (-not $env:AJ_RELEASE_KEY_PASSWORD -and -not $env:AJ_RELEASE_KEY_PASSWORD_FILE) {
    $env:AJ_RELEASE_KEY_PASSWORD = Read-SecureText 'Key password'
}

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
       elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
       else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$aapt = Join-Path $sdk 'build-tools\36.0.0\aapt.exe'
$apksigner = Join-Path $sdk 'build-tools\36.0.0\apksigner.bat'
if (-not (Test-Path $aapt) -or -not (Test-Path $apksigner)) {
    throw 'Android SDK Build Tools 36.0.0 are required.'
}

if (-not $SkipBuild) {
    Push-Location $androidDir
    try {
        & '.\gradlew.bat' --no-daemon ':app:testDebugUnitTest' ':app:lintDebug' ':app:assembleRelease' --stacktrace
        Assert-Exit 'Gradle build and tests'
    } finally { Pop-Location }
}
if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) {
    throw 'Signed app-release.apk missing.'
}

$badge = (& $aapt dump badging $apk | Out-String)
Assert-Exit 'APK manifest validation'
if (-not $badge.Contains("name='$AppId' versionCode='10' versionName='0.4.0-aj'")) {
    throw 'Incorrect APK ID or version. Refusing publication.'
}

$signerOutput = (& $apksigner verify --verbose --print-certs $apk | Out-String)
Assert-Exit 'APK signature verification'
$found = [regex]::Match($signerOutput, 'Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F]+)')
if (-not $found.Success) { throw 'APK certificate not found.' }
$actual = $found.Groups[1].Value.ToLowerInvariant()
if ($actual -ne $ExpectedCert) {
    throw "AJ signing key mismatch: $actual. Refusing publication."
}

Copy-Item -LiteralPath $apk -Destination $namedApk -Force
$hash = (Get-FileHash -LiteralPath $namedApk -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath $sums -Value "$hash  S1-Control-by-AJ-v0.4.0.apk" -Encoding Ascii
Write-Host "Verified signed AJ APK: $namedApk"
Write-Host "SHA-256: $hash"

if (-not $Publish) {
    Write-Host 'Local validation complete. Pass -Publish to publish.'
    exit 0
}

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw 'GitHub CLI is required. Install gh, then run gh auth login.'
}
& gh auth status
Assert-Exit 'GitHub login'

& gh release view $Tag --repo $Repo 1>$null 2>$null
if ($LASTEXITCODE -ne 0) {
    & gh release create $Tag --repo $Repo --target main --draft --title 'S1 Control by AJ v0.4.0 - v1.5 Mod Integration' --notes-file (Join-Path $root 'UPSTREAM-v1.5-PARITY.md')
    Assert-Exit 'Create draft release'
}
& gh release upload $Tag $namedApk $sums --clobber --repo $Repo
Assert-Exit 'Upload signed release APK and checksum'
& gh release edit $Tag --draft=false --prerelease --repo $Repo
Assert-Exit 'Publish prerelease'
Write-Host "Published: https://github.com/$Repo/releases/tag/$Tag"
