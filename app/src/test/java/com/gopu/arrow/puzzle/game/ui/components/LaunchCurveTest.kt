package com.gopu.arrow.puzzle.game.ui.components

import androidx.compose.ui.geometry.Offset
import com.gopu.arrow.puzzle.game.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Launch effects are drawn by arc length along the flattened spine, so the whole
 * "follows the real geometry" guarantee rests on [CurvePath]. These tests pin it
 * down for the four body shapes the board ships and for all four exit
 * directions, without needing a device.
 */
class LaunchCurveTest {

    private val cell = 60f
    private val gap = 3f
    private val pitch = cell + gap
    private val unit = cell

    /** How far a smoothed point may sit from the drawn polyline, in cell units. */
    private val drift = unit * 0.35f

    private val shapes = mapOf(
        "straight" to listOf(0 to 0, 0 to 1),
        "L" to listOf(1 to 0, 0 to 0, 0 to 1),
        "U" to listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2),
        "zigzag" to listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2, 2 to 2, 2 to 3)
    )

    private val exits = listOf(
        Direction.RIGHT to 0,
        Direction.DOWN to 1,
        Direction.LEFT to 2,
        Direction.UP to 3
    )

    @Test
    fun smoothsEveryBendWithoutLeavingTheArrow() {
        for ((name, cells) in shapes) {
            for ((direction, turns) in exits) {
                val polyline = spineOf(cells, turns)
                val curve = CurvePath.of(polyline, unit)

                assertTrue("$name/$direction has no curve", curve.points.size >= polyline.size)
                for (point in curve.points) {
                    val distance = distanceToPolyline(point, polyline)
                    assertTrue(
                        "$name/$direction drifts $distance from its own spine",
                        distance <= drift
                    )
                }
            }
        }
    }

    @Test
    fun keepsTheSmoothingShortButNotShortcutting() {
        for ((name, cells) in shapes) {
            for ((direction, turns) in exits) {
                val polyline = spineOf(cells, turns)
                val straight = polylineLength(polyline)
                val curve = CurvePath.of(polyline, unit)

                assertTrue(
                    "$name/$direction got longer than the body",
                    curve.length <= straight + 0.01f
                )
                assertTrue(
                    "$name/$direction shortcut the body",
                    curve.length > straight * 0.85f
                )
                if (turnCount(polyline) > 0) {
                    assertTrue(
                        "$name/$direction was never rounded",
                        curve.length < straight - 0.01f
                    )
                }
            }
        }
    }

    @Test
    fun reportsOneOrderedCornerPerRealTurn() {
        for ((name, cells) in shapes) {
            for ((direction, turns) in exits) {
                val polyline = spineOf(cells, turns)
                val curve = CurvePath.of(polyline, unit)

                assertEquals(
                    "$name/$direction corner count",
                    turnCount(polyline),
                    curve.corners.size
                )
                var previous = 0f
                for (corner in curve.corners) {
                    assertTrue("$name/$direction corner at $corner", corner > previous)
                    assertTrue("$name/$direction corner at $corner", corner < curve.length)
                    previous = corner
                }
            }
        }
    }

    @Test
    fun cornersSitOnTheCurve() {
        for ((name, cells) in shapes) {
            for ((direction, turns) in exits) {
                val polyline = spineOf(cells, turns)
                val curve = CurvePath.of(polyline, unit)

                for (corner in curve.corners) {
                    val point = curve.pointAt(corner)
                    assertTrue(
                        "$name/$direction corner is off the body",
                        distanceToPolyline(point, polyline) <= drift
                    )
                }
            }
        }
    }

    @Test
    fun arcLengthRunsFromTailToHead() {
        for ((name, cells) in shapes) {
            for ((direction, turns) in exits) {
                val polyline = spineOf(cells, turns)
                val curve = CurvePath.of(polyline, unit)

                assertClose(name, direction, curve.pointAt(0f), polyline.first())
                assertClose(name, direction, curve.pointAt(curve.length), polyline.last())

                var previous = curve.pointAt(0f)
                var step = curve.length / 24f
                while (step <= curve.length) {
                    val point = curve.pointAt(step)
                    // Consecutive samples only ever move by their own spacing:
                    // a jump would mean the effect could skip across a bend.
                    val travelled = (point - previous).getDistance()
                    assertTrue(
                        "$name/$direction jumps $travelled at $step",
                        travelled <= step + unit * 0.12f
                    )
                    previous = point
                    step += curve.length / 24f
                }
            }
        }
    }

    @Test
    fun tailTangentFollowsTheBodyNotTheBoardAxis() {
        for ((name, cells) in shapes) {
            for ((direction, turns) in exits) {
                val polyline = spineOf(cells, turns)
                val curve = CurvePath.of(polyline, unit)

                val back = unitVector(polyline.first() - polyline[1])
                val tangent = curve.directionAt(0f)
                // The head enters the body at the tail, so the tangent it faces
                // there has to run into the arrow, which for a bent arrow is not a
                // board axis.
                assertTrue(
                    "$name/$direction tail does not point into the body",
                    tangent.x * back.x + tangent.y * back.y < -0.999f
                )
            }
        }
    }

    @Test
    fun headTangentFollowsTheFinalSegment() {
        for ((name, cells) in shapes) {
            for ((direction, turns) in exits) {
                val polyline = spineOf(cells, turns)
                val curve = CurvePath.of(polyline, unit)

                val last = unitVector(polyline.last() - polyline[polyline.lastIndex - 1])
                val tangent = curve.directionAt(curve.length)
                assertTrue(
                    "$name/$direction head is off axis",
                    tangent.x * last.x + tangent.y * last.y > 0.999f
                )
            }
        }
    }

    @Test
    fun normalStaysPerpendicularAndUnitLength() {
        for ((name, cells) in shapes) {
            for ((direction, turns) in exits) {
                val polyline = spineOf(cells, turns)
                val curve = CurvePath.of(polyline, unit)

                for (step in 1..8) {
                    val along = curve.length * step / 9f
                    val tangent = curve.directionAt(along)
                    val normal = curve.normalAt(along)
                    assertTrue(
                        "$name/$direction normal is not perpendicular",
                        abs(tangent.x * normal.x + tangent.y * normal.y) < 0.001f
                    )
                    assertClose(name, direction, Offset(normal.getDistance(), 0f), Offset(1f, 0f))
                }
            }
        }
    }

    @Test
    fun aSinglePointSpineIsHarmless() {
        val curve = CurvePath.of(listOf(Offset(12f, 34f)), unit)
        assertEquals(0f, curve.length, 0f)
        assertEquals(0, curve.corners.size)
    }

    @Test
    fun aStraightArrowHasNoCorners() {
        val polyline = spineOf(shapes.getValue("straight"), 0)
        assertEquals(0, CurvePath.of(polyline, unit).corners.size)
    }

    /**
     * The flow is measured only in arc length along the drawn curve, so the
     * fractions below are fractions of the arrow's real body: the tail, the first
     * segment, the bend, the last segment, the arrowhead. This is what guarantees a
     * bent arrow visibly flows through its own bends instead of being slid across
     * the board as a static curved sprite.
     */
    @Test
    fun theFrontWalksTheWholeBodyFromTheTailToTheHead() {
        for ((name, cells) in shapes) {
            for ((direction, turns) in exits) {
                val curve = CurvePath.of(spineOf(cells, turns), unit)

                assertEquals(
                    "$name/$direction front does not start at the tail",
                    0f,
                    pipeFront(0f, curve.length),
                    0f
                )
                assertEquals(
                    "$name/$direction front does not end at the head",
                    curve.length,
                    pipeFront(1f, curve.length),
                    0.001f
                )

                var previous = -1f
                for (step in 0..20) {
                    val front = pipeFront(step / 20f, curve.length)
                    assertTrue("$name/$direction front reversed", front >= previous)
                    assertTrue("$name/$direction front left the body", front <= curve.length)
                    previous = front
                }
            }
        }
    }

    @Test
    fun everyBendIsReachedByTheFrontInOrder() {
        for ((name, cells) in shapes) {
            for ((direction, turns) in exits) {
                val polyline = spineOf(cells, turns)
                val curve = CurvePath.of(polyline, unit)

                assertEquals(
                    "$name/$direction bend count",
                    turnCount(polyline),
                    curve.corners.size
                )

                var previous = 0f
                for (corner in curve.corners) {
                    // The bend is behind the front, and therefore converted to the
                    // thin line, only once the front has actually arrived there.
                    // A corner can never be jumped by the flow crossing the empty
                    // cell inside the bend.
                    val flow = corner / curve.length
                    assertTrue(
                        "$name/$direction bend at $corner converted too early",
                        pipeFront(flow - 0.01f, curve.length) < corner
                    )
                    assertTrue(
                        "$name/$direction bend at $corner converted too late",
                        pipeFront(flow, curve.length) >= corner
                    )
                    // And the whole collapsed line runs from the tail up to it.
                    assertTrue(
                        "$name/$direction line does not start before the tail",
                        trailStartAt(corner, curve.length, 0f) <= 0f
                    )
                    assertTrue("$name/$direction bend order", corner > previous)
                    previous = corner
                }
            }
        }
    }

    /**
     * The collapsed body is the arrow's own path drawn thinner, so the only thing
     * that may change along it is brightness, and only in one direction: wet where
     * the flow just passed, drier behind it, never brighter again further back.
     */
    @Test
    fun theCollapsedLineOnlyEverBrightensTowardTheHead() {
        for ((name, cells) in shapes) {
            for ((direction, turns) in exits) {
                val curve = CurvePath.of(spineOf(cells, turns), unit)

                for (fraction in listOf(0.15f, 0.4f, 0.7f, 1f)) {
                    val front = curve.length * fraction
                    var previous = -1f
                    var step = 0f
                    while (step <= front) {
                        val lit = trailLitAt(front, curve.length, 0f, step)
                        assertTrue(
                            "$name/$direction brightens again at $step",
                            lit >= previous - 0.001f
                        )
                        previous = lit
                        step += front / 20f
                    }
                    // The front itself is the wettest point on the line, and the
                    // far tail never drops out of sight entirely.
                    assertEquals(
                        "$name/$direction front is not the wettest point",
                        1f,
                        trailLitAt(front, curve.length, 0f, front),
                        0.001f
                    )
                    assertTrue(
                        "$name/$direction tail of the line went dark",
                        trailLitAt(front, curve.length, 0f, 0f) > 0.05f
                    )
                }
            }
        }
    }

    @Test
    fun theBodyIsAlwaysNarrowerBehindTheFrontThanAheadOfIt() {
        val tube = unit * 0.088f
        val collapsed = collapsedWidth(tube)
        assertTrue("collapsed body is $collapsed against a $tube tube", collapsed < tube)
        assertTrue("collapsed body is not a visible line", collapsed > 0f)
        // It scales with the arrow rather than being a fixed size, so a big board
        // gets a proportionally thin line.
        assertEquals(collapsed * 2f, collapsedWidth(tube * 2f), 0.0001f)
    }

    /**
     * The head is the one thing the effect turns, and it only ever turns to face
     * the way it is travelling. On a grid-aligned body that is a quarter turn or a
     * half turn and never anything in between, so the head snaps cleanly from
     * segment to segment instead of slewing through a bend.
     */
    @Test
    fun theHeadTurnsInWholeQuarterTurnsAndLandsOnItsOwnAxis() {
        val east = Offset(1f, 0f)
        assertEquals("no turn", 0f, headTurnDegrees(east, east), 0.01f)
        assertEquals("quarter turn", 90f, headTurnDegrees(east, Offset(0f, 1f)), 0.01f)
        assertEquals("negative quarter turn", -90f, headTurnDegrees(east, Offset(0f, -1f)), 0.01f)
        assertEquals("half turn", 180f, abs(headTurnDegrees(east, Offset(-1f, 0f))), 0.01f)

        for ((name, cells) in shapes) {
            for ((direction, turns) in exits) {
                val polyline = spineOf(cells, turns)
                val curve = CurvePath.of(polyline, unit)
                val axis = unitVector(polyline.last() - polyline[polyline.lastIndex - 1])

                // At the tip the tangent is the arrow's own final-segment axis, so
                // the head arrives in exactly the orientation it was drawn in.
                assertEquals(
                    "$name/$direction head does not land on its own axis",
                    0f,
                    headTurnDegrees(axis, curve.directionAt(curve.length)),
                    0.01f
                )
                assertTrue(
                    "$name/$direction tail turn is not a whole turn",
                    isQuarterTurn(headTurnDegrees(axis, curve.directionAt(0f)))
                )

                // Wherever the head is, it is on the arrow's own body: riding the
                // path never takes it off into the cell beside a bend.
                var step = 0f
                while (step <= curve.length) {
                    assertTrue(
                        "$name/$direction head left the body at $step",
                        distanceToPolyline(curve.pointAt(step), polyline) <= drift
                    )
                    step += curve.length / 24f
                }
            }
        }
    }

    @Test
    fun theHeadIsReleasedAtTheTailAndFadedOutLast() {
        // Nothing is visible before the flow starts, and the head comes in over
        // the first part of the drain rather than popping into a cell the flow has
        // not reached.
        assertEquals(0f, headLitAt(0f, 0f), 0.001f)
        assertEquals(1f, headLitAt(0.2f, 0f), 0.001f)

        // Leaving the board, the collapsed line is drawn away from the tail forward
        // and is completely gone before the head is.
        for (fraction in listOf(0.25f, 0.5f, 0.75f)) {
            assertTrue(
                "line at $fraction does not start at the tail",
                trailStartAt(0f, 1f, fraction) < 0f
            )
            assertTrue(
                "line at $fraction outran the head",
                trailStartAt(1f, 1f, fraction) < 1f
            )
            assertTrue(
                "head at $fraction is gone before the line",
                headLitAt(1f, fraction) > 0f
            )
        }
        // Fully out: the line has been drawn all the way forward to the head, so
        // nothing is left, and the head goes with it.
        assertEquals(1f, trailStartAt(1f, 1f, 1f), 0.0001f)
        assertEquals(0f, headLitAt(1f, 1f), 0.001f)
    }

    /** True when [degrees] is within a degree of a multiple of a quarter turn. */
    private fun isQuarterTurn(degrees: Float): Boolean {
        val wrapped = abs(degrees) % 90f
        return min(wrapped, 90f - wrapped) < 1f
    }

    /**
     * Builds the same spine `ArrowShape` uses: cell centres, a tail pushed back
     * half a cell along the body's first direction, and the head cell last.
     */
    private fun spineOf(
        cells: List<Pair<Int, Int>>,
        turns: Int
    ): List<Offset> {
        val body = cells.map { (row, column) ->
            Offset(column * pitch + cell / 2f, row * pitch + cell / 2f)
        }
        val rotated = rotate(body, turns)
        val first = rotated.first()
        val second = rotated[1]
        val out = ArrayList<Offset>(rotated.size + 1)
        val tail = cell * 1.5f
        out += Offset(
            x = first.x + (first.x - second.x) / (second - first).getDistance() * tail,
            y = first.y + (first.y - second.y) / (second - first).getDistance() * tail
        )
        out += rotated
        return out
    }

    /** Quarter turns clockwise on screen, so RIGHT becomes DOWN becomes LEFT. */
    private fun rotate(
        points: List<Offset>,
        turns: Int
    ): List<Offset> {
        var current = points
        repeat((turns % 4 + 4) % 4) {
            current = current.map { Offset(x = -it.y, y = it.x) }
        }
        return current
    }

    private fun unitVector(vector: Offset): Offset {
        val length = vector.getDistance()
        return if (length < 0.0001f) Offset(1f, 0f) else vector * (1f / length)
    }

    /** How many times the body actually changes direction. */
    private fun turnCount(polyline: List<Offset>): Int {
        var turns = 0
        for (index in 1 until polyline.lastIndex) {
            val inDir = unitVector(polyline[index] - polyline[index - 1])
            val outDir = unitVector(polyline[index + 1] - polyline[index])
            if (abs(inDir.x * outDir.x + inDir.y * outDir.y) < 0.9f) turns++
        }
        return turns
    }

    private fun polylineLength(points: List<Offset>): Float {
        var total = 0f
        for (index in 1 until points.size) {
            total += (points[index] - points[index - 1]).getDistance()
        }
        return total
    }

    private fun distanceToPolyline(
        point: Offset,
        polyline: List<Offset>
    ): Float {
        var best = Float.MAX_VALUE
        for (index in 1 until polyline.size) {
            val a = polyline[index - 1]
            val b = polyline[index]
            val ab = b - a
            val lengthSquared = ab.x * ab.x + ab.y * ab.y
            val t = if (lengthSquared < 0.0001f) {
                0f
            } else {
                (((point.x - a.x) * ab.x + (point.y - a.y) * ab.y) / lengthSquared)
                    .coerceIn(0f, 1f)
            }
            val closest = Offset(a.x + ab.x * t, a.y + ab.y * t)
            best = minOf(best, (point - closest).getDistance())
        }
        return best
    }

    private fun assertClose(
        name: String,
        direction: Direction,
        actual: Offset,
        expected: Offset
    ) {
        val dx = actual.x - expected.x
        val dy = actual.y - expected.y
        val distance = sqrt(dx * dx + dy * dy)
        assertTrue("$name/$direction is $distance off", distance <= 0.01f)
    }
}