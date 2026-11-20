package com.foschia.tondo.android

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.LruCache
import com.foschia.tondo.core.DrawOp
import com.foschia.tondo.core.Rect
import com.foschia.tondo.core.Scene
import com.foschia.tondo.core.TextAlign

/**
 * Paints a [Scene] produced by the shared engine on an Android [Canvas].
 * This is the only Android specific rendering code: ~150 lines replacing the Unity player.
 */
class CanvasPainter(private val assets: AssetManager) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private val path = Path()

    /** Optional custom font: drop a file in tondo/assets/fonts/tondo.ttf to use it. */
    private val typeface: Typeface = runCatching { Typeface.createFromAsset(assets, "fonts/tondo.ttf") }.getOrNull()
        ?: Typeface.create("sans-serif", Typeface.NORMAL)
    private val boldTypeface: Typeface = Typeface.create(typeface, Typeface.BOLD)

    /** Decoded bitmaps, bounded to ~12 MB. Emojis are decoded lazily, only when visible. */
    private val bitmaps = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val missing = HashSet<String>()

    private fun bitmap(name: String): Bitmap? {
        bitmaps.get(name)?.let { return it }
        if (name in missing) return null
        val path = if (name.startsWith("emoji/")) "$name.png" else "icons/$name.png"
        val bmp = runCatching { assets.open(path).use { BitmapFactory.decodeStream(it) } }.getOrNull()
        if (bmp == null) missing += name else bitmaps.put(name, bmp)
        return bmp
    }

    fun trimMemory() = bitmaps.evictAll()

    private fun RectF.set(r: Rect) = set(r.left, r.top, r.right, r.bottom)

    fun paint(canvas: Canvas, scene: Scene) {
        canvas.save()
        canvas.scale(scene.scale, scene.scale)
        for (op in scene.ops) when (op) {
            is DrawOp.FillRect -> {
                fill.shader = null; fill.color = op.color.toArgb()
                canvas.drawRect(op.rect.left, op.rect.top, op.rect.right, op.rect.bottom, fill)
            }
            is DrawOp.FillRoundRect -> {
                fill.shader = null; fill.color = op.color.toArgb()
                val r = op.radius
                val radii = floatArrayOf(
                    if (op.roundTop) r else 0f, if (op.roundTop) r else 0f, if (op.roundTop) r else 0f, if (op.roundTop) r else 0f,
                    if (op.roundBottom) r else 0f, if (op.roundBottom) r else 0f, if (op.roundBottom) r else 0f, if (op.roundBottom) r else 0f,
                )
                rectF.set(op.rect)
                path.reset(); path.addRoundRect(rectF, radii, Path.Direction.CW)
                canvas.drawPath(path, fill)
            }
            is DrawOp.FillCircle -> {
                fill.shader = null; fill.color = op.color.toArgb()
                canvas.drawCircle(op.center.x, op.center.y, op.radius, fill)
            }
            is DrawOp.StrokeCircle -> {
                stroke.color = op.color.toArgb(); stroke.strokeWidth = op.width
                canvas.drawCircle(op.center.x, op.center.y, op.radius, stroke)
            }
            is DrawOp.FillSector -> {
                fill.shader = null; fill.color = op.color.toArgb()
                val o = op.outerRadius
                val i = op.innerRadius
                path.reset()
                rectF.set(op.center.x - o, op.center.y - o, op.center.x + o, op.center.y + o)
                path.arcTo(rectF, op.startAngle, op.sweepAngle, true)
                if (i > 0f) {
                    rectF.set(op.center.x - i, op.center.y - i, op.center.x + i, op.center.y + i)
                    path.arcTo(rectF, op.startAngle + op.sweepAngle, -op.sweepAngle, false)
                } else {
                    path.lineTo(op.center.x, op.center.y)
                }
                path.close()
                canvas.drawPath(path, fill)
            }
            is DrawOp.Capsule -> {
                stroke.color = op.color.toArgb(); stroke.strokeWidth = op.width
                canvas.drawLine(op.from.x, op.from.y, op.to.x, op.to.y, stroke)
            }
            is DrawOp.CircleShadow -> {
                // Radial gradient instead of BlurMaskFilter: works with hardware acceleration on every API level.
                val rr = op.radius + op.blur
                val c0 = op.color.toArgb()
                val c1 = c0 and 0x00FFFFFF
                val f = ((op.radius - op.blur) / rr).coerceIn(0.01f, 0.98f)
                fill.shader = RadialGradient(op.center.x, op.center.y, rr, intArrayOf(c0, c0, c1), floatArrayOf(0f, f, 1f), Shader.TileMode.CLAMP)
                canvas.drawCircle(op.center.x, op.center.y, rr, fill)
                fill.shader = null
            }
            is DrawOp.RoundRectShadow -> {
                fill.shader = null
                val base = op.color
                val steps = 8
                for (s in steps downTo 1) {
                    val d = op.blur * s / steps
                    fill.color = base.withAlpha(base.a / steps).toArgb()
                    val r = op.rect.inset(-d + op.blur / 2f)
                    rectF.set(r)
                    canvas.drawRoundRect(rectF, op.radius + d, op.radius + d, fill)
                }
            }
            is DrawOp.Text -> drawText(canvas, op)
            is DrawOp.Icon -> bitmap(op.name)?.let { bmp ->
                bitmapPaint.alpha = 255
                bitmapPaint.colorFilter = PorterDuffColorFilter(op.tint.toArgb(), PorterDuff.Mode.MULTIPLY)
                rectF.set(op.rect)
                if (op.flipY) {
                    canvas.save()
                    canvas.scale(1f, -1f, rectF.centerX(), rectF.centerY())
                    canvas.drawBitmap(bmp, null, rectF, bitmapPaint)
                    canvas.restore()
                } else {
                    canvas.drawBitmap(bmp, null, rectF, bitmapPaint)
                }
                bitmapPaint.colorFilter = null
            }
            is DrawOp.Image -> (bitmap(op.name) ?: op.fallback?.let { bitmap(it) })?.let { bmp ->
                bitmapPaint.colorFilter = null
                bitmapPaint.alpha = (op.alpha * 255).toInt()
                rectF.set(op.rect)
                canvas.drawBitmap(bmp, null, rectF, bitmapPaint)
                bitmapPaint.alpha = 255
            }
            is DrawOp.PushClip -> {
                canvas.save()
                canvas.clipRect(op.rect.left, op.rect.top, op.rect.right, op.rect.bottom)
            }
            DrawOp.PopClip -> canvas.restore()
        }
        canvas.restore()
    }

    private fun drawText(canvas: Canvas, op: DrawOp.Text) {
        textPaint.color = op.color.toArgb()
        textPaint.typeface = if (op.bold) boldTypeface else typeface
        textPaint.textSize = op.size * Scene.FONT_SCALE
        if (op.maxWidth <= 0f || op.align == TextAlign.CENTER && op.maxLines <= 1) {
            textPaint.textAlign = if (op.align == TextAlign.CENTER) Paint.Align.CENTER else Paint.Align.LEFT
            val fm = textPaint.fontMetrics
            val text = if (op.maxWidth > 0f) TextUtils.ellipsize(op.text, textPaint, op.maxWidth, TextUtils.TruncateAt.END).toString() else op.text
            canvas.drawText(text, op.anchor.x, op.anchor.y - (fm.ascent + fm.descent) / 2f, textPaint)
            return
        }
        textPaint.textAlign = Paint.Align.LEFT
        val width = op.maxWidth.toInt().coerceAtLeast(1)
        @Suppress("DEPRECATION")
        val layout = if (android.os.Build.VERSION.SDK_INT >= 23) {
            StaticLayout.Builder.obtain(op.text, 0, op.text.length, textPaint, width)
                .setAlignment(if (op.align == TextAlign.CENTER) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(op.maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
        } else {
            StaticLayout(op.text, textPaint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
        }
        val x = if (op.align == TextAlign.CENTER) op.anchor.x - width / 2f else op.anchor.x
        canvas.save()
        canvas.translate(x, op.anchor.y - layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()
    }
}
