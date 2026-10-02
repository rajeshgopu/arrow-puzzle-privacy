# Generates the 50 shipped levels: 5 packs x 10 levels.
#
# Difficulty is a ramp, not a dice roll. $script:Ramp is the whole curve in one
# table - board size, arrow count and target board occupancy per level - and
# every other knob (body length, bend chance, dependency pressure, silhouette
# thickness) is derived from it, so a level cannot drift off the curve by
# accident. Early levels are small boards, few arrows and generous spacing;
# later levels grow the board, add arrows, close the gaps and bury the arrows
# under each other. Level 50 is not level 1 with extra arrows: it is 54 arrows
# on 112 cells with a long forced clearing order and about two obvious first
# moves.
#
# Boards stay portrait, wider than they are tall by roughly the same ratio as the
# gameplay play area (the HUD eats the top of the screen and the hint row the
# bottom), so the grid fills the plate instead of letterboxing inside it. Each
# pack keeps its silhouette identity (1 Square, 2 Diamond, 3 Cross, 4 Ring,
# 5 Star), but the silhouette is grown until it covers the level's target
# occupancy plus a margin, so the shaped packs keep their outline early and
# close up as the levels get hard. A hard board never leaves a large unused
# region in the middle of it.
#
# Arrows are placed in the reverse of a valid removal order, so every level is
# solvable under the greedy rule the engine uses, by construction rather than by
# search. A newly placed arrow only has to keep its exit ray clear of the arrows
# placed before it, because the arrows placed after it are removed first and are
# gone by the time this one leaves.
#
# Difficulty comes from *how* each arrow is chosen, not from the arrow count
# alone:
#
#   ChainChance - the chance of deliberately parking the new arrow's head on the
#                 previous arrow's exit ray. That makes the previous arrow wait
#                 for this one, which is what builds a dependency chain.
#   CrossWeight - how strongly a growing body is pulled towards the exit rays of
#                 earlier arrows, which stacks extra layers of blocking on top
#                 of the chain and is what produces crossings.
#   TurnChance  - how often a body turns instead of running straight, which is
#                 what produces L, U, zigzag and winding arrows.
#   LongChance  - the chance of letting one arrow run well past the length the
#                 board's occupancy allows, so a dense board still gets long
#                 arrows weaving through it.
#
# So the measure that matters is how few arrows are available at any one moment
# and how long the order before them is. Every level is measured (occupancy,
# dependency depth, blocking layers, open moves, crossings, orientations) and
# greedily re-solved before it is written; the seed sweep keeps the layout that
# comes closest to that level's targets.
[CmdletBinding()]
param(
    [int]$SeedBase = 20260930,
    # Seeds tried per level before the layout closest to the level's targets is
    # kept. Generation is chain-biased, so most seeds are already close.
    [int]$MaxAttempts = 16,
    # Regenerate a subset while iterating on one pack.
    [int[]]$Packs = @(1, 2, 3, 4, 5),
    [int[]]$Levels = @(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$levelsDir = Join-Path $root 'app\src\main\assets\levels'
New-Item -ItemType Directory -Force -Path $levelsDir | Out-Null

# Direction table, indexed 0..3. Row grows downward, column rightward, so UP is
# (-1, 0) and RIGHT is (0, 1) - the same convention as PuzzleEngine.
$script:Dr = @(-1, 0, 1, 0)
$script:Dc = @(0, 1, 0, -1)
$script:DirNames = @('UP', 'RIGHT', 'DOWN', 'LEFT')
$script:DirOpposite = @(2, 3, 0, 1)

# The difficulty curve. One row per level in play order: columns, rows, arrow
# count, target board occupancy (occupied cells / board cells).
#
# Arrow count rises every level and is monotone across the whole game. Occupancy
# rises inside a pack and steps back slightly where a pack opens on a bigger
# board, which is also where the average body steps up. Arrow count bands:
# pack 1 = 10-18, packs 2-3 = 19-38, packs 4-5 = 39-50. The last pack stops at
# 50 rather than climbing further because past that the arrow count alone starves
# the body length, and a board of fifty one-cell arrows is easier to read than a
# board of fifty mixed short and long ones.
$script:Ramp = @(
    # columns, rows, arrow count, target board occupancy
    @(4, 6, 10, 0.42), @(4, 6, 11, 0.50), @(4, 7, 12, 0.46), @(4, 7, 13, 0.54), @(4, 7, 14, 0.61),
    @(5, 8, 15, 0.50), @(5, 8, 16, 0.55), @(5, 8, 17, 0.58), @(5, 8, 17, 0.60), @(5, 8, 18, 0.63),
    @(5, 9, 19, 0.62), @(5, 9, 20, 0.67), @(6, 10, 21, 0.63), @(6, 10, 22, 0.68), @(6, 10, 23, 0.72),
    @(6, 10, 24, 0.77), @(6, 11, 25, 0.78), @(6, 11, 26, 0.82), @(6, 11, 27, 0.85), @(6, 11, 28, 0.88),
    @(7, 12, 29, 0.80), @(7, 12, 30, 0.82), @(7, 12, 31, 0.85), @(7, 12, 32, 0.88), @(7, 12, 33, 0.90),
    @(7, 13, 34, 0.89), @(7, 13, 35, 0.90), @(7, 13, 36, 0.92), @(7, 13, 37, 0.92), @(7, 13, 38, 0.93),
    @(8, 14, 38, 0.85), @(8, 14, 39, 0.86), @(8, 14, 40, 0.87), @(8, 14, 41, 0.88), @(8, 14, 42, 0.89),
    @(8, 14, 43, 0.90), @(8, 14, 44, 0.90), @(8, 14, 45, 0.91), @(8, 14, 46, 0.92), @(8, 14, 46, 0.92),
    @(8, 14, 47, 0.88), @(8, 14, 47, 0.88), @(8, 14, 48, 0.89), @(8, 14, 49, 0.89), @(8, 14, 49, 0.89),
    @(8, 14, 50, 0.90), @(8, 14, 50, 0.90), @(8, 14, 50, 0.91), @(8, 14, 50, 0.91), @(8, 14, 50, 0.91)
)
if ($script:Ramp.Count -ne 50) { throw "Ramp has $($script:Ramp.Count) rows, expected 50" }

function Clamp([double]$value, [double]$low, [double]$high) {
    if ($value -lt $low) { return $low }
    if ($value -gt $high) { return $high }
    return $value
}

# Everything the generator needs for one level, derived from $script:Ramp.
function Get-LevelPlan([int]$pack, [int]$order) {
    $step = ($pack - 1) * 10 + $order
    $row = $script:Ramp[$step - 1]
    $columns = $row[0]
    $rows = $row[1]
    $arrows = $row[2]
    $density = [double]$row[3]
    $cells = $columns * $rows

    # The shape has to hold every arrow at least once over, so it is grown past
    # the target occupancy. Early levels keep a wide margin and read as a shape;
    # hard levels close up until there is no wasted region left.
    $shapeFill = Clamp ($density + 0.12) 0.70 0.995

    # Target average body length. Arrows x average = occupied cells, so the
    # density target and the arrow count together fix how long the bodies run.
    $avgBody = ($cells * $density) / $arrows
    # Longest body a grown arrow may reach, raised until the expected length
    # works, then raised again for variety so hard levels still spread their
    # cells over long arrows instead of pure single cells.
    $maxBody = [int][math]::Ceiling((2.0 * ($avgBody - 1.0)) / 0.9)
    $byVariety = [int][math]::Ceiling($avgBody) + 2
    if ($byVariety -gt $maxBody) { $maxBody = $byVariety }
    $maxBody = [int](Clamp $maxBody 2 7)

    # Chance a body grows at all. A grown arrow is uniformly 2..MaxBody cells
    # long, so the expected length is 1 + GrowChance * MaxBody/2. Growth also
    # fails for real reasons - the cell behind the head can already be taken - so
    # the placement loop adds a pressure term each round to make up the shortfall
    # against the level's occupancy target instead of hoping the average works
    # out.
    $growChance = Clamp (($avgBody - 1.0) / ($maxBody / 2.0)) 0.10 0.92

    # How hard this level leans on ordering rather than on counting arrows. This
    # is the single strongest difficulty lever: the expected run of consecutive
    # forced links is about 1 / (1 - ChainChance), so the top of the game is a
    # very long forced order rather than a long list.
    $t = ($step - 1) / 49.0
    $chainChance = 0.45 + 0.54 * $t
    $crossWeight = 1.0 + 5.0 * $t
    $turnChance = 0.30 + 0.55 * $t
    $longChance = 0.05 + 0.12 * $t
    $longExtra = 2 + [int][math]::Round(2 * $t)

    # Targets the seed sweep scores against. Open moves are how many arrows are
    # available with nothing else removed: a handful early, about two at the top
    # so the first tap is always obvious. Depth is the longest chain of arrows
    # that have to be cleared before the last one can move.
    $targetOpen = [int][math]::Max(2, [math]::Round(7 - 5 * $t))
    if ($targetOpen -gt $arrows) { $targetOpen = $arrows }
    $targetDepth = [int][math]::Round($arrows * (0.20 + 0.45 * $t))
    $targetMulti = [int][math]::Max(1, [math]::Round($arrows * (0.05 + 0.35 * $t)))
    $targetBent = [int][math]::Max(1, [math]::Round($arrows * (0.05 + 0.40 * $t)))

    return @{
        Pack        = $pack
        Order       = $order
        Step        = $step
        Columns     = $columns
        Rows        = $rows
        Cells       = $cells
        Arrows      = $arrows
        Density     = $density
        ShapeFill   = $shapeFill
        MaxBody     = $maxBody
        GrowChance  = $growChance
        TurnChance  = $turnChance
        ChainChance = $chainChance
        CrossWeight = $crossWeight
        LongChance  = $longChance
        LongExtra   = $longExtra
        TargetOpen  = $targetOpen
        TargetDepth = $targetDepth
        TargetMulti = $targetMulti
        TargetBent  = $targetBent
    }
}

# --- Rendered-arrow clearance -------------------------------------------------
# The renderer draws a tail stub half a cell long behind an arrow's tail, so two
# arrows in neighbouring cells that point away from each other would have their
# stubs cross and their tubes touch. These helpers measure the rendered
# centre-line of an arrow so the generator can keep neighbours apart. The
# constants and the maths mirror LevelAssetValidatorTest and ArrowShape, so a
# level the generator reports as clear is one the shipped validator accepts.
$script:StubReach = 0.5
$script:HeadOverlap = 0.025
$script:HeadLength = 0.28
$script:HeadWidth = 0.20
$script:MinClearance = 0.20

# Direction vector in the (row, column) form Get-Footprint expects.
$script:FootDir = @{ UP = @(-1, 0); DOWN = @(1, 0); LEFT = @(0, -1); RIGHT = @(0, 1) }

function New-Point([double]$x, [double]$y) {
    return [pscustomobject]@{ X = $x; Y = $y }
}

function Get-Footprint($bodyCells, [string]$dir) {
    # $bodyCells is the tail-first cell list; returns the rendered centre-line.
    $vec = $script:FootDir[$dir]
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
    # seconds rather than minutes.
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

# Closest approach, in cells, between a candidate footprint and everything
# already placed, or MaxValue when nothing is placed yet.
#
# Bounding boxes are tested first and the exact distance is only computed for
# arrows that are actually near enough to matter. A pair whose boxes are
# separated by more than the separation itself cannot be closer than that gap, so
# the gap is the exact lower bound and the expensive segment maths is skipped.
# Every pair of arrows is measured exactly once, when its later arrow is placed,
# so the smallest result over the whole run is the level's tightest clearance.
#
# Boxes live in four flat double arrays rather than on the footprint objects.
# The placement loop calls this once per candidate per placement, which is the
# hottest loop in the generator, and property access through a collection of
# objects is what made it slow enough to cap the seed sweep.
function Get-GapFlat($footprint, $pFp, $pMinX, $pMinY, $pMaxX, $pMaxY, $count) {
    $best = [double]::MaxValue
    for ($i = 0; $i -lt $count; $i++) {
        if ($pMaxX[$i] -lt $footprint.MinX) {
            $gap = $footprint.MinX - $pMaxX[$i]
            if ($gap -lt $best) { $best = $gap }
            continue
        }
        if ($pMinX[$i] -gt $footprint.MaxX) {
            $gap = $pMinX[$i] - $footprint.MaxX
            if ($gap -lt $best) { $best = $gap }
            continue
        }
        if ($pMaxY[$i] -lt $footprint.MinY) {
            $gap = $footprint.MinY - $pMaxY[$i]
            if ($gap -lt $best) { $best = $gap }
            continue
        }
        if ($pMinY[$i] -gt $footprint.MaxY) {
            $gap = $pMinY[$i] - $footprint.MaxY
            if ($gap -lt $best) { $best = $gap }
            continue
        }
        $d = Get-FootprintDistance $footprint.Fp $pFp[$i]
        if ($d -lt $best) { $best = $d }
    }
    return $best
}

# --- Board silhouette ---------------------------------------------------------
# One 0/1 mask per cell for a given thickness. Thickness 0 is the thinnest
# version of the pack's shape and thickness 1 covers the whole board, so the
# caller can grow a shape by turning one dial.
function Get-ShapeMask([int]$pack, [int]$columns, [int]$rows, [double]$thickness) {
    $mask = New-Object 'bool[]' ($columns * $rows)
    $cr = [int][math]::Floor(($rows - 1) / 2)
    $cc = [int][math]::Floor(($columns - 1) / 2)
    # Half-extents, used to normalise distance so one silhouette rule stretches
    # to fill a portrait board instead of only a square one.
    $hh = [double][math]::Max(1, [math]::Floor($rows / 2))
    $hw = [double][math]::Max(1, [math]::Floor($columns / 2))
    # Rounded rather than true diamonds: a hard Manhattan diamond on a tall
    # board reaches only its centre rows and leaves the rest of the plate bare.
    $exponent = 1.6

    for ($r = 0; $r -lt $rows; $r++) {
        $ny = [math]::Abs(($r - $cr) / $hh)
        for ($c = 0; $c -lt $columns; $c++) {
            $nx = [math]::Abs(($c - $cc) / $hw)
            $in = $false
            switch ($pack) {
                1 { $in = $true }
                2 {
                    $in = (([math]::Pow($ny, $exponent) + [math]::Pow($nx, $exponent)) -le (0.75 + 2.0 * $thickness))
                }
                3 {
                    $arm = 0.08 + 1.6 * $thickness
                    $in = ($ny -le $arm) -or ($nx -le $arm)
                }
                4 {
                    # A ring whose band grows until it closes over the middle.
                    $band = [int][math]::Round(6 * $thickness)
                    $depth = [int][math]::Min([math]::Min($r, $rows - 1 - $r), [math]::Min($c, $columns - 1 - $c))
                    $in = ($depth -le $band)
                }
                default {
                    $arm = 0.06 + 0.7 * $thickness
                    $in = (([math]::Pow($ny, $exponent) + [math]::Pow($nx, $exponent)) -le (0.85 + 1.95 * $thickness)) -or
                          ($ny -le $arm) -or ($nx -le $arm)
                }
            }
            $mask[($r * $columns) + $c] = $in
        }
    }
    return $mask
}

# A mask and its cell count, but only when the shape is one connected region. A
# mask with a detached island would leave a pocket of cells the reverse
# placement can never reach, which is exactly the unused region to avoid.
function Get-ConnectedShape([int]$pack, [int]$columns, [int]$rows, [double]$thickness) {
    $mask = Get-ShapeMask $pack $columns $rows $thickness
    $total = 0
    $start = -1
    for ($i = 0; $i -lt $mask.Length; $i++) {
        if ($mask[$i]) {
            $total++
            if ($start -lt 0) { $start = $i }
        }
    }
    if ($start -lt 0) { return @{ Mask = $mask; Count = 0; Connected = $false } }

    $seen = New-Object 'bool[]' ($columns * $rows)
    $queue = New-Object System.Collections.Generic.Queue[int]
    $queue.Enqueue($start)
    $seen[$start] = $true
    $reached = 0
    while ($queue.Count -gt 0) {
        $idx = $queue.Dequeue()
        $reached++
        $r = [int][math]::Floor($idx / $columns)
        $c = $idx - ($r * $columns)
        for ($d = 0; $d -lt 4; $d++) {
            $rr = $r + $script:Dr[$d]
            $cc = $c + $script:Dc[$d]
            if ($rr -lt 0 -or $rr -ge $rows -or $cc -lt 0 -or $cc -ge $columns) { continue }
            $n = ($rr * $columns) + $cc
            if (-not $mask[$n] -or $seen[$n]) { continue }
            $seen[$n] = $true
            $queue.Enqueue($n)
        }
    }
    return @{ Mask = $mask; Count = $total; Connected = ($reached -eq $total) }
}

# The board's usable region: the largest connected shape that still fits inside
# the level's target occupancy plus a margin.
function Get-Shape($plan) {
    $target = [int][math]::Floor($plan.ShapeFill * $plan.Cells)
    $low = 0.0
    $high = 1.0
    $best = Get-ConnectedShape $plan.Pack $plan.Columns $plan.Rows $low
    for ($i = 0; $i -lt 18; $i++) {
        $mid = ($low + $high) / 2
        $probe = Get-ConnectedShape $plan.Pack $plan.Columns $plan.Rows $mid
        if ($probe.Connected -and $probe.Count -le $target) {
            $low = $mid
            $best = $probe
        } else {
            $high = $mid
        }
    }
    return $best
}

# --- Placement ---------------------------------------------------------------
# Footprint of a single-cell arrow at one cell facing one direction. Built once
# per level for all four directions because the candidate scan asks for these
# hundreds of times per placement.
function New-SingleFootprints($w, $h, $rowOf, $colOf) {
    $cache = New-Object 'object[]' ($w * $h * 4)
    for ($idx = 0; $idx -lt ($w * $h); $idx++) {
        $cell = @{ R = $rowOf[$idx]; C = $colOf[$idx] }
        for ($d = 0; $d -lt 4; $d++) {
            $body = New-Object System.Collections.Generic.List[object]
            $body.Add($cell)
            $cache[($idx * 4) + $d] = Get-Footprint $body $script:DirNames[$d]
        }
    }
    return $cache
}

# Footprint of a body given head-first cell indices. Get-Footprint reads the head
# off the end of the list, so the indices are reversed into tail-first order.
function Get-BodyFootprint($headFirst, $headDir, $rowOf, $colOf) {
    $body = New-Object System.Collections.Generic.List[object]
    for ($i = $headFirst.Count - 1; $i -ge 0; $i--) {
        $idx = $headFirst[$i]
        $body.Add(@{ R = $rowOf[$idx]; C = $colOf[$idx] })
    }
    return Get-Footprint $body $script:DirNames[$headDir]
}

# Greedy re-solve of the finished level, using the same rule as PuzzleEngine:
# tap the first arrow whose route to the edge is empty, and an arrow never
# blocks itself. Every generated level is run through this before it is written.
function Test-Solvable($plan, $cells, $dirs, $heads) {
    $w = $plan.Columns
    $h = $plan.Rows
    $n = $cells.Count
    $occ = New-Object 'int[]' ($w * $h)
    for ($i = 0; $i -lt $occ.Length; $i++) { $occ[$i] = -1 }
    $alive = New-Object 'bool[]' $n
    for ($i = 0; $i -lt $n; $i++) {
        $alive[$i] = $true
        foreach ($idx in $cells[$i]) { $occ[$idx] = $i }
    }
    $left = $n
    $guard = $n + 1
    while ($left -gt 0) {
        if ($guard-- -le 0) { return $false }
        $moved = $false
        for ($i = 0; $i -lt $n; $i++) {
            if (-not $alive[$i]) { continue }
            $d = $dirs[$i]
            $r = $heads[$i].Row + $script:Dr[$d]
            $c = $heads[$i].Col + $script:Dc[$d]
            $clear = $true
            while ($r -ge 0 -and $r -lt $h -and $c -ge 0 -and $c -lt $w) {
                $o = $occ[($r * $w) + $c]
                if ($o -ne -1 -and $o -ne $i) { $clear = $false; break }
                $r += $script:Dr[$d]
                $c += $script:Dc[$d]
            }
            if ($clear) {
                $alive[$i] = $false
                foreach ($idx in $cells[$i]) { $occ[$idx] = -1 }
                $left--
                $moved = $true
                break
            }
        }
        if (-not $moved) { return $false }
    }
    return $true
}

function New-LevelObject($plan, [int]$attempt, $rowOf, $colOf, $shape, $singleFp) {
    $w = $plan.Columns
    $h = $plan.Rows
    $cellCount = $w * $h
    $mask = $shape.Mask

    $rng = New-Object System.Random($SeedBase + ($plan.Pack * 1000) + $plan.Order + ($attempt * 7919))

    # owner: cell -> placing index, or -1 when free.
    # ahead: cell*4 + direction -> how many placed cells sit on that ray. Zero
    #        is the solvability condition, so keeping it current is what turns
    #        the candidate scan into a single array read instead of a ray walk.
    # score: cell -> how many placed arrows have this cell on their exit ray.
    #        Parking a new arrow or body cell there blocks that many arrows at
    #        once, which is what turns single-depth blocking into layers.
    $owner = New-Object 'int[]' $cellCount
    $ahead = New-Object 'int[]' ($cellCount * 4)
    $score = New-Object 'int[]' $cellCount
    for ($i = 0; $i -lt $cellCount; $i++) { $owner[$i] = -1 }

    # rayLen never changes, so the number of still-empty cells on a ray is
    # rayLen minus ahead. That is what the scan needs to tell an arrow with
    # somewhere to go from one that would be an instant free move.
    $rayLen = New-Object 'int[]' ($cellCount * 4)
    for ($idx = 0; $idx -lt $cellCount; $idx++) {
        for ($d = 0; $d -lt 4; $d++) {
            $rr = $rowOf[$idx] + $script:Dr[$d]
            $cc = $colOf[$idx] + $script:Dc[$d]
            $len = 0
            while ($rr -ge 0 -and $rr -lt $h -and $cc -ge 0 -and $cc -lt $w) {
                $len++
                $rr += $script:Dr[$d]
                $cc += $script:Dc[$d]
            }
            $rayLen[($idx * 4) + $d] = $len
        }
    }

    $free = 0
    foreach ($m in $mask) { if ($m) { $free++ } }

    $pFp = New-Object 'object[]' $plan.Arrows
    $pMinX = New-Object 'double[]' $plan.Arrows
    $pMinY = New-Object 'double[]' $plan.Arrows
    $pMaxX = New-Object 'double[]' $plan.Arrows
    $pMaxY = New-Object 'double[]' $plan.Arrows
    $placedCount = 0
    $tiles = New-Object System.Collections.Generic.List[object]
    $tileDirs = New-Object System.Collections.Generic.List[int]
    $tileHeads = New-Object System.Collections.Generic.List[object]
    $tileCells = New-Object System.Collections.Generic.List[object]
    $occCells = 0
    $longest = 1
    $bent = 0
    $stalled = $false
    $tightest = [double]::MaxValue

    $link = New-Object 'bool[]' $cellCount
    # hasBlocker marks arrows some later arrow already sits in front of.
    # cellArrows lists, per cell, the arrows whose exit ray runs through it, so
    # the flags can be maintained as arrows are placed.
    $hasBlocker = New-Object 'bool[]' $plan.Arrows
    $cellArrows = New-Object 'object[]' $cellCount
    $linkHead = -1
    $linkDir = -1

    while ($tiles.Count -lt $plan.Arrows -and $free -gt 0) {
        # The arrow this one should block. Normally that is the arrow placed just
        # before it, which is what grows one long forced order. When its lane is
        # used up - its head reached the edge, or everything ahead of it filled -
        # step back. The first pass prefers an arrow that has nobody in front of
        # it yet, because covering a fresh arrow is what turns a free move into a
        # blocked one; the second pass accepts an arrow that is already blocked,
        # which still buys another layer of blocking. Linking to any earlier
        # arrow is always valid, because earlier arrows are removed later.
        for ($i = 0; $i -lt $cellCount; $i++) { $link[$i] = $false }
        $linkHead = -1
        $linkDir = -1
        for ($want = 0; $want -lt 2 -and $linkHead -lt 0; $want++) {
            for ($k = $tiles.Count - 1; $k -ge 0; $k--) {
                if ($hasBlocker[$k] -ne ([bool]$want)) { continue }
                $d = $tileDirs[$k]
                $pr = $tileHeads[$k].Row + $script:Dr[$d]
                $pc = $tileHeads[$k].Col + $script:Dc[$d]
                if ($pr -lt 0 -or $pr -ge $h -or $pc -lt 0 -or $pc -ge $w) { continue }
                if ($owner[($pr * $w) + $pc] -ne -1) { continue }
                $linkHead = $k
                $linkDir = $d
                break
            }
        }
        if ($linkHead -ge 0) {
            $pr = $tileHeads[$linkHead].Row + $script:Dr[$linkDir]
            $pc = $tileHeads[$linkHead].Col + $script:Dc[$linkDir]
            while ($pr -ge 0 -and $pr -lt $h -and $pc -ge 0 -and $pc -lt $w) {
                $link[($pr * $w) + $pc] = $true
                $pr += $script:Dr[$linkDir]
                $pc += $script:Dc[$linkDir]
            }
        }

        # Candidate heads: a free cell with a clear exit ray, whose drawn stub
        # keeps its distance from the arrows already placed.
        $candIdx = New-Object 'int[]' ($cellCount * 4)
        $candDir = New-Object 'int[]' ($cellCount * 4)
        $candWeight = New-Object 'double[]' ($cellCount * 4)
        $candFree = New-Object 'int[]' ($cellCount * 4)
        $candCount = 0
        for ($idx = 0; $idx -lt $cellCount; $idx++) {
            if ($owner[$idx] -ne -1) { continue }
            for ($d = 0; $d -lt 4; $d++) {
                $base = $idx * 4 + $d
                if ($ahead[$base] -ne 0) { continue }
                if ($placedCount -gt 0) {
                    $gap = Get-GapFlat $singleFp[$base] $pFp $pMinX $pMinY $pMaxX $pMaxY $placedCount
                    if ($gap -lt $script:MinClearance) { continue }
                }
                $weight = 1.0 + ($plan.CrossWeight * $score[$idx])
                if ($linkHead -ge 0 -and $link[$idx]) { $weight += 6.0 }
                # Room to grow: a body can only start in the cell directly behind
                # its head, so a head whose back neighbour is taken can never be
                # more than one cell long. On a dense board that is what stops the
                # board filling up, so heads with room behind them count more.
                $opposite = $script:DirOpposite[$d]
                $rr = $rowOf[$idx] + $script:Dr[$opposite]
                $cc = $colOf[$idx] + $script:Dc[$opposite]
                if ($rr -ge 0 -and $rr -lt $h -and $cc -ge 0 -and $cc -lt $w) {
                    $behind = ($rr * $w) + $cc
                    if ($owner[$behind] -eq -1) {
                        $room = 1
                        for ($k = 0; $k -lt 4; $k++) {
                            $nr = $rowOf[$behind] + $script:Dr[$k]
                            $nc = $colOf[$behind] + $script:Dc[$k]
                            if ($nr -lt 0 -or $nr -ge $h -or $nc -lt 0 -or $nc -ge $w) { continue }
                            if ($owner[(($nr * $w) + $nc)] -eq -1) { $room++ }
                        }
                        $weight += 0.9 * $room
                    }
                }
                # An arrow whose exit ray is already full can never be blocked by
                # anything, which makes it a free move for the rest of the level
                # and, worse, leaves the next arrow nothing to chain onto: the
                # lane it was going to extend stops there. A long clear ray is
                # worth a lot here, because it is what makes an arrow blockable,
                # so the length of the clear run is the strongest term on the
                # candidate and an empty ray is nearly excluded.
                $freeAhead = $rayLen[$base] - $ahead[$base]
                if ($freeAhead -gt 1) { $weight *= [math]::Min(4.0, 1.0 + (0.7 * ($freeAhead - 1))) }
                $candIdx[$candCount] = $idx
                $candDir[$candCount] = $d
                $candWeight[$candCount] = $weight
                $candFree[$candCount] = $freeAhead
                $candCount++
            }
        }
        if ($candCount -eq 0) { $stalled = $true; break }

        # Drop the arrows whose exit ray is already full, as long as anything else
        # is available. A low weight would not be enough: the weights are
        # normalised, so the moment every remaining candidate is full-rayed the
        # discount cancels itself out and the arrow still gets placed. One of
        # those is a free move the player can take at any point in the level, for
        # the rest of the level, and it is the single biggest reason a dense
        # board still feels generous.
        $anyLane = $false
        for ($i = 0; $i -lt $candCount; $i++) {
            if ($candFree[$i] -ge 1) { $anyLane = $true; break }
        }
        if ($anyLane) {
            $laneIdx = New-Object 'int[]' $candCount
            $laneDir = New-Object 'int[]' $candCount
            $laneWeight = New-Object 'double[]' $candCount
            $laneFree = New-Object 'int[]' $candCount
            $laneCount = 0
            for ($i = 0; $i -lt $candCount; $i++) {
                if ($candFree[$i] -ge 1) {
                    $laneIdx[$laneCount] = $candIdx[$i]
                    $laneDir[$laneCount] = $candDir[$i]
                    $laneWeight[$laneCount] = $candWeight[$i]
                    $laneFree[$laneCount] = $candFree[$i]
                    $laneCount++
                }
            }
            $candIdx = $laneIdx
            $candDir = $laneDir
            $candWeight = $laneWeight
            $candFree = $laneFree
            $candCount = $laneCount
        }

        # Chain restriction: with the level's probability, insist on an arrow
        # that blocks the previous one. Without it the board would offer several
        # moves at once at every step and the order would barely matter.
        $poolIdx = $candIdx
        $poolDir = $candDir
        $poolWeight = $candWeight
        $poolCount = $candCount
        if ($linkHead -ge 0 -and $rng.NextDouble() -lt $plan.ChainChance) {
            $pickIdx = New-Object 'int[]' $candCount
            $pickDir = New-Object 'int[]' $candCount
            $pickWeight = New-Object 'double[]' $candCount
            $pickCount = 0
            # Only linked arrows that still have somewhere to go are worth
            # chaining onto. A link that produces an arrow with a full exit ray
            # buys one temporary blocked arrow but creates one permanent free
            # move, and it also stops the next arrow from having a lane to extend,
            # so it is worse than not linking at all.
            for ($i = 0; $i -lt $candCount; $i++) {
                if ($link[$candIdx[$i]] -and $candFree[$i] -ge 1) {
                    $pickIdx[$pickCount] = $candIdx[$i]
                    $pickDir[$pickCount] = $candDir[$i]
                    $pickWeight[$pickCount] = $candWeight[$i]
                    $pickCount++
                }
            }
            if ($pickCount -gt 0) {
                $poolIdx = $pickIdx
                $poolDir = $pickDir
                $poolWeight = $pickWeight
                $poolCount = $pickCount
            } else {
                # The previous arrow's lane is used up, usually because its head
                # reached the board edge. Take any arrow that blocks at least one
                # earlier arrow instead, so the board still gains a link rather
                # than opening a fresh front of free moves.
                for ($i = 0; $i -lt $candCount; $i++) {
                    if ($score[$candIdx[$i]] -gt 0) {
                        $pickIdx[$pickCount] = $candIdx[$i]
                        $pickDir[$pickCount] = $candDir[$i]
                        $pickWeight[$pickCount] = $candWeight[$i]
                        $pickCount++
                    }
                }
                if ($pickCount -gt 0) {
                    $poolIdx = $pickIdx
                    $poolDir = $pickDir
                    $poolWeight = $pickWeight
                    $poolCount = $pickCount
                }
            }
        }

        $total = 0.0
        for ($i = 0; $i -lt $poolCount; $i++) { $total += $poolWeight[$i] }
        $roll = $rng.NextDouble() * $total
        $chosen = $poolCount - 1
        for ($i = 0; $i -lt $poolCount; $i++) {
            $roll -= $poolWeight[$i]
            if ($roll -le 0) { $chosen = $i; break }
        }
        $headIdx = $poolIdx[$chosen]
        $headDir = $poolDir[$chosen]

        # Grow the body backwards from the head. The cell directly behind the
        # head is the only legal start: it is the one that keeps the exit
        # direction equal to the direction of the final body segment, which the
        # shared validator enforces.
        # The pressure term compares how much of the board is filled against how
        # far through the arrow list the level is. Falling behind means bodies
        # are too long for the board, which fills the cells before the arrows are
        # all placed and leaves the level short; running ahead means they are too
        # short and the level ends up sparse. One signed term closes both.
        $onTrack = ($tiles.Count / $plan.Arrows) * $plan.Density
        $actual = $occCells / $plan.Cells
        $growPressure = Clamp ($plan.GrowChance - (3.0 * ($actual - $onTrack))) 0.05 0.97
        $body = New-Object System.Collections.Generic.List[int]
        $body.Add($headIdx)
        if ($rng.NextDouble() -lt $growPressure) {
            $behindDir = $script:DirOpposite[$headDir]
            $br = $rowOf[$headIdx] + $script:Dr[$behindDir]
            $bc = $colOf[$headIdx] + $script:Dc[$behindDir]
            if ($br -ge 0 -and $br -lt $h -and $bc -ge 0 -and $bc -lt $w) {
                $behind = ($br * $w) + $bc
                if ($owner[$behind] -eq -1) {
                    $body.Add($behind)
                    $limit = $plan.MaxBody
                    if ($rng.NextDouble() -lt $plan.LongChance) { $limit += $plan.LongExtra }
                    $target = 2 + $rng.Next($limit - 1)
                    $current = $behind
                    $straight = $behindDir
                    $headRow = $rowOf[$headIdx]
                    $headCol = $colOf[$headIdx]
                    while ($body.Count -lt $target) {
                        $optIdx = New-Object 'int[]' 4
                        $optDir = New-Object 'int[]' 4
                        $optWeight = New-Object 'double[]' 4
                        $optCount = 0
                        for ($d = 0; $d -lt 4; $d++) {
                            $rr = $rowOf[$current] + $script:Dr[$d]
                            $cc = $colOf[$current] + $script:Dc[$d]
                            if ($rr -lt 0 -or $rr -ge $h -or $cc -lt 0 -or $cc -ge $w) { continue }
                            $nx = ($rr * $w) + $cc
                            if ($owner[$nx] -ne -1) { continue }
                            # Never occupy the arrow's own exit ray: those cells
                            # must stay clear for the arrow to leave.
                            $onRay = $false
                            switch ($headDir) {
                                0 { $onRay = ($cc -eq $headCol -and $rr -lt $headRow) }
                                1 { $onRay = ($rr -eq $headRow -and $cc -gt $headCol) }
                                2 { $onRay = ($cc -eq $headCol -and $rr -gt $headRow) }
                                3 { $onRay = ($rr -eq $headRow -and $cc -lt $headCol) }
                            }
                            if ($onRay) { continue }
                            $dup = $false
                            foreach ($b in $body) { if ($b -eq $nx) { $dup = $true; break } }
                            if ($dup) { continue }
                            # Landing here blocks every earlier arrow whose exit
                            # ray runs through it, which is what builds
                            # intersections and extra layers of blocking.
                            $weight = 1.0 + ($plan.CrossWeight * $score[$nx])
                            if ($d -ne $straight) { $weight += $plan.TurnChance }
                            if ($link[$nx]) { $weight += 4.0 }
                            $optIdx[$optCount] = $nx
                            $optDir[$optCount] = $d
                            $optWeight[$optCount] = $weight
                            $optCount++
                        }
                        if ($optCount -eq 0) { break }
                        $total = 0.0
                        for ($i = 0; $i -lt $optCount; $i++) { $total += $optWeight[$i] }
                        $roll = $rng.NextDouble() * $total
                        $picked = $optCount - 1
                        for ($i = 0; $i -lt $optCount; $i++) {
                            $roll -= $optWeight[$i]
                            if ($roll -le 0) { $picked = $i; break }
                        }
                        $body.Add($optIdx[$picked])
                        $current = $optIdx[$picked]
                        $straight = $optDir[$picked]
                    }
                }
            }
        }

        # A long body moved a tail stub too close to a neighbour: trim back to the
        # longest prefix that still clears. The body is head-first, so a prefix
        # is always contiguous with the head and the exit direction stays valid.
        $probe = New-Object System.Collections.Generic.List[int]
        for ($i = 0; $i -lt $body.Count; $i++) { $probe.Add($body[$i]) }
        $gap = Get-GapFlat (Get-BodyFootprint $probe $headDir $rowOf $colOf) $pFp $pMinX $pMinY $pMaxX $pMaxY $placedCount
        while ($gap -lt $script:MinClearance -and $probe.Count -gt 1) {
            $probe.RemoveAt($probe.Count - 1)
            $gap = Get-GapFlat (Get-BodyFootprint $probe $headDir $rowOf $colOf) $pFp $pMinX $pMinY $pMaxX $pMaxY $placedCount
        }
        if ($gap -lt $tightest) { $tightest = $gap }
        if ($probe.Count -lt $body.Count) { $body = $probe }

        # Cheap invariants: every body entry must be a free cell and the body
        # must be contiguous with the head. Anything else ships a broken asset.
        $headRow = $rowOf[$headIdx]
        $headCol = $colOf[$headIdx]
        foreach ($idx in $body) {
            if ($owner[$idx] -ne -1) {
                throw "Overlapping body at pack $($plan.Pack) level $($plan.Order), cell $idx"
            }
        }
        for ($i = 1; $i -lt $body.Count; $i++) {
            $a = $body[$i - 1]
            $b = $body[$i]
            $gapCells = [math]::Abs($rowOf[$a] - $rowOf[$b]) + [math]::Abs($colOf[$a] - $colOf[$b])
            if ($gapCells -ne 1) {
                $dump = ($body | ForEach-Object { "($_ = $($rowOf[$_]),$($colOf[$_]))" }) -join ' '
                throw "Non-contiguous body at pack $($plan.Pack) level $($plan.Order) (index $i): $dump"
            }
        }

        # Commit ownership and the ray counters. score only follows the exit ray
        # from the head, because that ray is what blocks other arrows; every body
        # cell updates ahead, because any occupied cell blocks a ray.
        $cellsTailFirst = New-Object System.Collections.Generic.List[int]
        for ($i = $body.Count - 1; $i -ge 0; $i--) {
            $idx = $body[$i]
            $cellsTailFirst.Add($idx)
            $owner[$idx] = $tiles.Count
            $r = $rowOf[$idx]
            $c = $colOf[$idx]
            $free--
            for ($d = 0; $d -lt 4; $d++) {
                $back = $script:DirOpposite[$d]
                $rr = $r + $script:Dr[$back]
                $cc = $c + $script:Dc[$back]
                while ($rr -ge 0 -and $rr -lt $h -and $cc -ge 0 -and $cc -lt $w) {
                    $ahead[(($rr * $w) + $cc) * 4 + $d]++
                    $rr += $script:Dr[$back]
                    $cc += $script:Dc[$back]
                }
            }
        }
        $rr = $headRow + $script:Dr[$headDir]
        $cc = $headCol + $script:Dc[$headDir]
        $newIndex = $tiles.Count + 1
        while ($rr -ge 0 -and $rr -lt $h -and $cc -ge 0 -and $cc -lt $w) {
            $rayIdx = ($rr * $w) + $cc
            $score[$rayIdx]++
            if ($cellArrows[$rayIdx] -eq $null) { $cellArrows[$rayIdx] = New-Object 'int[]' $plan.Arrows }
            # One slot per arrow, holding that arrow's index offset by one so an
            # untouched slot reads as "not on this ray" instead of matching index
            # zero by accident.
            $cellArrows[$rayIdx][$tiles.Count] = $newIndex
            $rr += $script:Dr[$headDir]
            $cc += $script:Dc[$headDir]
        }
        # Any earlier arrow whose exit ray runs through one of the cells just
        # taken is now blocked. Marking them here is what lets the next placement
        # prefer an arrow nobody is standing in front of yet.
        foreach ($idx in $body) {
            $rays = $cellArrows[$idx]
            if ($rays -eq $null) { continue }
            for ($j = 0; $j -lt $tiles.Count; $j++) {
                if ($rays[$j] -eq ($j + 1)) { $hasBlocker[$j] = $true }
            }
        }

        $tiles.Add(@{ Dir = $headDir; Head = $headIdx })
        $tileDirs.Add($headDir)
        $tileHeads.Add(@{ Row = $headRow; Col = $headCol })
        $tileCells.Add($cellsTailFirst)
        $footprint = Get-BodyFootprint $body $headDir $rowOf $colOf
        $pFp[$placedCount] = $footprint
        $pMinX[$placedCount] = $footprint.MinX
        $pMinY[$placedCount] = $footprint.MinY
        $pMaxX[$placedCount] = $footprint.MaxX
        $pMaxY[$placedCount] = $footprint.MaxY
        $placedCount++

        if ($body.Count -gt $longest) { $longest = $body.Count }
        if ($body.Count -gt 2) { $bent++ }
        $occCells += $body.Count

    }

    # --- Measure the level ---------------------------------------------------
    $n = $tiles.Count
    $dirs = $tileDirs.ToArray()
    $heads = $tileHeads.ToArray()
    $cells = $tileCells.ToArray()
    $solvable = ($n -gt 0) -and (-not $stalled) -and (Test-Solvable $plan $cells $dirs $heads)

    # Blockers and dependency depth. An arrow can only be blocked by one placed
    # after it, because only those leave before it, so the blocker graph is
    # acyclic and depth falls out of a single backwards pass.
    $blockerCount = New-Object 'int[]' $n
    $blockerFlat = New-Object 'int[]' ([math]::Max(1, $n * $n))
    for ($i = 0; $i -lt $n; $i++) {
        $d = $dirs[$i]
        $r = $heads[$i].Row + $script:Dr[$d]
        $c = $heads[$i].Col + $script:Dc[$d]
        while ($r -ge 0 -and $r -lt $h -and $c -ge 0 -and $c -lt $w) {
            $o = $owner[($r * $w) + $c]
            if ($o -ge 0 -and $o -ne $i) {
                $dup = $false
                for ($k = 0; $k -lt $blockerCount[$i]; $k++) {
                    if ($blockerFlat[($i * $n) + $k] -eq $o) { $dup = $true; break }
                }
                if (-not $dup) {
                    $blockerFlat[($i * $n) + $blockerCount[$i]] = $o
                    $blockerCount[$i]++
                }
            }
            $r += $script:Dr[$d]
            $c += $script:Dc[$d]
        }
    }

    $depth = New-Object 'int[]' $n
    $layers = New-Object System.Collections.Generic.HashSet[int]
    $open = 0
    $multi = 0
    $crossings = 0
    $maxBlockers = 0
    for ($i = $n - 1; $i -ge 0; $i--) {
        $bestDepth = 0
        for ($k = 0; $k -lt $blockerCount[$i]; $k++) {
            $candidateDepth = $depth[$blockerFlat[($i * $n) + $k]] + 1
            if ($candidateDepth -gt $bestDepth) { $bestDepth = $candidateDepth }
        }
        $depth[$i] = $bestDepth
        [void]$layers.Add($bestDepth)
        if ($blockerCount[$i] -eq 0) { $open++ }
        if ($blockerCount[$i] -gt 1) { $multi++ }
        if ($blockerCount[$i] -gt $maxBlockers) { $maxBlockers = $blockerCount[$i] }
        $crossings += $blockerCount[$i]
    }
    $maxDepth = 0
    foreach ($d in $depth) { if ($d -gt $maxDepth) { $maxDepth = $d } }

    $dirSet = New-Object System.Collections.Generic.HashSet[int]
    foreach ($d in $dirs) { [void]$dirSet.Add($d) }

    return @{
        Id         = 'pack-0{0}-level-{1}' -f $plan.Pack, $plan.Order.ToString('00')
        Plan       = $plan
        Width      = $w
        Height     = $h
        Cells      = $cellCount
        Shape      = $shape.Count
        Dirs       = $dirs
        Heads      = $heads
        BodyCells  = $cells
        Arrows     = $n
        Occupied   = $occCells
        Density    = $(if ($cellCount -gt 0) { $occCells / $cellCount } else { 0.0 })
        Longest    = $longest
        Bent       = $bent
        Directions = $dirSet.Count
        Open       = $open
        Depth      = $maxDepth
        Layers     = $layers.Count
        Multi      = $multi
        MaxBlock   = $maxBlockers
        Crossings  = $crossings
        Clearance  = $tightest
        Solvable   = $solvable
        Stalled    = $stalled
    }
}

# How close a layout came to what the level is supposed to be. Solvability is a
# gate, not a term: an unsolvable layout scores nothing however dense it is.
#
# Every term is written so it keeps discriminating past its target instead of
# flattening at one. A term that saturates stops steering the seed sweep exactly
# when a metric is still far off, which is how a level ends up dense and layered
# but still handing the player fifteen free arrows at the first move.
function Get-LevelScore($result) {
    $plan = $result.Plan
    if (-not $result.Solvable) { return -1000.0 }
    if ($result.Stalled) { return -100.0 }

    $score = 0.0
    # Arrow count first: a layout that fell short is missing arrows, not hard.
    $score += 6.0 * ($result.Arrows / $plan.Arrows)
    # Depth uses a square-root ramp, so 20 of a wanted 40 still scores half.
    $depthWanted = [math]::Max(1, $plan.TargetDepth)
    $score += 5.0 * [math]::Min(1.0, [math]::Sqrt($result.Depth / $depthWanted))
    # Open moves are the sharpest term: a reciprocal, because going from two
    # obvious moves to five should cost more than going from five to eight.
    $openGap = [math]::Abs($result.Open - $plan.TargetOpen)
    $score += 5.0 / (1.0 + $openGap)
    $score += 3.0 * [math]::Min(1.0, $result.Density / $plan.Density)
    # Overshooting the occupancy target is not free either: an early board that
    # fills up completely reads as a late one.
    $score -= 2.0 * [math]::Max(0.0, ($result.Density - $plan.Density - 0.12) / $plan.Density)
    $score += 2.0 * [math]::Min(1.0, $result.Multi / [math]::Max(1, $plan.TargetMulti))
    $score += 2.0 * [math]::Min(1.0, $result.Bent / [math]::Max(1, $plan.TargetBent))
    $score += 1.5 * [math]::Min(1.0, $result.Directions / 4.0)
    $score += 1.0 * [math]::Min(1.0, $result.Crossings / [math]::Max(1.0, $result.Arrows * 0.8))
    $score += 2.0 * [math]::Min(1.0, $result.Clearance / $script:MinClearance)
    # A board with a single blocking depth is one layer, not a plan.
    $score += 1.0 * [math]::Min(1.0, $result.Layers / 6.0)
    return $score
}

function Convert-LevelJson($result) {
    # Format strings are built into a variable first: PowerShell binds the comma
    # before -f inside a method argument list, which would silently split the
    # argument list.
    $w = $result.Width
    $parts = New-Object System.Collections.Generic.List[string]
    for ($i = 0; $i -lt $result.Arrows; $i++) {
        $cells = $result.BodyCells[$i]
        $headRow = $result.Heads[$i].Row
        $headCol = $result.Heads[$i].Col
        $dir = $script:DirNames[$result.Dirs[$i]]
        if ($cells.Count -le 1) {
            $text = '{{"row":{0},"column":{1},"direction":"{2}"}}' -f $headRow, $headCol, $dir
            [void]$parts.Add($text)
            continue
        }
        $cellTexts = New-Object System.Collections.Generic.List[string]
        for ($k = 0; $k -lt $cells.Count; $k++) {
            $idx = $cells[$k]
            $row = $idx - ($idx % $w)
            $text = '{{"row":{0},"column":{1}}}' -f [int]($row / $w), ($idx % $w)
            [void]$cellTexts.Add($text)
        }
        $cellJson = $cellTexts -join ','
        $text = '{{"row":{0},"column":{1},"direction":"{2}","cells":[{3}]}}' -f `
            $headRow, $headCol, $dir, $cellJson
        [void]$parts.Add($text)
    }
    $tileJson = $parts -join ','
    $json = '{{"id":"{0}","pack":{1},"order":{2},"width":{3},"height":{4},"tiles":[{5}],"parMoves":{6}}}' -f `
        $result.Id, $result.Plan.Pack, $result.Plan.Order, $result.Width, $result.Height, `
        $tileJson, $result.Arrows
    return $json
}

$encoding = New-Object System.Text.UTF8Encoding($false)
$written = 0
$problems = New-Object System.Collections.Generic.List[string]

foreach ($packNumber in $Packs) {
    foreach ($level in $Levels) {
        $plan = Get-LevelPlan $packNumber $level

        # Board geometry does not depend on the seed, so the shape, the row and
        # column lookup and the single-cell footprints are built once per level.
        $cellCount = $plan.Columns * $plan.Rows
        $rowOf = New-Object 'int[]' $cellCount
        $colOf = New-Object 'int[]' $cellCount
        for ($r = 0; $r -lt $plan.Rows; $r++) {
            for ($c = 0; $c -lt $plan.Columns; $c++) {
                $rowOf[($r * $plan.Columns) + $c] = $r
                $colOf[($r * $plan.Columns) + $c] = $c
            }
        }
        $shape = Get-Shape $plan
        if ($shape.Count -lt $plan.Arrows) {
            throw "Pack $packNumber level ${level}: shape has $($shape.Count) cells for $($plan.Arrows) arrows"
        }
        $singleFp = New-SingleFootprints $plan.Columns $plan.Rows $rowOf $colOf

        # A chain-biased attempt is usually already close to the targets, so a
        # short sweep is enough. A board that cannot reach them is reported
        # rather than quietly shipped, and a board dense enough to trap the
        # placement loop gets a second, longer sweep before giving up: a stalled
        # layout has fewer arrows than the level asks for, and a level with
        # missing arrows is not a harder level, it is a shorter one.
        $best = $null
        $bestScore = [double]::MinValue
        $attempt = 0
        $limit = $MaxAttempts
        while ($attempt -lt $limit) {
            $candidate = New-LevelObject $plan $attempt $rowOf $colOf $shape $singleFp
            $score = Get-LevelScore $candidate
            if ($score -gt $bestScore) {
                $bestScore = $score
                $best = $candidate
            }
            if ($best.Solvable -and $best.Arrows -eq $plan.Arrows -and
                $best.Clearance -ge ($script:MinClearance + 0.02)) { break }
            $attempt++
            if ($attempt -ge $limit -and (-not $best.Solvable -or $best.Arrows -ne $plan.Arrows) -and $limit -lt ($MaxAttempts * 4)) {
                $limit = $limit * 2
            }
        }

        $json = Convert-LevelJson $best
        $path = Join-Path $levelsDir ("$($best.Id).json")
        [System.IO.File]::WriteAllText($path, $json, $encoding)

        $status = if (-not $best.Solvable) { 'UNSOLVABLE' }
                  elseif ($best.Arrows -ne $plan.Arrows) { "SHORT-$($plan.Arrows - $best.Arrows)" }
                  elseif ($best.Clearance -lt $script:MinClearance) { 'TIGHT' }
                  else { 'ok' }
        if ($status -ne 'ok') { [void]$problems.Add("$($best.Id) $status") }

        $gapText = if ($best.Clearance -ge [double]::MaxValue) { 'n/a' } else { [Math]::Round($best.Clearance, 3) }
        Write-Host ("{0}  {1}x{2}  {3,2}/{4} arrows  fill {5,3}%  open {6,2}/{7}  depth {8,2}/{9}  layers {10,2}  multi {11,2}  bent {12,2}/{13}  long {14}  gap {15}  {16}" -f `
                $best.Id, $best.Width, $best.Height, $best.Arrows, $plan.Arrows, `
                [Math]::Round(100 * $best.Density), $best.Open, $plan.TargetOpen, `
                $best.Depth, $plan.TargetDepth, $best.Layers, $best.Multi, `
                $best.Bent, $plan.TargetBent, $best.Longest, $gapText, $status)
        $written++
    }
}

Write-Host "Generated $written levels in $levelsDir"
if ($problems.Count -gt 0) {
    Write-Warning "Levels that missed a target: $($problems -join ', ')"
}