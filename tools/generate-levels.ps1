# Generates the 50 shipped levels: 5 packs x 10 levels.
# Each pack is a different basic shape and grows more complex with level order:
#   pack 1 = Square, pack 2 = Diamond, pack 3 = Cross, pack 4 = Ring, pack 5 = Star.
# Arrows are multi-cell bodies that walk the board — straight bars, L, U,
# zigzag and winding paths — so the result reads as a dense neon maze.
# Boards are portrait, wider than they are tall by roughly the same ratio as the
# gameplay play area (the HUD eats the top of the screen and the hint row the
# bottom, so the space left for the maze is taller than it is wide). A square
# board there only used about 60% of the plate; these sizes fill it. They also
# run from 4x7 up to 7x11, so the biggest board is 77 cells against the 64 of
# the old 8x8 while its cells are still larger on screen.
# Directions are assigned by simulating a valid removal order
# outside-in, so every generated level is solvable under the game's greedy rule.
[CmdletBinding()]
param(
    [int]$SeedBase = 20260930,
    # Seeds tried per level before the best clearance found is kept. A level
    # that still falls short is reported, so the shipped assets can be checked.
    [int]$MaxAttempts = 60,
    # Regenerate a subset while iterating on one pack.
    [int[]]$Packs = @(1, 2, 3, 4, 5),
    [int[]]$Levels = @(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$levelsDir = Join-Path $root 'app\src\main\assets\levels'
New-Item -ItemType Directory -Force -Path $levelsDir | Out-Null

$dirVec = @{ UP = @(-1, 0); DOWN = @(1, 0); LEFT = @(0, -1); RIGHT = @(0, 1) }
$dirNames = @('UP', 'DOWN', 'LEFT', 'RIGHT')
$dirOpposite = @{ UP = 'DOWN'; DOWN = 'UP'; LEFT = 'RIGHT'; RIGHT = 'LEFT' }

# Board size per pack and level, as columns x rows.
function Get-Size([int]$pack, [int]$level) {
    $columns = 7
    $rows = 11
    switch ($pack) {
        1 {
            if ($level -le 2) { $columns = 3; $rows = 5 }
            elseif ($level -le 4) { $columns = 4; $rows = 6 }
            else { $columns = 4; $rows = 7 }
        }
        2 {
            if ($level -le 3) { $columns = 4; $rows = 7 } else { $columns = 5; $rows = 8 }
        }
        3 {
            if ($level -le 4) { $columns = 5; $rows = 8 } else { $columns = 5; $rows = 9 }
        }
        4 {
            if ($level -le 3) { $columns = 5; $rows = 9 } else { $columns = 6; $rows = 10 }
        }
        default { $columns = 7; $rows = 11 }
    }
    return @{ Width = $columns; Height = $rows }
}

function Get-Shape([int]$pack, [int]$level) {
    $size = Get-Size $pack $level
    $w = $size.Width
    $h = $size.Height

    $cr = [int][math]::Floor(($h - 1) / 2)
    $cc = [int][math]::Floor(($w - 1) / 2)
    # Half-extents, used to normalise distance so one silhouette rule stretches
    # to fill a portrait board instead of only a square one.
    $hh = [double][math]::Max(1, [math]::Floor($h / 2))
    $hw = [double][math]::Max(1, [math]::Floor($w / 2))
    # One and two cells out from the centre row or column, in those same units.
    $oneRow = 1.0 / $hh
    $twoRow = 2.0 / $hh
    $oneCol = 1.0 / $hw
    $twoCol = 2.0 / $hw
    # Rounded rather than true diamonds: a hard Manhattan diamond on a tall
    # board reaches only its centre rows and leaves the rest of the plate bare.
    $exponent = 1.6
    # Thin silhouettes gain a wider radius once the level is hard enough, so
    # there is room for multi-cell bodies to walk instead of single arrows.
    $thick = switch ($pack) {
        2 { $level -ge 6 }
        4 { $level -ge 4 }
        default { $level -gt 5 }
    }
    $radius = 1.15 + $(if ($thick) { 0.35 } else { 0.0 })
    $cells = New-Object System.Collections.Generic.List[object]

    for ($r = 0; $r -lt $h; $r++) {
        for ($c = 0; $c -lt $w; $c++) {
            $ny = [math]::Abs(($r - $cr) / $hh)
            $nx = [math]::Abs(($c - $cc) / $hw)
            $round = [math]::Pow($ny, $exponent) + [math]::Pow($nx, $exponent)
            $edge = ($r -eq 0 -or $r -eq ($h - 1) -or $c -eq 0 -or $c -eq ($w - 1))
            $inEdge = ($edge -or ($thick -and ($r -eq 1 -or $r -eq ($h - 2) -or $c -eq 1 -or $c -eq ($w - 2))))
            $in = $false
            switch ($pack) {
                1 { $in = $true }
                2 { $in = ($round -le $radius) }
                3 { $in = (($ny -le $oneRow) -or ($nx -le $oneCol)) }
                4 { $in = $inEdge }
                default { $in = ($round -le $radius) -or ($ny -le $twoRow) -or ($nx -le $twoCol) }
            }
            if ($pack -eq 1 -and $level -gt 6 -and $r -eq $cr -and $c -eq $cc) { $in = $false }
            if ($in) { $cells.Add(@{ R = $r; C = $c }) }
        }
    }

    return @{ Width = $w; Height = $h; Cells = $cells }
}

# How likely an arrow grows a body at all, how long that body may get, and how
# often the body turns instead of running straight. Turning is what produces L,
# U, zigzag and winding arrows rather than plain bars. Pack 1 starts flat so the
# tutorial level stays a simple grid, then ramps up with the rest.
function Get-BendPolicy([int]$pack, [int]$level) {
    switch ($pack) {
        1 {
            if ($level -le 4) { return @{ GrowChance = 0.0; MaxExtra = 2; TurnChance = 0.6 } }
            if ($level -le 7) { return @{ GrowChance = 0.25; MaxExtra = 2; TurnChance = 0.65 } }
            return @{ GrowChance = 0.45; MaxExtra = 3; TurnChance = 0.7 }
        }
        2 { return @{ GrowChance = 0.40 + 0.025 * $level; MaxExtra = 3; TurnChance = 0.65 } }
        3 { return @{ GrowChance = 0.60 + 0.02 * $level; MaxExtra = 4; TurnChance = 0.7 } }
        4 { return @{ GrowChance = 0.55 + 0.03 * $level; MaxExtra = 5; TurnChance = 0.72 } }
        default { return @{ GrowChance = 0.80 + 0.015 * $level; MaxExtra = 5; TurnChance = 0.75 } }
    }
}

# --- Rendered-arrow clearance -------------------------------------------------
# The renderer draws a tail stub half a cell long behind an arrow's tail, so two
# arrows in neighbouring cells that point away from each other would have their
# stubs cross and their tubes touch. These helpers measure the rendered
# centre-line of an arrow so the generator can keep neighbours apart.
$script:StubReach = 0.5
$script:HeadOverlap = 0.025
$script:HeadLength = 0.28
$script:HeadWidth = 0.20
$script:MinClearance = 0.20

function New-Point([double]$x, [double]$y) {
    return [pscustomobject]@{ X = $x; Y = $y }
}

function Get-Footprint($bodyCells, [string]$dir) {
    # $bodyCells is the head-first body list; returns the rendered centre-line.
    $vec = $dirVec[$dir]
    $fx = [double]$vec[0]
    $fy = [double]$vec[1]
    $head = $bodyCells[$bodyCells.Count - 1]
    $baseX = [double]$head.R - ($fx * $script:HeadOverlap)
    $baseY = [double]$head.C - ($fy * $script:HeadOverlap)

    $points = New-Object System.Collections.Generic.List[object]
    if ($bodyCells.Count -eq 1) {
        $points.Add((New-Point ($baseX - ($fx * $script:StubReach)) ($baseY - ($fy * $script:StubReach))))
    } else {
        $tail = $bodyCells[0]
        $next = $bodyCells[1]
        $dx = [double]$tail.R - [double]$next.R
        $dy = [double]$tail.C - [double]$next.C
        $len = [Math]::Sqrt(($dx * $dx) + ($dy * $dy))
        if ($len -lt 0.01) { $len = 1.0 }
        # Precompute the scalars: PowerShell binds the comma before the operators
        # inside an argument list.
        $sx = [double]$tail.R + ($dx / $len * $script:StubReach)
        $sy = [double]$tail.C + ($dy / $len * $script:StubReach)
        $points.Add((New-Point $sx $sy))
        for ($i = 0; $i -lt ($bodyCells.Count - 1); $i++) {
            $points.Add((New-Point ([double]$bodyCells[$i].R) ([double]$bodyCells[$i].C)))
        }
    }
    $points.Add((New-Point $baseX $baseY))

    $perpX = -$fy
    $perpY = $fx
    $wingOffset = $script:HeadLength * 0.32
    $wingHalf = $script:HeadWidth * 0.5
    $points.Add((New-Point ($baseX + ($fx * $script:HeadLength)) ($baseY + ($fy * $script:HeadLength))))
    $points.Add((New-Point ($baseX + ($fx * $wingOffset) + ($perpX * $wingHalf)) ($baseY + ($fy * $wingOffset) + ($perpY * $wingHalf))))
    $points.Add((New-Point ($baseX + ($fx * $wingOffset) - ($perpX * $wingHalf)) ($baseY + ($fy * $wingOffset) - ($perpY * $wingHalf))))

    # Carry the bounding box with the polyline. Most arrows are nowhere near most
    # others, and the placement loop tests every candidate against every arrow
    # already placed, so rejecting on boxes first is what keeps generation to
    # minutes rather than tens of minutes.
    $minX = [double]::MaxValue
    $minY = [double]::MaxValue
    $maxX = [double]::MinValue
    $maxY = [double]::MinValue
    foreach ($p in $points) {
        if ($p.X -lt $minX) { $minX = $p.X }
        if ($p.X -gt $maxX) { $maxX = $p.X }
        if ($p.Y -lt $minY) { $minY = $p.Y }
        if ($p.Y -gt $maxY) { $maxY = $p.Y }
    }
    return [pscustomobject]@{ Fp = $points; MinX = $minX; MinY = $minY; MaxX = $maxX; MaxY = $maxY }
}

function Get-PointSegmentDistance($p, $a, $b) {
    $dx = $b.X - $a.X
    $dy = $b.Y - $a.Y
    $lenSq = ($dx * $dx) + ($dy * $dy)
    if ($lenSq -lt 1e-9) {
        $ex = $p.X - $a.X
        $ey = $p.Y - $a.Y
        return [Math]::Sqrt(($ex * $ex) + ($ey * $ey))
    }
    $t = ((($p.X - $a.X) * $dx) + (($p.Y - $a.Y) * $dy)) / $lenSq
    if ($t -lt 0) { $t = 0 } elseif ($t -gt 1) { $t = 1 }
    $cx = $a.X + ($dx * $t)
    $cy = $a.Y + ($dy * $t)
    $ex = $p.X - $cx
    $ey = $p.Y - $cy
    return [Math]::Sqrt(($ex * $ex) + ($ey * $ey))
}

function Get-FootprintDistance($a, $b) {
    # Segment to segment: vertices of each polyline against the other's segments.
    $best = [double]::MaxValue
    for ($i = 0; $i -lt $a.Count; $i++) {
        for ($j = 0; $j -lt ($b.Count - 1); $j++) {
            $d = Get-PointSegmentDistance $a[$i] $b[$j] $b[$j + 1]
            if ($d -lt $best) { $best = $d }
        }
    }
    for ($j = 0; $j -lt $b.Count; $j++) {
        for ($i = 0; $i -lt ($a.Count - 1); $i++) {
            $d = Get-PointSegmentDistance $b[$j] $a[$i] $a[$i + 1]
            if ($d -lt $best) { $best = $d }
        }
    }
    return $best
}

function Test-Clearance($footprint, $placed) {
    # Bounding boxes first, exact distance only for arrows that are actually
    # near enough to matter, and stop at the first one that fails.
    foreach ($other in $placed) {
        if ($other.MaxX + $script:MinClearance -lt $footprint.MinX -or
            $other.MinX -gt $footprint.MaxX + $script:MinClearance -or
            $other.MaxY + $script:MinClearance -lt $footprint.MinY -or
            $other.MinY -gt $footprint.MaxY + $script:MinClearance) {
            continue
        }
        if ((Get-FootprintDistance $footprint.Fp $other.Fp) -lt $script:MinClearance) { return $false }
    }
    return $true
}

function Get-Clearance($footprint, $placed) {
    $best = [double]::MaxValue
    foreach ($other in $placed) {
        $d = Get-FootprintDistance $footprint.Fp $other.Fp
        if ($d -lt $best) { $best = $d }
    }
    return $best
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

# The tightest approach, in cells, between the rendered centre-lines of any two
# arrows in $placed. Mirrors LevelAssetValidatorTest, so a level this reports as
# clear is one the shipped validator will accept.
function Get-TightestClearance($placed) {
    $best = [double]::MaxValue
    for ($i = 0; $i -lt $placed.Count; $i++) {
        for ($j = $i + 1; $j -lt $placed.Count; $j++) {
            $d = Get-FootprintDistance $placed[$i].Fp $placed[$j].Fp
            if ($d -lt $best) { $best = $d }
        }
    }
    return $best
}

function New-LevelObject([int]$pack, [int]$level, [int]$attempt = 0) {
    $shape = Get-Shape $pack $level
    $w = $shape.Width
    $h = $shape.Height
    $policy = Get-BendPolicy $pack $level

    $remaining = New-Object System.Collections.Generic.HashSet[string]
    foreach ($cell in $shape.Cells) { [void]$remaining.Add("$($cell.R),$($cell.C)") }

    $rng = New-Object System.Random($SeedBase + ($pack * 1000) + $level + ($attempt * 7919))
    $tiles = New-Object System.Collections.Generic.List[object]
    $placed = New-Object System.Collections.Generic.List[object]
    $longest = 1
    $guard = 0
    $tight = 0

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
                while ($rr -ge 0 -and $rr -lt $h -and $cc -ge 0 -and $cc -lt $w) {
                    if ($remaining.Contains("$rr,$cc")) { $clear = $false; break }
                    $rr += $v[0]
                    $cc += $v[1]
                }
                if ($clear) { $candidates.Add(@{ R = $r; C = $c; D = $d }) }
            }
        }

        # Prefer a head whose drawn stub keeps clear of the arrows already placed.
        # Only the second and later arrows in a pair are checked, which is enough:
        # every pair is compared once, when its later arrow is placed.
        $safe = New-Object System.Collections.Generic.List[object]
        foreach ($candidate in $candidates) {
            $single = New-Object System.Collections.Generic.List[object]
            $single.Add(@{ R = $candidate.R; C = $candidate.C })
            if (Test-Clearance (Get-Footprint $single $candidate.D) $placed) {
                $safe.Add($candidate)
            }
        }
        if ($safe.Count -gt 0) {
            $head = $safe[$rng.Next($safe.Count)]
        } else {
            # Nothing leaves room: take the roomiest head available and let the
            # seed sweep at the end of the script look for a better layout.
            $tight++
            $bestClearance = -1.0
            foreach ($candidate in $candidates) {
                $single = New-Object System.Collections.Generic.List[object]
                $single.Add(@{ R = $candidate.R; C = $candidate.C })
                $gap = Get-Clearance (Get-Footprint $single $candidate.D) $placed
                if ($gap -gt $bestClearance) { $bestClearance = $gap; $head = $candidate }
            }
        }

        # Optionally grow a multi-cell body backwards from the head. The list is
        # head-first. The cell directly behind the head is the only legal start:
        # it is the one that keeps the exit direction equal to the direction of
        # the final body segment, which the shared validator enforces.
        $body = New-Object System.Collections.Generic.List[object]
        $body.Add(@{ R = $head.R; C = $head.C })
        if ($rng.NextDouble() -lt $policy.GrowChance) {
            $v = $dirVec[$head.D]
            $br = $head.R - $v[0]
            $bc = $head.C - $v[1]
            if ($br -ge 0 -and $br -lt $h -and $bc -ge 0 -and $bc -lt $w -and $remaining.Contains("$br,$bc")) {
                $target = 2 + $rng.Next($policy.MaxExtra)
                $body.Add(@{ R = $br; C = $bc })
                $current = $body[$body.Count - 1]
                $straight = $dirOpposite[$head.D]
                while ($body.Count -lt $target) {
                    $opts = New-Object System.Collections.Generic.List[object]
                    foreach ($d in $dirNames) {
                        $vv = $dirVec[$d]
                        $nr = $current.R + $vv[0]
                        $nc = $current.C + $vv[1]
                        if ($nr -lt 0 -or $nr -ge $h -or $nc -lt 0 -or $nc -ge $w) { continue }
                        $key = "$nr,$nc"
                        if (-not $remaining.Contains($key)) { continue }
                        # Never occupy the arrow's own exit ray: those cells must
                        # stay clear for the arrow to leave.
                        if (Test-OnRay $head.R $head.C $head.D $nr $nc) { continue }
                        $dup = $false
                        foreach ($b in $body) { if ($b.R -eq $nr -and $b.C -eq $nc) { $dup = $true; break } }
                        if ($dup) { continue }
                        $opts.Add(@{ R = $nr; C = $nc; D = $d })
                    }
                    if ($opts.Count -eq 0) { break }
                    $turn = New-Object System.Collections.Generic.List[object]
                    foreach ($o in $opts) { if ($o.D -ne $straight) { $turn.Add($o) } }
                    # A statement (not an if-expression) assigns the list itself:
                    # returning a collection from an if-expression enumerates it.
                    $pool = $opts
                    if ($turn.Count -gt 0 -and $rng.NextDouble() -lt $policy.TurnChance) { $pool = $turn }
                    $pick = $pool[$rng.Next($pool.Count)]
                    $body.Add(@{ R = $pick.R; C = $pick.C })
                    $current = $pick
                    $straight = $pick.D
                }
            }
        }
        if ($body.Count -gt 1 -and -not (Test-Clearance (Get-Footprint $body $head.D) $placed)) {
            # A long body moved the tail stub too close to a neighbour: fall back
            # to the single-cell arrow, which the head choice already cleared.
            $body = New-Object System.Collections.Generic.List[object]
            $body.Add(@{ R = $head.R; C = $head.C })
            $tight++
        }
        if ($body.Count -gt $longest) { $longest = $body.Count }
        # Cheap invariant: every body entry must be a real cell, and the tail must
        # be contiguous with the head. Anything else would ship a broken asset.
        for ($b = 0; $b -lt $body.Count; $b++) {
            $cell = $body[$b]
            if ($cell -isnot [System.Collections.IDictionary] -or $null -eq $cell.R -or $null -eq $cell.C) {
                throw "Malformed body cell at pack $pack level $level (index $b)"
            }
            if ($b -gt 0) {
                $prev = $body[$b - 1]
                $gap = [math]::Abs($prev.R - $cell.R) + [math]::Abs($prev.C - $cell.C)
                if ($gap -ne 1) { throw "Non-contiguous body at pack $pack level $level (index $b)" }
            }
        }

        # cells run tail..head, so reverse the head-first body.
        $cells = New-Object System.Collections.Generic.List[object]
        for ($i = $body.Count - 1; $i -ge 0; $i--) { $cells.Add($body[$i]) }

        $tiles.Add(@{ Cells = $cells; D = $head.D; HR = $head.R; HC = $head.C })
        foreach ($cell in $cells) { [void]$remaining.Remove("$($cell.R),$($cell.C)") }
        # Remember the drawn footprint so later arrows can keep away from it.
        # Get-Footprint reads the head off the END of the list, so it gets the
        # tail-first $cells, not the head-first $body it was grown from.
        $placed.Add((Get-Footprint $cells $head.D))
    }

    $id = 'pack-0{0}-level-{1}' -f $pack, $level.ToString('00')
    return @{
        id        = $id
        pack      = $pack
        order     = $level
        width     = $w
        height    = $h
        tiles     = $tiles
        parMoves  = $tiles.Count
        longest   = $longest
        tight     = $tight
        clearance = (Get-TightestClearance $placed)
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
$unclear = New-Object System.Collections.Generic.List[string]

foreach ($packNumber in $Packs) {
    foreach ($level in $Levels) {
        # A dense board can leave a placement with nowhere clear to point, which
        # the generator covers by taking the tightest head available and can push
        # two rendered arrows together. Reshuffling the seed is cheap, so sweep a
        # few of them and keep the roomiest layout the level can be given.
        $best = $null
        for ($attempt = 0; $attempt -lt $MaxAttempts; $attempt++) {
            $candidate = New-LevelObject -pack $packNumber -level $level -attempt $attempt
            if ($null -eq $best -or $candidate.clearance -gt $best.clearance) { $best = $candidate }
            if ($candidate.clearance -ge $script:MinClearance) { break }
        }

        $obj = $best
        $tileJson = ($obj.tiles | ForEach-Object { Convert-TileJson $_ }) -join ','
        $json = '{{"id":"{0}","pack":{1},"order":{2},"width":{3},"height":{4},"tiles":[{5}],"parMoves":{6}}}' -f `
            $obj.id, $obj.pack, $obj.order, $obj.width, $obj.height, $tileJson, $obj.parMoves
        $path = Join-Path $levelsDir ("$($obj.id).json")
        [System.IO.File]::WriteAllText($path, $json, $encoding)

        $bent = ($obj.tiles | Where-Object { $_.Cells.Count -gt 1 }).Count
        $clear = $obj.clearance -ge $script:MinClearance
        if (-not $clear) { [void]$unclear.Add("$($obj.id) at $([Math]::Round($obj.clearance, 3))") }
        Write-Host ("{0}  {1}x{2}  {3,3} arrows ({4} multi-cell, longest {5})  gap {6}  {7}" -f `
                $obj.id, $obj.width, $obj.height, $obj.parMoves, $bent, $obj.longest, `
                ([Math]::Round($obj.clearance, 3)), $(if ($clear) { 'ok' } else { 'TIGHT' }))
        $written++
    }
}

Write-Host "Generated $written levels in $levelsDir"
if ($unclear.Count -gt 0) {
    Write-Warning "These levels could not reach the $script:MinClearance cell clearance in $MaxAttempts seeds: $($unclear -join ', ')"
}
