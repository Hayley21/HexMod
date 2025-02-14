package at.petrak.hexcasting.client

import at.petrak.hexcasting.HexMod
import at.petrak.hexcasting.HexUtils
import at.petrak.hexcasting.client.gui.SQRT_3
import at.petrak.hexcasting.hexmath.HexCoord
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.math.Matrix4f
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.level.levelgen.XoroshiroRandomSource
import net.minecraft.world.level.levelgen.synth.PerlinNoise
import net.minecraft.world.phys.Vec2
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.roundToInt
import net.minecraft.util.FastColor.ARGB32 as FC

/**
 * Common draw code
 */
object RenderLib {
    /**
     * Source of perlin noise
     */
    val NOISE = PerlinNoise.create(XoroshiroRandomSource(9001L), listOf(0, 1, 2, 3, 4))

    /**
     * Draw a sequence of linePoints spanning the given points.
     *
     * Please make sure to enable the right asinine shaders; see [GuiSpellcasting][at.petrak.hexcasting.client.gui.GuiSpellcasting]
     */
    @JvmStatic
    @JvmOverloads
    fun drawLineSeq(
        mat: Matrix4f,
        points: List<Vec2>,
        width: Float,
        z: Float,
        tail: Int,
        head: Int,
        animTime: Float? = null,
    ) {
        if (points.size <= 1) return

        val r1 = FC.red(tail).toFloat()
        val g1 = FC.green(tail).toFloat()
        val b1 = FC.blue(tail).toFloat()
        val a = FC.alpha(tail)
        val headSource = if (Screen.hasControlDown() != HexMod.CONFIG.ctrlTogglesOffStrokeOrder.get())
            head
        else
            tail
        val r2 = FC.red(headSource).toFloat()
        val g2 = FC.green(headSource).toFloat()
        val b2 = FC.blue(headSource).toFloat()

        // they spell it wrong at mojang lmao
        val tess = Tesselator.getInstance()
        val buf = tess.builder

        // We use one single TRIANGLE_STRIP
        // in order to connect adjacent segments together and not get the weird hinge effect.
        // There's still some artifacting but this is passable, at least.
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR)

        val n = points.size
        for ((i, pair) in points.zipWithNext().withIndex()) {
            val i = i.toFloat()
            val (p1, p2) = pair
            // https://github.com/not-fl3/macroquad/blob/master/src/shapes.rs#L163
            // GuiComponent::innerFill line 52
            // fedor have useful variable names challenge (99% can't beat)
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            // normal x and y, presumably?
            val nx = -dy
            val ny = dx
            // thickness?
            val tlen = Mth.sqrt(nx * nx + ny * ny) / (width * 0.5f)
            val tx = nx / tlen
            val ty = ny / tlen

            fun color(time: Float): BlockPos =
                BlockPos(Mth.lerp(time, r1, r2).toInt(), Mth.lerp(time, g1, g2).toInt(), Mth.lerp(time, b1, b2).toInt())

            val color1 = color(i / n)
            val color2 = color((i + 1) / n)
            buf.vertex(mat, p1.x + tx, p1.y + ty, z).color(color1.x, color1.y, color1.z, a).endVertex()
            buf.vertex(mat, p1.x - tx, p1.y - ty, z).color(color1.x, color1.y, color1.z, a).endVertex()
            buf.vertex(mat, p2.x + tx, p2.y + ty, z).color(color2.x, color2.y, color2.z, a).endVertex()
            buf.vertex(mat, p2.x - tx, p2.y - ty, z).color(color2.x, color2.y, color2.z, a).endVertex()
        }
        tess.end()

        if (animTime != null) {
            val pointCircuit =
                (animTime * 30f * HexMod.CONFIG.patternPointSpeedMultiplier.get().toFloat()) % (points.size + 10)
            // subtract 1 to avoid the point appearing between the end and start for 1 frame
            if (pointCircuit < points.size - 1) {
                val pointMacro = floor(pointCircuit).toInt()
                val pointMicro = pointCircuit - pointMacro

                val p1 = points[pointMacro]
                val p2 = points[(pointMacro + 1) % points.size]
                val drawPos = Vec2(
                    p1.x + (p2.x - p1.x) * pointMicro,
                    p1.y + (p2.y - p1.y) * pointMicro,
                )
                drawSpot(
                    mat,
                    drawPos,
                    2f,
                    (r1 + 255) / 2f / 255f,
                    (g1 + 255) / 2f / 255f,
                    (b1 + 255) / 2f / 255f,
                    a / 1.2f / 255f
                )
            }
        }
    }

    /**
     * Draw a hex pattern from the given list of non-zappy points (as in, do the *style* of drawing it,
     * you have to do the conversion yourself.)
     */
    @JvmStatic
    fun drawPatternFromPoints(
        mat: Matrix4f,
        points: List<Vec2>,
        drawLast: Boolean,
        tail: Int,
        head: Int,
        animTime: Float? = null
    ) {
        fun dodge(n: Int): Float = n * 0.9f
        fun screen(n: Int): Int = (n + 255) / 2

        fun screenCol(n: Int): Int {
            return FC.color(
                FC.alpha(n),
                screen(FC.red(n)),
                screen(FC.green(n)),
                screen(FC.blue(n)),
            )
        }

        val zappyPts = makeZappy(points, 10f, 2.5f, 0.1f)
        val nodes = if (drawLast) {
            points
        } else {
            points.dropLast(1)
        }
        drawLineSeq(mat, zappyPts, 5f, 0f, tail, head, null)
        drawLineSeq(mat, zappyPts, 2f, 1f, screenCol(tail), screenCol(head), animTime)
        for (node in nodes) {
            drawSpot(
                mat,
                node,
                2f,
                dodge(FC.red(head)) / 255f,
                dodge(FC.green(head)) / 255f,
                dodge(FC.blue(head)) / 255f,
                FC.alpha(head) / 255f
            );
        }
    }

    /**
     * Split up a sequence of linePoints with a lightning effect
     * @param hops: rough number of points to subdivide each segment into
     * @param speed: rate at which the lightning effect should move/shake/etc
     */
    @JvmStatic
    fun makeZappy(points: List<Vec2>, hops: Float, variance: Float, speed: Float): List<Vec2> {
        // Nothing in, nothing out
        if (points.isEmpty()) {
            return emptyList()
        }
        val mc = Minecraft.getInstance()
        val zSeed = (mc.frameTime.toDouble() + mc.level!!.levelData.gameTime) * speed
        // Create our output list of zap points
        val zappyPts = mutableListOf(points[0])
        // For each segment in the original...
        for ((i, pair) in points.zipWithNext().withIndex()) {
            val (src, target) = pair
            // Compute distance-squared to the destination, then scale it down by # of hops
            // to know how long each individual hop should be (squared)
            val hopDistSqr = src.distanceToSqr(target) / (hops * hops)
            // Then take the square root to find the actual hop distance
            val hopDist = Mth.sqrt(hopDistSqr)
            // Compute how big the radius of variance should be
            val maxVariance = hopDist * variance

            var position = src
            var j = 0
            while (position.distanceToSqr(target) > hopDistSqr) {
                // Add the next hop...
                val hop = target.add(position.negated()).normalized().scale(hopDist)
                // as well as some random variance...
                // (We use i, j (segment #, subsegment #) as seeds for the Perlin noise,
                // and zSeed (i.e. time elapsed) to perturb the shape gradually over time)
                val theta = (3 * NOISE.getValue(i.toDouble(), j.toDouble(), zSeed) * HexUtils.TAU).toFloat()
                val r = NOISE.getValue(i.inv().toDouble(), j.toDouble(), zSeed).toFloat() * maxVariance
                val randomHop = Vec2(r * Mth.cos(theta), r * Mth.sin(theta))
                position = position.add(hop).add(randomHop)
                // Then record the new location.
                zappyPts.add(position)
                j += 1
            }
            // Finally, we hit the destination, add that too
            zappyPts.add(target)
        }
        return zappyPts
    }

    /**
     * Draw a little circle, because Minecraft rendering code is a nightmare and doesn't
     * include primitive drawing code...
     */
    @JvmStatic
    fun drawSpot(mat: Matrix4f, point: Vec2, radius: Float, r: Float, g: Float, b: Float, a: Float) {
        val tess = Tesselator.getInstance()
        val buf = tess.builder
        // https://stackoverflow.com/questions/20394727/gl-triangle-strip-vs-gl-triangle-fan
        // Starting point is the center
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR)
        buf.vertex(mat, point.x, point.y, 1f).color(r, g, b, a).endVertex()

        // https://github.com/not-fl3/macroquad/blob/master/src/shapes.rs#L98
        // yes they are gonna be little hexagons fite me
        val fracOfCircle = 6
        // run 0 AND last; this way the circle closes
        for (i in 0..fracOfCircle) {
            val theta = i.toFloat() / fracOfCircle * HexUtils.TAU.toFloat()
            val rx = Mth.cos(theta) * radius + point.x
            val ry = Mth.sin(theta) * radius + point.y
            buf.vertex(mat, rx, ry, 1f).color(r, g, b, a).endVertex()
        }

        tess.end()
    }

    @JvmStatic
    fun coordToPx(coord: HexCoord, size: Float, offset: Vec2) =
        Vec2(
            SQRT_3 * coord.q.toFloat() + SQRT_3 / 2.0f * coord.r.toFloat(),
            1.5f * coord.r.toFloat()
        ).scale(size).add(offset)

    @JvmStatic
    fun pxToCoord(px: Vec2, size: Float, offset: Vec2): HexCoord {
        val offsetted = px.add(offset.negated())
        var qf = (SQRT_3 / 3.0f * offsetted.x - 0.33333f * offsetted.y) / size
        var rf = (0.66666f * offsetted.y) / size

        val q = qf.roundToInt()
        val r = rf.roundToInt()
        qf -= q
        rf -= r
        return if (q.absoluteValue >= r.absoluteValue)
            HexCoord(q + (qf + 0.5f * rf).roundToInt(), r)
        else
            HexCoord(q, r + (rf + 0.5 * qf).roundToInt())
    }
}
