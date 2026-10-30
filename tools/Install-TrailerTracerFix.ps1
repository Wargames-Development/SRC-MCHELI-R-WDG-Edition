param(
    [Parameter(Mandatory = $true)][string]$MinecraftRoot,
    [Parameter(Mandatory = $true)][string]$MchrJar,
    [Parameter(Mandatory = $true)][string]$FmurJar
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root = [IO.Path]::GetFullPath($MinecraftRoot)
$pack = Join-Path $root 'mods/MCHELI-R-WDG-Edition-1.1.8_X194'
if (!(Test-Path -LiteralPath "$pack/assets/mcheli/weapons/ac_cram_m61a1.txt")) { throw 'Expected expanded MCHR pack not found' }
if (Get-Process java,javaw -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -match 'Minecraft|Wargames|Trailer' }) {
    throw 'Close the Minecraft game before installing mod binaries'
}
$mchr = [IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($MchrJar))
$fmur = [IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($FmurJar))
try {
    foreach ($pair in @(@($mchr, 'mcheli'), @($fmur, 'flansmod'))) {
        $reader = [IO.StreamReader]::new($pair[0].GetEntry('mcmod.info').Open())
        try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
        if ($metadata[0].modid -ne $pair[1]) { throw 'Unexpected mod archive identity' }
    }
    foreach ($required in @('mcheli/render/MCH_FMURTracer.class', 'mcheli/MCH_FMURUtil$MountedBulletHandler.class')) {
        if ($null -eq $mchr.GetEntry($required)) { throw "Missing MCHR integration class: $required" }
    }
    if ($null -eq $fmur.GetEntry('com/flansmod/client/TracerRenderApi.class')) { throw 'FMUR jar lacks the shared API' }
    $count = 0
    foreach ($entry in $mchr.Entries) {
        if (!(($entry.FullName.StartsWith('mcheli/') -and $entry.FullName.EndsWith('.class')) -or $entry.FullName -eq 'mcmod.info')) { continue }
        $destination = [IO.Path]::GetFullPath((Join-Path $pack $entry.FullName))
        if (!$destination.StartsWith($pack + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Archive entry escapes the expanded MCHR directory'
        }
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination)) | Out-Null
        [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destination, $true)
        ++$count
    }
    $destinationJar = Join-Path $root ('mods/' + [IO.Path]::GetFileName($FmurJar))
    Copy-Item -LiteralPath $FmurJar -Destination $destinationJar -Force
    if ((Get-FileHash -LiteralPath $destinationJar).Hash -ne (Get-FileHash -LiteralPath $FmurJar).Hash) { throw 'Installed FMUR hash mismatch' }
    # Retire only the exact previously installed jar after the new archive has been verified.
    $oldJar = Join-Path $root 'mods/FMUR-WDG-Edition-1.8.06_X189.jar'
    if ($oldJar -ne $destinationJar -and (Test-Path -LiteralPath $oldJar)) { Remove-Item -LiteralPath $oldJar }
    [pscustomobject]@{ MinecraftRoot = $root; MchrDirectory = $pack; MchrClassesAndMetadata = $count; FmurJar = $destinationJar; FmurSHA256 = (Get-FileHash -LiteralPath $destinationJar).Hash }
} finally { $mchr.Dispose(); $fmur.Dispose() }
