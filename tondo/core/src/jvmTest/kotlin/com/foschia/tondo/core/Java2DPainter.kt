package com.foschia.tondo.core

import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Font
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Reference painter on Java2D, used to render previews of the shared scene on the JVM.
 * It mirrors what the Android (Canvas) and iOS (CoreGraphics) painters do.
 */
class Java2DPainter(private val assetsDir: File) {
    private val cache = HashMap<String, BufferedImage?>()

    private fun load(name: String): BufferedImage? = cache.getOrPut(name) {
        val f = if (name.startsWith("emoji/")) File(assetsDir, "$name.png") else File(assetsDir, "icons/$name.png")
        if (f.exists()) ImageIO.read(f) else null
    }

    private fun awt(c: Color) = java.awt.Color(c.r.coerceIn(0f, 1f), c.g.coerceIn(0f, 1f), c.b.coerceIn(0f, 1f), c.a.coerceIn(0f, 1f))

    private fun tinted(img: BufferedImage, tint: Color): BufferedImage {
        val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until img.height) for (x in 0 until img.width) {
            val p = img.getRGB(x, y)
            val a = ((p ushr 24) and 0xFF) * tint.a
            val r = ((p shr 16) and 0xFF) * tint.r
            val g = ((p shr 8) and 0xFF) * tint.g
            val b = (p and 0xFF) * tint.b
            out.setRGB(x, y, (a.toInt() shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt())
        }
        return out
    }

    fun paint(scene: Scene, widthPx: Int, heightPx: Int): BufferedImage {
        val img = BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.scale(scene.scale.toDouble(), scene.scale.toDouble())
        val clips = ArrayDeque<java.awt.Shape?>()
        for (op in scene.ops) {
            when (op) {
                is DrawOp.FillRect -> { g.color = awt(op.color); g.fill(Rectangle2D.Float(op.rect.left, op.rect.top, op.rect.width, op.rect.height)) }
                is DrawOp.FillRoundRect -> {
                    g.color = awt(op.color)
                    val r = op.rect
                    val shape = Area(RoundRectangle2D.Float(r.left, r.top, r.width, r.height, op.radius * 2, op.radius * 2))
                    if (!op.roundTop) shape.add(Area(Rectangle2D.Float(r.left, r.top, r.width, r.height / 2)))
                    if (!op.roundBottom) shape.add(Area(Rectangle2D.Float(r.left, r.top + r.height / 2, r.width, r.height / 2)))
                    g.fill(shape)
                }
                is DrawOp.FillCircle -> { g.color = awt(op.color); g.fill(Ellipse2D.Float(op.center.x - op.radius, op.center.y - op.radius, op.radius * 2, op.radius * 2)) }
                is DrawOp.StrokeCircle -> {
                    g.color = awt(op.color); g.stroke = BasicStroke(op.width)
                    g.draw(Ellipse2D.Float(op.center.x - op.radius, op.center.y - op.radius, op.radius * 2, op.radius * 2))
                }
                is DrawOp.FillSector -> {
                    g.color = awt(op.color)
                    val o = op.outerRadius
                    val i = op.innerRadius
                    // Java2D arcs are counter-clockwise: negate the clockwise screen angles.
                    val outer = Area(Arc2D.Float(op.center.x - o, op.center.y - o, o * 2, o * 2, -op.startAngle, -op.sweepAngle, Arc2D.PIE))
                    if (i > 0) outer.subtract(Area(Ellipse2D.Float(op.center.x - i, op.center.y - i, i * 2, i * 2)))
                    g.fill(outer)
                }
                is DrawOp.Capsule -> {
                    g.color = awt(op.color); g.stroke = BasicStroke(op.width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                    g.draw(Line2D.Float(op.from.x, op.from.y, op.to.x, op.to.y))
                }
                is DrawOp.CircleShadow -> {
                    val rr = op.radius + op.blur
                    val c0 = awt(op.color)
                    val c1 = java.awt.Color(c0.red, c0.green, c0.blue, 0)
                    val f = ((op.radius - op.blur) / rr).coerceIn(0.01f, 0.98f)
                    g.paint = RadialGradientPaint(op.center.x, op.center.y, rr, floatArrayOf(0f, f, 1f), arrayOf(c0, c0, c1))
                    g.fill(Ellipse2D.Float(op.center.x - rr, op.center.y - rr, rr * 2, rr * 2))
                }
                is DrawOp.RoundRectShadow -> {
                    val c0 = awt(op.color)
                    val steps = 8
                    for (s in steps downTo 1) {
                        val d = op.blur * s / steps
                        g.color = java.awt.Color(c0.red, c0.green, c0.blue, (c0.alpha / steps.toFloat()).toInt().coerceAtLeast(1))
                        val r = op.rect.inset(-d + op.blur / 2)
                        g.fill(RoundRectangle2D.Float(r.left, r.top, r.width, r.height, (op.radius + d) * 2, (op.radius + d) * 2))
                    }
                }
                is DrawOp.Text -> {
                    g.color = awt(op.color)
                    g.font = Font("DejaVu Sans", if (op.bold) Font.BOLD else Font.PLAIN, 1).deriveFont(op.size * Scene.FONT_SCALE)
                    val fm = g.fontMetrics
                    if (op.align == TextAlign.CENTER) {
                        val tw = fm.stringWidth(op.text)
                        g.drawString(op.text, op.anchor.x - tw / 2f, op.anchor.y + (fm.ascent - fm.descent) / 2f)
                    } else {
                        var t = op.text.replace('\n', ' ')
                        if (op.maxWidth > 0) while (fm.stringWidth(t) > op.maxWidth * op.maxLines && t.length > 1) t = t.dropLast(2) + "…"
                        g.drawString(t, op.anchor.x, op.anchor.y + (fm.ascent - fm.descent) / 2f)
                    }
                }
                is DrawOp.Icon -> load(op.name)?.let { src ->
                    val t = tinted(src, op.tint)
                    val r = op.rect
                    if (op.flipY) g.drawImage(t, r.left.toInt(), r.bottom.toInt(), r.width.toInt(), -r.height.toInt(), null)
                    else g.drawImage(t, r.left.toInt(), r.top.toInt(), r.width.toInt(), r.height.toInt(), null)
                }
                is DrawOp.Image -> (load(op.name) ?: op.fallback?.let { load(it) })?.let { src ->
                    val old = g.composite
                    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, op.alpha)
                    g.drawImage(src, op.rect.left.toInt(), op.rect.top.toInt(), op.rect.width.toInt(), op.rect.height.toInt(), null)
                    g.composite = old
                }
                is DrawOp.PushClip -> { clips.addLast(g.clip); g.clip(Rectangle2D.Float(op.rect.left, op.rect.top, op.rect.width, op.rect.height)) }
                DrawOp.PopClip -> g.clip = clips.removeLastOrNull()
            }
        }
        g.dispose()
        return img
    }
}
