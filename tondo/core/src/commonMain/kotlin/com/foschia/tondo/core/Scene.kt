package com.foschia.tondo.core

/**
 * The renderer-agnostic description of one frame. The engine produces a flat list of primitive
 * draw commands in design units; every platform has a tiny painter (Android Canvas, iOS
 * CoreGraphics, Java2D for previews) that executes them after scaling by [Scene.scale].
 *
 * Keeping the platform painters this small is what makes the keyboard look identical everywhere
 * while staying far below the memory budget of an iOS keyboard extension.
 */
class Scene(
    /** View pixels per design unit. */
    val scale: Float,
    val widthUnits: Float,
    val heightUnits: Float,
    val background: Color,
    val ops: List<DrawOp>,
) {
    companion object {
        /**
         * Text sizes in [DrawOp.Text] are TextMeshPro font sizes from the Unity prefab; painters
         * multiply them by this factor to get the em size in design units.
         */
        const val FONT_SCALE = 0.75f

        /** Font family of the original design (TextMeshPro "Arial" asset). */
        const val FONT_FAMILY = "Arial"
    }
}

/** Angles are in degrees, measured clockwise from the positive x axis (screen convention, y down). */
sealed class DrawOp {
    data class FillRect(val rect: Rect, val color: Color) : DrawOp()

    /** Rounded rect; corners can be selectively squared (used by the action bar). */
    data class FillRoundRect(
        val rect: Rect,
        val radius: Float,
        val color: Color,
        val roundTop: Boolean = true,
        val roundBottom: Boolean = true,
    ) : DrawOp()

    data class FillCircle(val center: Vec, val radius: Float, val color: Color) : DrawOp()

    /** Ring sector (annulus slice). Used for the diacriticizer "niche". */
    data class FillSector(
        val center: Vec,
        val innerRadius: Float,
        val outerRadius: Float,
        val startAngle: Float,
        val sweepAngle: Float,
        val color: Color,
    ) : DrawOp()

    data class StrokeCircle(val center: Vec, val radius: Float, val width: Float, val color: Color) : DrawOp()

    /** Line with round caps (needles). */
    data class Capsule(val from: Vec, val to: Vec, val width: Float, val color: Color) : DrawOp()

    /** Soft blurred drop shadow of a circle. */
    data class CircleShadow(val center: Vec, val radius: Float, val blur: Float, val color: Color) : DrawOp()

    /** Soft blurred drop shadow of a rounded rect. */
    data class RoundRectShadow(val rect: Rect, val radius: Float, val blur: Float, val color: Color) : DrawOp()

    data class Text(
        val text: String,
        /** Center of the text box for CENTER alignment, left/vertical center for START. */
        val anchor: Vec,
        val size: Float,
        val color: Color,
        val bold: Boolean = false,
        val align: TextAlign = TextAlign.CENTER,
        /** When > 0 text is wrapped/ellipsized inside this width (START alignment). */
        val maxWidth: Float = 0f,
        val maxLines: Int = 1,
    ) : DrawOp()

    /** A monochrome icon (white alpha mask) tinted with [tint]. [name] is the file name without extension. */
    data class Icon(val name: String, val rect: Rect, val tint: Color, val flipY: Boolean = false) : DrawOp()

    /**
     * A full color bitmap, e.g. a Noto emoji ("emoji/emoji_u1f600"). When [name] does not exist
     * the painter tries [fallback] (skin-toned emojis without a neutral variant, like EmojiSprite.cs).
     */
    data class Image(val name: String, val rect: Rect, val alpha: Float = 1f, val fallback: String? = null) : DrawOp()

    data class PushClip(val rect: Rect) : DrawOp()
    data object PopClip : DrawOp()
}

enum class TextAlign { CENTER, START }
