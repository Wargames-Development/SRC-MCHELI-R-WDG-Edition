param(
    [Parameter(Mandatory = $true)][string]$MinecraftRoot,
    [switch]$Apply,
    [string]$ReportPath
)
$ErrorActionPreference = 'Stop'
$pack = Join-Path $MinecraftRoot 'mods/MCHELI-R-WDG-Edition-1.1.8_X194/assets/mcheli'
function Read-Definition([string]$Path) {
    $result = @{}
    foreach ($line in [IO.File]::ReadAllLines($Path)) {
        if ($line -match '^\s*([^;#/=]+?)\s*=\s*(.*?)\s*$') {
            $result[$matches[1].Trim().ToLowerInvariant()] = $matches[2]
        }
    }
    return $result
}
$definitions = @{}
$converted = @{}
$ammo = @{}
foreach ($item in @(@('50calammo', '50Cal300rdbox.txt'), @('762x51100rdbox', '762x51100rdbox.txt'),
    @('50calammoTracer', '50Cal300rdbox_Tracer.txt'), @('762x51100rdboxTracer', '762x51100rdbox_Tracer.txt'))) {
    $path = Join-Path $MinecraftRoot ('Flan/BFMC Gun Pack/bullets/' + $item[1])
    $text = [IO.File]::ReadAllText($path)
    if ($text -notmatch ('(?m)^ShortName\s+' + [regex]::Escape($item[0]) + '\s*$')) {
        throw "Unexpected FMUR ammunition short name: $path"
    }
    $ammo[$item[0]] = $path
    if ($item[0].EndsWith('Tracer') -and ($text -notmatch '(?m)^Tracer\s+True\s*$' -or $text -notmatch '(?m)^TracerColor\s+Red\s*$')) {
        throw "Expected red tracer ammunition: $path"
    }
}
foreach ($file in Get-ChildItem -LiteralPath (Join-Path $pack 'weapons') -Filter '*.txt' -Recurse) {
    $data = Read-Definition $file.FullName
    $definitions[$file.BaseName.ToLowerInvariant()] = $data
    if ($data.type -notmatch '^machinegun[12]$') { continue }
    $name = $file.BaseName + ' ' + $data.displayname
    $bullet = $null
    if ($name -match '(?i)7[.,]62|\bmg_762\b|mi8_762|\bm134\b|minigun') { $bullet = '762x51100rdbox' }
    elseif ($name -match '(?i)\.50\s*(cal|bmg)|50cal|12[.,]7\s*mm|m2hb|gunner_mg_127') { $bullet = '50calammo' }
    if (!$bullet) { continue }
    if ([double]$data.explosion -ne 0 -or [double]$data.canister -gt 0) { throw "Review explosive/canister gun: $file" }
    # Match FMUR's M2HB base damage (1 x ammo multiplier 14), rather than multiplying MCHR power by 14.
    $damage = if ($bullet -eq '50calammo') { '1' } else { $data.power }
    $converted[$file.BaseName.ToLowerInvariant()] = [ordered]@{
        weapon = $file.BaseName; displayName = $data.displayname; bullet = $bullet
        tracerBullet = $bullet + 'Tracer'; tracerInterval = 1
        baseDamage = $damage; speed = $data.acceleration; path = $file.FullName
    }
}
$vehicles = @()
$mounts = @()
$unresolved = @()
foreach ($category in @('tanks', 'vehicles', 'helicopters', 'planes')) {
    foreach ($file in Get-ChildItem -LiteralPath (Join-Path $pack $category) -Filter '*.txt' -Recurse) {
        $data = Read-Definition $file.FullName
        $vehicles += [ordered]@{ category = $category; name = $file.BaseName; displayName = $data.displayname }
        foreach ($line in [IO.File]::ReadAllLines($file.FullName)) {
            if ($line -match '^\s*Add(?:Turret)?Weapon\s*=\s*([^,\s]+)') {
                $weapon = $matches[1].ToLowerInvariant()
                if (!$definitions.ContainsKey($weapon)) { $unresolved += "$($file.Name): $weapon" }
                if ($converted.ContainsKey($weapon)) {
                    $mounts += [ordered]@{ category = $category; vehicle = $file.BaseName; weapon = $weapon; bullet = $converted[$weapon].bullet }
                }
            }
        }
    }
}
if ($Apply) {
    foreach ($entry in $converted.Values) {
        $bytes = [IO.File]::ReadAllBytes($entry.path)
        # These definitions are UTF-8. Preserve the original BOM and line endings.
        $bom = $bytes.Length -ge 3 -and $bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191
        $text = [Text.Encoding]::UTF8.GetString($bytes)
        if ($bom) { $text = $text.Substring(1) }
        $newline = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
        $text = [regex]::Replace($text, '(?im)^\s*FMUR(?:Bullet(?:Damage)?|TracerBullet|TracerInterval)\s*=.*\r?\n?', '')
        $text = $text.TrimEnd("`r", "`n") + $newline + $newline + 'FMURBullet = ' + $entry.bullet + $newline + 'FMURBulletDamage = ' + $entry.baseDamage + $newline
        $text += 'FMURTracerBullet = ' + $entry.tracerBullet + $newline + 'FMURTracerInterval = 1' + $newline
        [IO.File]::WriteAllText($entry.path, $text, [Text.UTF8Encoding]::new($bom))
    }
}
$report = [ordered]@{
    applied = [bool]$Apply; pack = $pack; ammunition = $ammo
    vehiclesAudited = $vehicles.Count; definitionsConverted = $converted.Count; mountsConverted = $mounts.Count
    definitions = @($converted.Values | Sort-Object { $_.weapon }); mounts = $mounts; vehicles = $vehicles; unresolvedReferences = $unresolved
}
if ($ReportPath) { [IO.File]::WriteAllText($ReportPath, ($report | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false)) }
$report | ConvertTo-Json -Depth 8
