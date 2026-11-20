package com.foschia.tondo.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A point in design units (the keyboard is laid out on a 1080-unit wide canvas, y grows downward). */
data class Vec(val x: Float, val y: Float) {
    operator fun plus(o: Vec) = Vec(x + o.x, y + o.y)
    operator fun minus(o: Vec) = Vec(x - o.x, y - o.y)
    operator fun times(f: Float) = Vec(x * f, y * f)
    fun length() = sqrt(x * x + y * y)
    fun distanceTo(o: Vec) = (this - o).length()

    companion object {
        val ZERO = Vec(0f, 0f)
    }
}

data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    val center get() = Vec((left + right) / 2f, (top + bottom) / 2f)
    operator fun contains(p: Vec) = p.x >= left && p.x <= right && p.y >= top && p.y <= bottom
    fun offset(dx: Float, dy: Float) = Rect(left + dx, top + dy, right + dx, bottom + dy)
    fun inset(d: Float) = Rect(left + d, top + d, right - d, bottom - d)

    companion object {
        fun centered(c: Vec, w: Float, h: Float = w) = Rect(c.x - w / 2f, c.y - h / 2f, c.x + w / 2f, c.y + h / 2f)
    }
}

/** ARGB color packed in a Long-safe Int-like representation. */
data class Color(val r: Float, val g: Float, val b: Float, val a: Float = 1f) {
    fun withAlpha(alpha: Float) = Color(r, g, b, alpha)
    fun lerp(o: Color, t: Float) = Color(r + (o.r - r) * t, g + (o.g - g) * t, b + (o.b - b) * t, a + (o.a - a) * t)

    /** 0xAARRGGBB, handy for Android. */
    fun toArgb(): Int {
        fun c(v: Float) = (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (c(a) shl 24) or (c(r) shl 16) or (c(g) shl 8) or c(b)
    }

    companion object {
        fun rgb(hex: Int, a: Float = 1f) = Color(((hex shr 16) and 0xFF) / 255f, ((hex shr 8) and 0xFF) / 255f, (hex and 0xFF) / 255f, a)
        val TRANSPARENT = Color(0f, 0f, 0f, 0f)
        val WHITE = Color(1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f)
    }
}

/** Port of MathUtils.cs. */
object MathUtils {
    fun closestMultiple(value: Float, divider: Int): Float {
        val sign = if (value < 0) -1 else 1
        val absValue = abs(value)
        var rest = absValue % divider
        if (rest > divider * 0.5f) rest -= divider
        return sign * (absValue - rest)
    }

    fun approximatelyEquals(a: Float, b: Float, threshold: Float) = abs(a - b) <= threshold

    fun squircleAngle(angle: Float, curvature: Float, closestMultiple: Float, threshold: Float): Float {
        val sign = if (angle < closestMultiple) 1f else -1f
        val inner = 1f - abs(((closestMultiple - angle) / threshold).pow(curvature))
        return sign * threshold * inner.pow(1f / curvature) + (closestMultiple - sign * threshold)
    }

    fun elasticLerp(from: Float, to: Float, t: Float): Float {
        val c4 = (2f * PI.toFloat()) / 3f
        val newT = when (t) {
            0f -> 0f
            1f -> 1f
            else -> 2f.pow(-10f * t) * sin((t * 10f - 0.75f) * c4) + 1f
        }
        return lerp(from, to, newT)
    }

    fun lerp(from: Float, to: Float, t: Float) = from + (to - from) * t.coerceIn(0f, 1f)

    /** Angle in degrees [0,360) of the vector going from [from] to [to], in a y-up coordinate system. */
    fun angleDegYUp(from: Vec, to: Vec): Float {
        // Design space is y-down, Unity was y-up: flip y to keep identical angle semantics.
        var a = (atan2(-(to.y - from.y), to.x - from.x) * 180.0 / PI).toFloat()
        if (a < 0) a += 360f
        return a
    }
}
