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

$directionColors = @{
    UP    = [System.Drawing.Color]::FromArgb(240, 93, 58)
    DOWN  = [System.Drawing.Color]::FromArgb(62, 153, 193)
    LEFT  = [System.Drawing.Color]::FromArgb(244, 189, 79)
    RIGHT = [System.Drawing.Color]::FromArgb(114, 201, 165)
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

function Get-ArrowPoints {
    param([float]$CenterX, [float]$CenterY, [float]$Size, [string]$Direction)

    $shaft = [double]$Size * 0.30
    $head = [double]$Size * 0.22
    $halfHead = $head / 2.0
    $neck = $shaft * 0.35

    # Flat list of x,y pairs. Scalars are precomputed because PowerShell
    # mis-parses arithmetic inside an array literal (the comma binds first).
    $base = @(
        (-$shaft), (-$halfHead),
        $neck, (-$halfHead),
        $neck, (-$head),
        $shaft, [double]0,
        $neck, $head,
        $neck, $halfHead,
        (-$shaft), $halfHead
    )

    $points = New-Object 'System.Collections.Generic.List[System.Drawing.PointF]'
    for ($i = 0; $i -lt $base.Count; $i += 2) {
        $x = [float]$base[$i]
        $y = [float]$base[$i + 1]
        $px = $CenterX
        $py = $CenterY
        switch ($Direction) {
            'RIGHT' { $px = $CenterX + $x; $py = $CenterY + $y }
            'LEFT'  { $px = $CenterX - $x; $py = $CenterY + $y }
            'UP'    { $px = $CenterX + $y; $py = $CenterY - $x }
            'DOWN'  { $px = $CenterX + $y; $py = $CenterY + $x }
        }
        $points.Add([System.Drawing.PointF]::new($px, $py))
    }
    return , $points.ToArray()
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
    $Graphics.Clear([System.Drawing.Color]::FromArgb(243, 245, 236))
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

    $cellBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(228, 233, 222))
    $tileBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
    $whiteBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
    $numberFont = New-Object System.Drawing.Font('Segoe UI', [float]($cell * 0.24), [System.Drawing.FontStyle]::Bold)

    try {
        for ($r = 0; $r -lt $rows; $r++) {
            for ($c = 0; $c -lt $cols; $c++) {
                $x = $offX + ($c * $cell)
                $y = $offY + ($r * $cell)
                $inset = 2
                $path = New-RoundedRectPath -X ($x + $inset) -Y ($y + $inset) `
                    -Width ($cell - (2 * $inset)) -Height ($cell - (2 * $inset)) -Radius ([float]($cell * 0.16))
                $Graphics.FillPath($cellBrush, $path)
                $path.Dispose()
            }
        }

        foreach ($tile in $Level.tiles) {
            $dir = [string]$tile.direction
            $tileBrush.Color = if ($directionColors.ContainsKey($dir)) {
                $directionColors[$dir]
            } else {
                [System.Drawing.Color]::White
            }

            $bodyCells = @(Get-BodyCells $tile)

            # Bent arrows paint every body cell, so each occupied cell reads as
            # part of the same arrow.
            foreach ($bodyCell in $bodyCells) {
                $x = $offX + ([int]$bodyCell.column * $cell)
                $y = $offY + ([int]$bodyCell.row * $cell)
                $inset = 3
                $cellPath = New-RoundedRectPath -X ($x + $inset) -Y ($y + $inset) `
                    -Width ($cell - (2 * $inset)) -Height ($cell - (2 * $inset)) -Radius ([float]($cell * 0.18))
                $Graphics.FillPath($tileBrush, $cellPath)
                $cellPath.Dispose()
            }

            # Arrowhead + removal-order label sit on the head cell.
            $head = $bodyCells[$bodyCells.Count - 1]
            $hx = $offX + ([int]$head.column * $cell)
            $hy = $offY + ([int]$head.row * $cell)
            $cx = $hx + ($cell / 2)
            $cy = $hy + ($cell / 2)
            $arrow = Get-ArrowPoints -CenterX $cx -CenterY $cy -Size ([float]($cell * 0.92)) -Direction $dir
            $Graphics.FillPolygon($whiteBrush, [System.Drawing.PointF[]]$arrow)

            if ($ShowOrder) {
                $r = [int]$tile.row
                $c = [int]$tile.column
                $key = "$r,$c"
                if ($Order.ContainsKey($key)) {
                    $label = [string]$Order[$key]
                    $measured = $Graphics.MeasureString($label, $numberFont)
                    $Graphics.DrawString($label, $numberFont, $whiteBrush,
                        [float]($hx + $cell - $measured.Width - 3), [float]($hy + 1))
                }
            }
        }
    }
    finally {
        $cellBrush.Dispose()
        $tileBrush.Dispose()
        $whiteBrush.Dispose()
        $numberFont.Dispose()
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
    $panel.BackColor = [System.Drawing.Color]::FromArgb(243, 245, 236)

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
