# Generates the 50 shipped levels: 5 packs x 10 levels.
# Each pack is a different basic shape and grows more complex with level order:
#   pack 1 = Square, pack 2 = Diamond, pack 3 = Cross, pack 4 = Ring, pack 5 = Star.
# Later packs also contain bent (multi-cell) arrows whose path turns through its
# body cells. Directions are assigned by simulating a valid removal order
# outside-in, so every generated level is solvable under the game's greedy rule.
[CmdletBinding()]
param(
    [int]$SeedBase = 20260930
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$levelsDir = Join-Path $root 'app\src\main\assets\levels'
New-Item -ItemType Directory -Force -Path $levelsDir | Out-Null

$dirVec = @{ UP = @(-1, 0); DOWN = @(1, 0); LEFT = @(0, -1); RIGHT = @(0, 1) }
$dirNames = @('UP', 'DOWN', 'LEFT', 'RIGHT')

function Get-Shape([int]$pack, [int]$level) {
    switch ($pack) {
        1 { $n = if ($level -le 4) { 4 } else { 5 } }
        2 { $n = if ($level -le 5) { 5 } else { 6 } }
        3 { $n = if ($level -le 3) { 5 } elseif ($level -le 7) { 6 } else { 7 } }
        4 { $n = if ($level -le 5) { 6 } else { 7 } }
        default { $n = 7 }
    }

    $cr = [int][math]::Floor(($n - 1) / 2)
    $half = [int][math]::Floor($n / 2)
    $cells = New-Object System.Collections.Generic.List[object]

    for ($r = 0; $r -lt $n; $r++) {
        for ($c = 0; $c -lt $n; $c++) {
            $dr = [math]::Abs($r - $cr)
            $dc = [math]::Abs($c - $cr)
            $in = $false
            switch ($pack) {
                1 { $in = $true }
                2 { $in = (($dr + $dc) -le $half) }
                3 { $in = (($dr -le 1) -or ($dc -le 1)) }
                4 { $in = ($r -eq 0 -or $r -eq ($n - 1) -or $c -eq 0 -or $c -eq ($n - 1)) }
                default { $in = (($dr + $dc) -le $half) -or ($dr -le 1) -or ($dc -le 1) }
            }
            if ($pack -eq 1 -and $level -gt 6 -and $dr -eq 0 -and $dc -eq 0) { $in = $false }
            if ($in) { $cells.Add(@{ R = $r; C = $c }) }
        }
    }

    return @{ N = $n; Cells = $cells }
}

# How likely a bent arrow is attempted, and the most body cells behind the head.
function Get-BendPolicy([int]$pack, [int]$level) {
    switch ($pack) {
        1 { return @{ Chance = 0.0; MaxExtra = 0 } }
        2 { return @{ Chance = 0.15 + 0.03 * $level; MaxExtra = 1 } }
        3 { return @{ Chance = 0.30 + 0.03 * $level; MaxExtra = 1 } }
        4 { return @{ Chance = 0.40 + 0.03 * $level; MaxExtra = 2 } }
        default { return @{ Chance = 0.45 + 0.03 * $level; MaxExtra = 2 } }
    }
}

function Test-OnRay([int]$hr, [int]$hc, [string]$dir, [int]$nr, [int]$nc) {
    switch ($dir) {
        'UP' { return ($nc -eq $hc -and $nr -lt $hr) }
        'DOWN' { return ($nc -eq $hc -and $nr -gt $hr) }
        'LEFT' { return ($nr -eq $hr -and $nc -lt $hc) }
        'RIGHT' { return ($nr -eq $hr -and $nc -gt $hc) }
    }
    return $false
}

function New-LevelObject([int]$pack, [int]$level) {
    $shape = Get-Shape $pack $level
    $n = $shape.N
    $policy = Get-BendPolicy $pack $level

    $remaining = New-Object System.Collections.Generic.HashSet[string]
    foreach ($cell in $shape.Cells) { [void]$remaining.Add("$($cell.R),$($cell.C)") }

    $rng = New-Object System.Random($SeedBase + ($pack * 1000) + $level)
    $tiles = New-Object System.Collections.Generic.List[object]
    $guard = 0

    while ($remaining.Count -gt 0) {
        if ($guard++ -gt 10000) { throw "Generator stuck at pack $pack level $level" }

        # Candidate heads: a clear line of sight in some direction.
        $candidates = New-Object System.Collections.Generic.List[object]
        foreach ($key in $remaining) {
            $parts = $key.Split(',')
            $r = [int]$parts[0]
            $c = [int]$parts[1]
            foreach ($d in $dirNames) {
                $v = $dirVec[$d]
                $rr = $r + $v[0]
                $cc = $c + $v[1]
                $clear = $true
                while ($rr -ge 0 -and $rr -lt $n -and $cc -ge 0 -and $cc -lt $n) {
                    if ($remaining.Contains("$rr,$cc")) { $clear = $false; break }
                    $rr += $v[0]
                    $cc += $v[1]
                }
                if ($clear) { $candidates.Add(@{ R = $r; C = $c; D = $d }) }
            }
        }

        $head = $candidates[$rng.Next($candidates.Count)]

        # Optionally grow a body backwards from the head (into the direction it
        # came from), avoiding the head's line of sight. The list is head-first.
        $body = New-Object System.Collections.Generic.List[object]
        $body.Add(@{ R = $head.R; C = $head.C })
        if ($policy.MaxExtra -gt 0 -and $rng.NextDouble() -lt $policy.Chance) {
            $current = @{ R = $head.R; C = $head.C }
            $extra = 1 + $rng.Next($policy.MaxExtra)
            for ($s = 0; $s -lt $extra; $s++) {
                $opts = New-Object System.Collections.Generic.List[object]
                foreach ($d in $dirNames) {
                    $v = $dirVec[$d]
                    $nr = $current.R + $v[0]
                    $nc = $current.C + $v[1]
                    $key = "$nr,$nc"
                    if ($nr -lt 0 -or $nr -ge $n -or $nc -lt 0 -or $nc -ge $n) { continue }
                    if (-not $remaining.Contains($key)) { continue }
                    if (Test-OnRay $head.R $head.C $head.D $nr $nc) { continue }
                    $dup = $false
                    foreach ($b in $body) { if ($b.R -eq $nr -and $b.C -eq $nc) { $dup = $true; break } }
                    if ($dup) { continue }
                    $opts.Add(@{ R = $nr; C = $nc })
                }
                if ($opts.Count -eq 0) { break }
                $current = $opts[$rng.Next($opts.Count)]
                $body.Add($current)
            }
        }

        # cells run tail..head, so reverse the head-first body.
        $cells = New-Object System.Collections.Generic.List[object]
        for ($i = $body.Count - 1; $i -ge 0; $i--) { $cells.Add($body[$i]) }

        $tiles.Add(@{ Cells = $cells; D = $head.D; HR = $head.R; HC = $head.C })
        foreach ($cell in $cells) { [void]$remaining.Remove("$($cell.R),$($cell.C)") }
    }

    $id = 'pack-0{0}-level-{1}' -f $pack, $level.ToString('00')
    return @{
        id       = $id
        pack     = $pack
        order    = $level
        width    = $n
        height   = $n
        tiles    = $tiles
        parMoves = $tiles.Count
    }
}

function Convert-TileJson($tile) {
    if ($tile.Cells.Count -le 1) {
        return '{{"row":{0},"column":{1},"direction":"{2}"}}' -f $tile.HR, $tile.HC, $tile.D
    }
    $cellJson = ($tile.Cells | ForEach-Object { '{{"row":{0},"column":{1}}}' -f $_.R, $_.C }) -join ','
    return '{{"row":{0},"column":{1},"direction":"{2}","cells":[{3}]}}' -f $tile.HR, $tile.HC, $tile.D, $cellJson
}

$encoding = New-Object System.Text.UTF8Encoding($false)
$written = 0

foreach ($pack in 1..5) {
    foreach ($level in 1..10) {
        $obj = New-LevelObject -pack $pack -level $level
        $tileJson = ($obj.tiles | ForEach-Object { Convert-TileJson $_ }) -join ','
        $json = '{{"id":"{0}","pack":{1},"order":{2},"width":{3},"height":{4},"tiles":[{5}],"parMoves":{6}}}' -f `
            $obj.id, $obj.pack, $obj.order, $obj.width, $obj.height, $tileJson, $obj.parMoves
        $path = Join-Path $levelsDir ("$($obj.id).json")
        [System.IO.File]::WriteAllText($path, $json, $encoding)

        $bent = ($obj.tiles | Where-Object { $_.Cells.Count -gt 1 }).Count
        Write-Host ("{0}  {1}x{2}  {3} arrows ({4} bent)" -f $obj.id, $obj.width, $obj.height, $obj.parMoves, $bent)
        $written++
    }
}

Write-Host "Generated $written levels in $levelsDir"
