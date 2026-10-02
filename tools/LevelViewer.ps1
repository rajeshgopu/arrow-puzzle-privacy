#requires -Version 5.1
<#
.SYNOPSIS
  Desktop viewer for the Arrow Puzzle level assets.

.DESCRIPTION
  Loads every level JSON from app/src/main/assets/levels, renders the board
  with its arrows, and reports whether the level is solvable using the same
  greedy rule as the game engine (a move never becomes invalid once it is
  valid, so greedy removal is a complete solvability check).

.PARAMETER Dump
  Print a text summary for every level and exit without opening a window.

.PARAMETER Smoke
  Render every level to an offscreen bitmap to verify the drawing code, then
  exit without showing a window.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools\LevelViewer.ps1
  powershell -ExecutionPolicy Bypass -File tools\LevelViewer.ps1 -Dump
#>
[CmdletBinding()]
param(
    [switch]$Dump,
    [switch]$Smoke,
    [string]$ExportTo
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$script:ScriptRoot = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$script:Levels = @()
$script:Index = 0
$script:Current = $null
$script:Solve = $null
$script:ShowOrder = $false

# Neon palette, matching ui/theme/Theme.kt. The viewer paints arrows the same way
# the game does: a connected glowing tube through the body's cell centres.
$neonPalette = @(
    [System.Drawing.Color]::FromArgb(43, 231, 255),   # cyan
    [System.Drawing.Color]::FromArgb(255, 61, 206),   # pink
    [System.Drawing.Color]::FromArgb(155, 92, 255),   # purple
    [System.Drawing.Color]::FromArgb(61, 255, 158),   # green
    [System.Drawing.Color]::FromArgb(255, 138, 31),   # orange
    [System.Drawing.Color]::FromArgb(255, 209, 26),   # yellow
    [System.Drawing.Color]::FromArgb(77, 124, 255)    # blue
)
$stageVoid = [System.Drawing.Color]::FromArgb(5, 6, 15)
$stageDeep = [System.Drawing.Color]::FromArgb(10, 10, 28)
$boardPlate = [System.Drawing.Color]::FromArgb(16, 16, 41)
$boardFrame = [System.Drawing.Color]::FromArgb(46, 46, 107)
$neonTextDim = [System.Drawing.Color]::FromArgb(154, 163, 212)
$neonCore = [System.Drawing.Color]::White

$dirVector = @{
    UP    = @(-1, 0)
    DOWN  = @(1, 0)
    LEFT  = @(0, -1)
    RIGHT = @(0, 1)
}

function Get-LevelsDirectory {
    $scriptDir = $script:ScriptRoot
    $candidates = @(
        (Join-Path $scriptDir "..\app\src\main\assets\levels"),
        (Join-Path (Get-Location).Path "app\src\main\assets\levels")
    )
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    throw "Level assets directory not found. Looked in: $($candidates -join '; ')"
}

function Load-Levels {
    $dir = Get-LevelsDirectory
    $files = Get-ChildItem -LiteralPath $dir -Filter *.json | Sort-Object Name
    if ($files.Count -eq 0) {
        throw "No level JSON files found in $dir"
    }
    return @($files | ForEach-Object {
        $level = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
        $level | Add-Member -NotePropertyName FileName -NotePropertyValue $_.Name -Force
        $level
    })
}

function Get-BodyCells {
    param($Tile)
    # `cells` is optional (absent for straight arrows) and strict mode throws on
    # missing properties, so probe the property bag explicitly.
    $property = $Tile.PSObject.Properties['cells']
    if ($null -ne $property -and $null -ne $property.Value -and $property.Value.Count -gt 0) {
        return @($property.Value)
    }
    return @($Tile)
}

function Solve-Level {
    param($Level)

    $width = [int]$Level.width
    $height = [int]$Level.height
    $remaining = New-Object 'System.Collections.Generic.HashSet[string]'
    $tileCells = @{}
    foreach ($tile in $Level.tiles) {
        $cells = @(Get-BodyCells $tile | ForEach-Object { "$([int]$_.row),$([int]$_.column)" })
        $tileCells["$([int]$tile.row),$([int]$tile.column)"] = $cells
        foreach ($cell in $cells) { [void]$remaining.Add($cell) }
    }

    $order = @{}
    $step = 0
    $progress = $true
    while ($remaining.Count -gt 0 -and $progress) {
        $progress = $false
        foreach ($tile in $Level.tiles) {
            $key = "$([int]$tile.row),$([int]$tile.column)"
            if (-not $remaining.Contains($key)) { continue }
            $own = $tileCells[$key]

            $dr = 0
            $dc = 0
            switch ([string]$tile.direction) {
                'UP'    { $dr = -1; $dc = 0 }
                'DOWN'  { $dr = 1; $dc = 0 }
                'LEFT'  { $dr = 0; $dc = -1 }
                'RIGHT' { $dr = 0; $dc = 1 }
            }

            $head = @(Get-BodyCells $tile)[-1]
            $r = [int]$head.row + $dr
            $c = [int]$head.column + $dc
            $clear = $true
            while ($r -ge 0 -and $r -lt $height -and $c -ge 0 -and $c -lt $width) {
                $cellKey = "$r,$c"
                # The arrow's own body leaves with it, so it never blocks itself.
                if ($remaining.Contains($cellKey) -and ($own -notcontains $cellKey)) { $clear = $false; break }
                $r += $dr
                $c += $dc
            }

            if ($clear) {
                foreach ($cell in $own) { [void]$remaining.Remove($cell) }
                $step++
                $order[$key] = $step
                $progress = $true
            }
        }
    }

    return [pscustomobject]@{
        Solvable = ($remaining.Count -eq 0)
        Order    = $order
        Steps    = $step
    }
}

function New-RoundedRectPath {
    param([float]$X, [float]$Y, [float]$Width, [float]$Height, [float]$Radius)

    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $Radius * 2
    $path.AddArc($X, $Y, $d, $d, 180, 90)
    $path.AddArc($X + $Width - $d, $Y, $d, $d, 270, 90)
    $path.AddArc($X + $Width - $d, $Y + $Height - $d, $d, $d, 0, 90)
    $path.AddArc($X, $Y + $Height - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

function Get-TileColors {
    param($Level)

    # Same rule as the game's assignArrowColors: never give two arrows that touch
    # the same colour, so a dense board stays readable.
    $occupancy = @{}
    for ($i = 0; $i -lt $Level.tiles.Count; $i++) {
        foreach ($cell in @(Get-BodyCells $Level.tiles[$i])) {
            $occupancy["$([int]$cell.row),$([int]$cell.column)"] = $i
        }
    }

    $colors = New-Object 'System.Collections.Generic.List[System.Drawing.Color]'
    for ($i = 0; $i -lt $Level.tiles.Count; $i++) {
        $forbidden = New-Object 'System.Collections.Generic.HashSet[System.Drawing.Color]'
        foreach ($cell in @(Get-BodyCells $Level.tiles[$i])) {
            $r = [int]$cell.row
            $c = [int]$cell.column
            $neighbours = @("$($r - 1),$c", "$($r + 1),$c", "$r,$($c - 1)", "$r,$($c + 1)")
            foreach ($key in $neighbours) {
                $owner = $occupancy[$key]
                if ($null -ne $owner -and $owner -lt $i) { [void]$forbidden.Add($colors[$owner]) }
            }
        }
        $pick = $null
        foreach ($candidate in $neonPalette) {
            if (-not $forbidden.Contains($candidate)) { $pick = $candidate; break }
        }
        if ($null -eq $pick) { $pick = $neonPalette[$i % $neonPalette.Count] }
        $colors.Add($pick)
    }
    return , $colors.ToArray()
}

function New-RoundedPolylinePath {
    param([System.Drawing.PointF[]]$Points, [float]$Radius)

    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    if ($null -eq $Points -or $Points.Length -eq 0) { return $path }
    if ($Points.Length -eq 1) {
        $path.AddLine($Points[0].X, $Points[0].Y, $Points[0].X, $Points[0].Y)
        return $path
    }

    # Every real direction change becomes a rounded quadratic Bezier bend whose
    # control point is the corner itself, exactly as ArrowShape.kt does. The curve
    # is tessellated because AddLines only draws straight segments.
    $out = New-Object 'System.Collections.Generic.List[System.Drawing.PointF]'
    $out.Add($Points[0])
    for ($i = 1; $i -lt ($Points.Length - 1); $i++) {
        $prev = $Points[$i - 1]
        $corner = $Points[$i]
        $next = $Points[$i + 1]
        $inX = [double]$corner.X - [double]$prev.X
        $inY = [double]$corner.Y - [double]$prev.Y
        $outX = [double]$next.X - [double]$corner.X
        $outY = [double]$next.Y - [double]$corner.Y
        $inLen = [Math]::Sqrt(($inX * $inX) + ($inY * $inY))
        $outLen = [Math]::Sqrt(($outX * $outX) + ($outY * $outY))
        if ($inLen -lt 0.01 -or $outLen -lt 0.01) { continue }

        $inDirX = $inX / $inLen
        $inDirY = $inY / $inLen
        $outDirX = $outX / $outLen
        $outDirY = $outY / $outLen
        $cross = ($inDirX * $outDirY) - ($inDirY * $outDirX)
        $dot = ($inDirX * $outDirX) + ($inDirY * $outDirY)
        if ([Math]::Abs($cross) -lt 0.01 -and $dot -gt 0) {
            $out.Add($corner)
            continue
        }

        # Clamp so neighbouring bends never overlap.
        $r = [Math]::Min($Radius, [Math]::Min($inLen * 0.42, $outLen * 0.42))
        $beforeX = [double]$corner.X - $inDirX * $r
        $beforeY = [double]$corner.Y - $inDirY * $r
        $afterX = [double]$corner.X + $outDirX * $r
        $afterY = [double]$corner.Y + $outDirY * $r

        for ($s = 0; $s -le 8; $s++) {
            $t = $s / 8.0
            $u = 1.0 - $t
            $w0 = $u * $u
            $w1 = 2.0 * $u * $t
            $w2 = $t * $t
            $out.Add([System.Drawing.PointF]::new(
                [float](($beforeX * $w0) + ([double]$corner.X * $w1) + ($afterX * $w2)),
                [float](($beforeY * $w0) + ([double]$corner.Y * $w1) + ($afterY * $w2))))
        }
    }
    $out.Add($Points[$Points.Length - 1])
    $path.AddLines([System.Drawing.PointF[]]$out.ToArray())
    return $path
}

function New-ArrowGeometry {
    param($Tile, $OffX, $OffY, [float]$Cell)

    $cells = @(Get-BodyCells $Tile)
    $dir = [string]$Tile.direction
    $vector = $dirVector[$dir]
    $fx = [double]$vector[0]
    $fy = [double]$vector[1]

$strokeWidth = [float]($Cell * 0.088)
    $bendRadius = [float]($Cell * 0.42)
    $headLength = [float]($Cell * 0.28)
    $headWidth = [float]($Cell * 0.20)
    $headOverlap = [float]($Cell * 0.025)
    $tailReach = [float]($Cell * 0.5)

    $centers = New-Object 'System.Collections.Generic.List[System.Drawing.PointF]'
    foreach ($bodyCell in $cells) {
        $centers.Add([System.Drawing.PointF]::new(
            [float]($OffX + ([int]$bodyCell.column * $Cell) + ($Cell / 2)),
            [float]($OffY + ([int]$bodyCell.row * $Cell) + ($Cell / 2))))
    }

    # Head orientation comes from the ACTUAL final segment, mirroring
    # ArrowShape.kt, so the head always points where the arrow exits.
    $headCentre = $centers[$centers.Count - 1]
    $fx = [double]0
    $fy = [double]0
    if ($centers.Count -ge 2) {
        $previous = $centers[$centers.Count - 2]
        $sx = [double]$headCentre.X - [double]$previous.X
        $sy = [double]$headCentre.Y - [double]$previous.Y
        $len = [Math]::Sqrt(($sx * $sx) + ($sy * $sy))
        if ($len -gt 0.01) { $fx = $sx / $len; $fy = $sy / $len }
    }
    if ($fx -eq 0 -and $fy -eq 0) {
        $vector = $dirVector[[string]$Tile.direction]
        $fx = [double]$vector[0]
        $fy = [double]$vector[1]
    }

    $base = [System.Drawing.PointF]::new(
        [float]($headCentre.X - ($fx * $headOverlap)), [float]($headCentre.Y - ($fy * $headOverlap)))
    $connection = [System.Drawing.PointF]::new(
        [float]($base.X - ($fx * $headOverlap)), [float]($base.Y - ($fy * $headOverlap)))
    $tip = [System.Drawing.PointF]::new(
        [float]($connection.X + ($fx * $headLength)), [float]($connection.Y + ($fy * $headLength)))

    $vertices = New-Object 'System.Collections.Generic.List[System.Drawing.PointF]'
    if ($centers.Count -eq 1) {
        $vertices.Add([System.Drawing.PointF]::new(
            [float]($base.X - ($fx * $tailReach)), [float]($base.Y - ($fy * $tailReach))))
    } else {
        $tail = $centers[0]
        $next = $centers[1]
        $dx = [double]$tail.X - [double]$next.X
        $dy = [double]$tail.Y - [double]$next.Y
        $len = [Math]::Sqrt(($dx * $dx) + ($dy * $dy))
        if ($len -lt 0.01) { $len = 1.0 }
        $vertices.Add([System.Drawing.PointF]::new(
            [float]($tail.X + ($dx / $len * $tailReach)), [float]($tail.Y + ($dy / $len * $tailReach))))
        for ($i = 0; $i -lt ($centers.Count - 1); $i++) { $vertices.Add($centers[$i]) }
    }
    $vertices.Add($base)

    # Kite head: neck matches the tube, wings flare out, tip on the body axis.
    $perpX = -$fy
    $perpY = $fx
    $neckHalfWidth = [float]($strokeWidth * 0.62)
    $wingHalfWidth = [float]($headWidth * 0.5)
    $neckA = [System.Drawing.PointF]::new([float]($connection.X + $perpX * $neckHalfWidth), [float]($connection.Y + $perpY * $neckHalfWidth))
    $neckB = [System.Drawing.PointF]::new([float]($connection.X - $perpX * $neckHalfWidth), [float]($connection.Y - $perpY * $neckHalfWidth))
    $wingCenter = [System.Drawing.PointF]::new(
        [float]($connection.X + ($fx * $headLength * 0.32)), [float]($connection.Y + ($fy * $headLength * 0.32)))
    $wingA = [System.Drawing.PointF]::new(
        [float]($wingCenter.X + $perpX * $wingHalfWidth), [float]($wingCenter.Y + $perpY * $wingHalfWidth))
    $wingB = [System.Drawing.PointF]::new(
        [float]($wingCenter.X - $perpX * $wingHalfWidth), [float]($wingCenter.Y - $perpY * $wingHalfWidth))

    function New-Kite([float]$lengthFactor, [float]$widthFactor, [float]$neckFactor, [float]$wingShift) {
        $gTip = [System.Drawing.PointF]::new(
            [float]($connection.X + ($fx * $headLength * $lengthFactor)),
            [float]($connection.Y + ($fy * $headLength * $lengthFactor)))
        $gWingCenter = [System.Drawing.PointF]::new(
            [float]($connection.X + ($fx * $headLength * $wingShift * $lengthFactor)),
            [float]($connection.Y + ($fy * $headLength * $wingShift * $lengthFactor)))
        $gWingHalf = [float]($wingHalfWidth * $widthFactor)
        $gNeckHalf = [float]($neckHalfWidth * $neckFactor)
        return [System.Drawing.PointF[]]@(
            $gTip,
            [System.Drawing.PointF]::new([float]($gWingCenter.X + $perpX * $gWingHalf), [float]($gWingCenter.Y + $perpY * $gWingHalf)),
            [System.Drawing.PointF]::new([float]($connection.X + $perpX * $gNeckHalf), [float]($connection.Y + $perpY * $gNeckHalf)),
            [System.Drawing.PointF]::new([float]($connection.X - $perpX * $gNeckHalf), [float]($connection.Y - $perpY * $gNeckHalf)),
            [System.Drawing.PointF]::new([float]($gWingCenter.X - $perpX * $gWingHalf), [float]($gWingCenter.Y - $perpY * $gWingHalf)))
    }

    return [pscustomobject]@{
        Shaft      = New-RoundedPolylinePath -Points ([System.Drawing.PointF[]]$vertices.ToArray()) -Radius $bendRadius
        Head       = [System.Drawing.PointF[]]($tip, $wingA, $neckA, $neckB, $wingB)
        HeadCore   = New-Kite -lengthFactor 0.88 -widthFactor 0.5 -neckFactor 0.645 -wingShift 0.30
        HeadGlows  = @(
            (New-Kite -lengthFactor 1.18 -widthFactor 1.18 -neckFactor 1.18 -wingShift 0.32),
            (New-Kite -lengthFactor 1.38 -widthFactor 1.38 -neckFactor 1.38 -wingShift 0.32),
            (New-Kite -lengthFactor 1.62 -widthFactor 1.62 -neckFactor 1.62 -wingShift 0.32))
        Base       = $connection
        Width      = $strokeWidth
        Cell       = $Cell
        HeadPos    = $headCentre
    }
}

function Render-Board {
    param(
        [System.Drawing.Graphics]$Graphics,
        $Level,
        [int]$Width,
        [int]$Height,
        [bool]$ShowOrder,
        $Order
    )

    $Graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $Graphics.Clear($stageVoid)
    if ($null -eq $Level) { return }

    $cols = [int]$Level.width
    $rows = [int]$Level.height
    $padding = 20
    $availW = [double]$Width - (2.0 * $padding)
    $availH = [double]$Height - (2.0 * $padding)
    $cell = [Math]::Floor([Math]::Min($availW / [double]$cols, $availH / [double]$rows))
    if ($cell -lt 8) { $cell = 8 }
    $boardW = $cell * $cols
    $boardH = $cell * $rows
    $offX = [Math]::Floor(([double]$Width - $boardW) / 2.0)
    $offY = [Math]::Floor(([double]$Height - $boardH) / 2.0)

    $numberFont = New-Object System.Drawing.Font('Segoe UI', [float]($cell * 0.22), [System.Drawing.FontStyle]::Bold)
    $labelBrush = New-Object System.Drawing.SolidBrush ($neonCore)
    $labelBg = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(200, 5, 6, 15))

    try {
        # Dark plate the maze sits on.
        $plateX = [float]($offX - ($cell * 0.28))
        $plateY = [float]($offY - ($cell * 0.28))
        $plateW = [float]($boardW + ($cell * 0.56))
        $plateH = [float]($boardH + ($cell * 0.56))
        $plateRadius = [float]($cell * 0.30)
        $plateBrush = New-Object System.Drawing.SolidBrush ($boardPlate)
        $platePath = New-RoundedRectPath -X $plateX -Y $plateY -Width $plateW -Height $plateH -Radius $plateRadius
        $Graphics.FillPath($plateBrush, $platePath)
        $framePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(170, $boardFrame)), ([float]($cell * 0.045))
        $Graphics.DrawPath($framePen, $platePath)
        $platePath.Dispose()
        $plateBrush.Dispose()
        $framePen.Dispose()

        # Faint dot on every cell so the grid reads through the maze.
        $dotBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(28, $neonTextDim))
        $dotSize = [float]($cell * 0.08)
        for ($r = 0; $r -lt $rows; $r++) {
            for ($c = 0; $c -lt $cols; $c++) {
                $cx = [float]($offX + ($c * $cell) + ($cell / 2))
                $cy = [float]($offY + ($r * $cell) + ($cell / 2))
                $Graphics.FillEllipse($dotBrush, ($cx - $dotSize), ($cy - $dotSize), ($dotSize * 2), ($dotSize * 2))
            }
        }
        $dotBrush.Dispose()

        $colors = Get-TileColors -Level $Level
        $glowTint = [System.Drawing.Color]::FromArgb(143, 143, 148)
        $tileIndex = 0

        foreach ($tile in $Level.tiles) {
            $geometry = New-ArrowGeometry -Tile $tile -OffX $offX -OffY $offY -Cell $cell
            $color = $colors[$tileIndex]
            $tileIndex++

            $keyPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(235, 5, 6, 15)), ([float]($geometry.Width + ($cell * 0.075)))
            $glowWide = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(15, $glowTint)), ([float]($geometry.Width + ($cell * 0.50)))
            $glowMid = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(31, $glowTint)), ([float]($geometry.Width + ($cell * 0.24)))
            $glowTight = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(56, $glowTint)), ([float]($geometry.Width + ($cell * 0.11)))
            $bodyPen = New-Object System.Drawing.Pen $color, $geometry.Width
            $corePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 255, 255, 255)), ([float]($geometry.Width * 0.34))
            foreach ($pen in @($keyPen, $glowWide, $glowMid, $glowTight, $bodyPen, $corePen)) {
                $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
                $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
                $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
            }

            # Dark keyline keeps neighbouring arrows separable.
            $Graphics.DrawPath($keyPen, $geometry.Shaft)
            # Widening glow passes, then the saturated tube, then the hot core.
            $Graphics.DrawPath($glowWide, $geometry.Shaft)
            $Graphics.DrawPath($glowMid, $geometry.Shaft)
            $Graphics.DrawPath($glowTight, $geometry.Shaft)
            $Graphics.DrawPath($bodyPen, $geometry.Shaft)
            $Graphics.DrawPath($corePen, $geometry.Shaft)

            $headBrush = New-Object System.Drawing.SolidBrush $color
            $coreBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(128, 255, 255, 255))
            # Head halo as grown fills so it follows the silhouette smoothly,
            # then the head itself and its crisp inner core.
            $haloAlphas = @(41, 29, 18)
            for ($g = 0; $g -lt 3; $g++) {
                $haloBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb($haloAlphas[$g], $glowTint))
                $glowPath = New-Object System.Drawing.Drawing2D.GraphicsPath
                $glowPath.AddPolygon([System.Drawing.PointF[]]$geometry.HeadGlows[$g])
                $Graphics.FillPath($haloBrush, $glowPath)
                $glowPath.Dispose()
                $haloBrush.Dispose()
            }
            $Graphics.FillPolygon($headBrush, $geometry.Head)
            $Graphics.FillPolygon($coreBrush, $geometry.HeadCore)

            foreach ($pen in @($keyPen, $glowWide, $glowMid, $glowTight, $bodyPen, $corePen)) { $pen.Dispose() }
            $headBrush.Dispose()
            $coreBrush.Dispose()
            $geometry.Shaft.Dispose()

            if ($ShowOrder) {
                $key = "$([int]$tile.row),$([int]$tile.column)"
                if ($Order.ContainsKey($key)) {
                    $label = [string]$Order[$key]
                    $measured = $Graphics.MeasureString($label, $numberFont)
                    $lx = [float]($geometry.HeadPos.X - $measured.Width - 3)
                    $ly = [float]($geometry.HeadPos.Y - ($cell * 0.42))
                    $Graphics.FillRectangle($labelBg, $lx - 3, $ly - 1, $measured.Width + 4, $measured.Height + 2)
                    $Graphics.DrawString($label, $numberFont, $labelBrush, $lx, $ly)
                }
            }
        }
    }
    finally {
        $numberFont.Dispose()
        $labelBrush.Dispose()
        $labelBg.Dispose()
    }
}

function Show-Level {
    param([int]$Index)

    if ($script:Levels.Count -eq 0) { return }
    $script:Index = [Math]::Max(0, [Math]::Min($Index, $script:Levels.Count - 1))
    $script:Current = $script:Levels[$script:Index]
    $script:Solve = Solve-Level -Level $script:Current

    $level = $script:Current
    $result = if ($script:Solve.Solvable) { "Solvable ($($script:Solve.Steps) moves)" } else { "UNSOLVABLE" }
    $script:Status.Text = "$($level.id)   |   $($level.width)x$($level.height)   |   " +
        "$($level.tiles.Count) tiles   |   par $($level.parMoves)   |   $result"
    $script:Status.ForeColor = if ($script:Solve.Solvable) {
        [System.Drawing.Color]::FromArgb(17, 60, 71)
    } else {
        [System.Drawing.Color]::FromArgb(200, 67, 31)
    }
    $script:List.SelectedIndex = $script:Index
    $script:Panel.Invalidate()
}

function Initialize-Viewer {
    [System.Windows.Forms.Application]::EnableVisualStyles()

    $form = New-Object System.Windows.Forms.Form
    $form.Text = "Arrow Puzzle - Level Viewer"
    $form.ClientSize = New-Object System.Drawing.Size(980, 680)
    $form.StartPosition = 'CenterScreen'
    $form.MinimumSize = New-Object System.Drawing.Size(720, 520)
    $form.KeyPreview = $true

    $list = New-Object System.Windows.Forms.ListBox
    $list.Location = New-Object System.Drawing.Point(12, 12)
    $list.Size = New-Object System.Drawing.Size(230, 520)
    $list.Anchor = 'Top,Bottom,Left'
    $list.Font = New-Object System.Drawing.Font('Consolas', 10)
    $list.IntegralHeight = $false

    $previous = New-Object System.Windows.Forms.Button
    $previous.Text = "Prev"
    $previous.Location = New-Object System.Drawing.Point(12, 542)
    $previous.Size = New-Object System.Drawing.Size(110, 32)
    $previous.Anchor = 'Bottom,Left'

    $next = New-Object System.Windows.Forms.Button
    $next.Text = "Next"
    $next.Location = New-Object System.Drawing.Point(132, 542)
    $next.Size = New-Object System.Drawing.Size(110, 32)
    $next.Anchor = 'Bottom,Left'

    $orderToggle = New-Object System.Windows.Forms.CheckBox
    $orderToggle.Text = "Show removal order"
    $orderToggle.Location = New-Object System.Drawing.Point(264, 546)
    $orderToggle.Size = New-Object System.Drawing.Size(200, 24)
    $orderToggle.Anchor = 'Bottom,Left'

    $panel = New-Object System.Windows.Forms.Panel
    $panel.Location = New-Object System.Drawing.Point(254, 12)
    $panel.Size = New-Object System.Drawing.Size(714, 562)
    $panel.Anchor = 'Top,Bottom,Left,Right'
    $panel.BackColor = $stageVoid

    $status = New-Object System.Windows.Forms.Label
    $status.Location = New-Object System.Drawing.Point(12, 584)
    $status.Size = New-Object System.Drawing.Size(956, 84)
    $status.Anchor = 'Bottom,Left,Right'
    $status.Font = New-Object System.Drawing.Font('Consolas', 11)
    $status.Text = "Loading..."
    $status.ForeColor = [System.Drawing.Color]::FromArgb(17, 60, 71)

    $form.Controls.AddRange(@($list, $previous, $next, $orderToggle, $panel, $status))

    $script:Form = $form
    $script:List = $list
    $script:Panel = $panel
    $script:Status = $status

    $panel.Add_Paint({
        param($sender, $e)
        Render-Board -Graphics $e.Graphics -Level $script:Current `
            -Width $sender.ClientSize.Width -Height $sender.ClientSize.Height `
            -ShowOrder $script:ShowOrder -Order $script:Solve.Order
    })

    $list.Add_SelectedIndexChanged({
        if ($script:List.SelectedIndex -ge 0 -and $script:List.SelectedIndex -ne $script:Index) {
            Show-Level -Index $script:List.SelectedIndex
        }
    })
    $previous.Add_Click({ Show-Level -Index ($script:Index - 1) })
    $next.Add_Click({ Show-Level -Index ($script:Index + 1) })
    $orderToggle.Add_CheckedChanged({
        $script:ShowOrder = $orderToggle.Checked
        $script:Panel.Invalidate()
    })
    $form.Add_KeyDown({
        param($sender, $e)
        if ($e.KeyCode -eq [System.Windows.Forms.Keys]::Right -or $e.KeyCode -eq [System.Windows.Forms.Keys]::Down) {
            Show-Level -Index ($script:Index + 1)
            $e.Handled = $true
        } elseif ($e.KeyCode -eq [System.Windows.Forms.Keys]::Left -or $e.KeyCode -eq [System.Windows.Forms.Keys]::Up) {
            Show-Level -Index ($script:Index - 1)
            $e.Handled = $true
        }
    })

    return $form
}

$script:Levels = Load-Levels

if ($Dump) {
    foreach ($level in $script:Levels) {
        $result = Solve-Level -Level $level
        $verdict = if ($result.Solvable) { "solvable ($($result.Steps) moves)" } else { "UNSOLVABLE" }
        $size = "$($level.width)x$($level.height)"
        "{0,-26} {1,-7} {2,3} tiles  par {3,3}  {4}" -f $level.FileName, $size, $level.tiles.Count, $level.parMoves, $verdict
    }
    exit 0
}

$viewer = Initialize-Viewer
foreach ($level in $script:Levels) {
    [void]$script:List.Items.Add("$($level.id)  ($($level.width)x$($level.height))")
}

if ($ExportTo) {
    if (-not (Test-Path -LiteralPath $ExportTo)) {
        New-Item -ItemType Directory -Force -Path $ExportTo | Out-Null
    }
    $exportDir = (Resolve-Path -LiteralPath $ExportTo).Path
    $bitmap = New-Object System.Drawing.Bitmap(900, 600)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        foreach ($level in $script:Levels) {
            $solution = Solve-Level -Level $level
            Render-Board -Graphics $graphics -Level $level -Width 900 -Height 600 `
                -ShowOrder $true -Order $solution.Order
            $target = Join-Path $exportDir "$($level.id).png"
            $bitmap.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
        }
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
        $viewer.Dispose()
    }
    "Exported $($script:Levels.Count) level images to $exportDir"
    exit 0
}

if ($Smoke) {
    $bitmap = New-Object System.Drawing.Bitmap(900, 600)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        foreach ($level in $script:Levels) {
            $solution = Solve-Level -Level $level
            Render-Board -Graphics $graphics -Level $level -Width 900 -Height 600 `
                -ShowOrder $false -Order $solution.Order
            Render-Board -Graphics $graphics -Level $level -Width 900 -Height 600 `
                -ShowOrder $true -Order $solution.Order
        }
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
        $viewer.Dispose()
    }
    "Rendered $($script:Levels.Count) levels without error."
    exit 0
}

Show-Level -Index 0
[void]$viewer.ShowDialog()
