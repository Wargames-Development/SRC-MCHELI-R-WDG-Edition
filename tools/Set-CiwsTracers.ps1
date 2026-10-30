param([Parameter(Mandatory = $true)][string]$MinecraftRoot, [switch]$Apply)
$ErrorActionPreference = 'Stop'
$weapons = Join-Path $MinecraftRoot 'mods/MCHELI-R-WDG-Edition-1.1.8_X194/assets/mcheli/weapons'
foreach ($gun in @(@('ac_cram_m61a1', '1.0', '4', '4000'), @('ac_hpj1130_30mm', '1.25', '8', '8000'))) {
    $path = Join-Path $weapons ($gun[0] + '.txt')
    $bytes = [IO.File]::ReadAllBytes($path)
    $bom = $bytes.Length -ge 3 -and $bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191
    $text = [Text.Encoding]::UTF8.GetString($bytes)
    if ($bom) { $text = $text.Substring(1) }
    if ($text -notmatch '(?im)^Type\s*=\s*MachineGun2\s*$' -or $text -match '(?im)^FMURBullet\s*=\s*\S+') {
        throw "CIWS must retain native MCHR machine-gun rounds: $path"
    }
    $newline = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
    $text = [regex]::Replace($text, '(?im)^\s*(?:FMURTracer(?:Glow|Scale)|RoundsPerShot)\s*=.*\r?\n?', '')
    foreach ($setting in @(@('Delay', '1'), @('HeatCount', '2'), @('MaxHeatCount', $gun[3]))) {
        $text = [regex]::Replace($text, '(?im)^' + $setting[0] + '\s*=.*$', $setting[0] + ' = ' + $setting[1])
    }
    $text = $text.TrimEnd("`r", "`n") + $newline + $newline + 'FMURTracerGlow = true' + $newline + 'FMURTracerScale = ' + $gun[1] + $newline + 'RoundsPerShot = ' + $gun[2] + $newline
    if ($Apply) { [IO.File]::WriteAllText($path, $text, [Text.UTF8Encoding]::new($bom)) }
    [pscustomobject]@{ Weapon = $gun[0]; Scale = $gun[1]; Color = 'Red'; RoundsPerSecond = [int]$gun[2] * 20; Applied = [bool]$Apply }
}
