param(
    [string]$Output,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

if (-not (Test-Path (Join-Path $root '.git'))) {
    throw "Not a Git repository: $root"
}

$dirty = git -C $root status --porcelain
if ($dirty) {
    Write-Warning 'The working tree has uncommitted changes. This archive contains Git HEAD only. Commit first to include the latest fixes.'
}

$tracked = @(git -C $root ls-files)
if ($LASTEXITCODE -ne 0) {
    throw 'git ls-files failed.'
}

$secretPatterns = @(
    '^server/\.sync-token$',
    '^local\.properties$',
    '^server/mbhm_server\.db'
)
$generatedPrefixes = @(
    '.idea',
    '.gradle',
    'build',
    'app/build',
    'server/node_modules'
)

function Test-MatchesAny([string]$path, [string[]]$patterns) {
    foreach ($pattern in $patterns) {
        if ($path -match $pattern) { return $true }
    }
    return $false
}

$badSecrets = @($tracked | Where-Object { Test-MatchesAny $_ $secretPatterns })
if ($badSecrets.Count -gt 0) {
    throw "Refusing to package tracked secrets/data:`n$($badSecrets -join "`n")`nRemove them from Git history before sharing this repository."
}

$excludeSpecs = @()
$trackedGenerated = @()
$matchedPrefixes = @()
foreach ($prefix in $generatedPrefixes) {
    $matches = @($tracked | Where-Object { $_ -eq $prefix -or $_ -like "$prefix/*" })
    if ($matches.Count -gt 0) {
        $trackedGenerated += $matches
        $excludeSpecs += ":(exclude)$prefix"
        $matchedPrefixes += $prefix
    }
}

if ($trackedGenerated.Count -gt 0) {
    Write-Warning "Tracked IDE/generated files will be excluded from the archive: $($trackedGenerated.Count) file(s) under $($matchedPrefixes -join ', ')."
    Write-Warning 'Consider untracking these files (for example, git rm -r --cached .idea) and adding them to .gitignore.'
}

if (-not $Output) {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $Output = Join-Path (Split-Path -Parent $root) "MBHM-source-$stamp.zip"
}
$Output = [System.IO.Path]::GetFullPath($Output)

if ((Test-Path $Output) -and -not $Force) {
    throw "Output already exists: $Output. Use -Force to replace it."
}
if (Test-Path $Output) {
    Remove-Item $Output -Force
}

$gitArgs = @('-C', $root, 'archive', '--format=zip', '--prefix=MBHM/', "--output=$Output", 'HEAD')
if ($excludeSpecs.Count -gt 0) {
    $gitArgs += '--'
    $gitArgs += '.'
    $gitArgs += $excludeSpecs
}
& git @gitArgs
if ($LASTEXITCODE -ne 0) {
    throw 'git archive failed.'
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($Output)
try {
    $badEntries = @(
        $zip.Entries |
            ForEach-Object { $_.FullName -replace '^MBHM/', '' } |
            Where-Object { (Test-MatchesAny $_ $secretPatterns) -or ($trackedGenerated -contains $_) }
    )
} finally {
    $zip.Dispose()
}

if ($badEntries.Count -gt 0) {
    Remove-Item $Output -Force
    throw "Archive still contains forbidden files:`n$($badEntries -join "`n")"
}

$size = (Get-Item $Output).Length
Write-Host "Created source archive: $Output"
Write-Host ("Size: {0:N0} bytes" -f $size)
Write-Host 'Included: Git-tracked files only. Excluded: .git, IDE/Gradle state, build output, local.properties, server/.sync-token, and server database files.'
Write-Host 'Inspect the ZIP before sharing it.'
