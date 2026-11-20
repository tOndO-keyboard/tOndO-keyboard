package com.foschia.tondo.core

/**
 * Geometry of the keyboard, in design units, extracted from Unity/Assets/Prefabs/Keyboard.prefab.
 * Unity anchors positions to the left/center/right of a 1080x583 area with y going up; here
 * everything is converted to a y-down coordinate system relative to the area top-left corner.
 */
object Dimensions {
    const val REFERENCE_WIDTH = 1080f
    const val TOP_BAR_HEIGHT = 137f
    const val BOTTOM_HEIGHT = 583f
    const val HIDDEN_TOP_HEIGHT = 10f

    /** CanvasScalerManager proportions (width / height). */
    const val DEFAULT_PROPORTION = 1.5f
    const val HIDDEN_TOP_BAR_PROPORTION = 1.82f
    const val LANDSCAPE_PROPORTION = 2.1f

    const val TONDO_SIZE = 280f
    const val TONDO_SCALE = 0.995f
    const val CENTER_BUTTON_SIZE = 116f
    const val GLYPH_RADIUS = 100f
    const val GLYPH_FONT_SIZE = 50f
    const val GLYPH_HIGHLIGHT_SIZE = 70f
    const val NEEDLE_WIDTH = 80f
    const val BUTTON_SIZE = 116f
    const val ICON_SIZE = 100f

    const val BAR_INSET = 17f
    const val BAR_RADIUS = 51f
    const val BAR_PADDING = 30f
    const val BAR_SPACING = 20f
    const val BAR_SPACING_LANDSCAPE = 87f

    const val EMOJI_SIZE = 90f
    const val EMOJI_SPACING = 25f
    const val EMOJI_PADDING = 16f
    const val EMOJI_GROUP_SPACING = 50f
    const val EMOJI_CATEGORY_BAR = 130f
}

private enum class HAnchor { LEFT, CENTER, RIGHT }

/** A Unity anchored position: x offset from the anchor, y offset (Unity y-up) from the area vertical center. */
private class Anchored(val h: HAnchor, val x: Float, val yUp: Float) {
    fun resolve(width: Float, top: Float): Vec {
        val baseX = when (h) {
            HAnchor.LEFT -> 0f
            HAnchor.CENTER -> width / 2f
            HAnchor.RIGHT -> width
        }
        return Vec(baseX + x, top + Dimensions.BOTTOM_HEIGHT / 2f - yUp)
    }
}

/** Positions of the main area elements for portrait and landscape (the "... 2" objects of the prefab). */
class BottomLayout(val width: Float, val top: Float, val landscape: Boolean) {
    private fun p(portrait: Anchored, land: Anchored = portrait) = (if (landscape) land else portrait).resolve(width, top)

    fun tondo(pos: StickPosition): Vec = when (pos) {
        StickPosition.TOP_LEFT -> p(Anchored(HAnchor.LEFT, 313f, 130f))
        StickPosition.TOP_CENTER -> p(Anchored(HAnchor.CENTER, 76f, 130f), Anchored(HAnchor.RIGHT, -464f, 130f))
        StickPosition.TOP_RIGHT -> p(Anchored(HAnchor.RIGHT, -162f, 130f))
        StickPosition.BOTTOM_LEFT -> p(Anchored(HAnchor.LEFT, 162f, -131f))
        StickPosition.BOTTOM_CENTER -> p(Anchored(HAnchor.CENTER, -75.5f, -131f), Anchored(HAnchor.RIGHT, -615.5f, -131f))
        StickPosition.BOTTOM_RIGHT -> p(Anchored(HAnchor.RIGHT, -313f, -131f))
    }

    val shift get() = p(Anchored(HAnchor.LEFT, 90f, 210f), Anchored(HAnchor.RIGHT, -680f, 210f))
    val symbols get() = p(Anchored(HAnchor.LEFT, 90f, 76f), Anchored(HAnchor.RIGHT, -680f, 76f))
    val backspace get() = p(Anchored(HAnchor.RIGHT, -90f, -82f), Anchored(HAnchor.LEFT, 680f, -82f))
    val done get() = p(Anchored(HAnchor.RIGHT, -90f, -216f), Anchored(HAnchor.LEFT, 680f, -216f))

    /** Hide/unhide arrow: top-right when the action bar is on top, bottom-left when it went bottom. */
    fun hideArrow(barOnBottom: Boolean) =
        if (barOnBottom) Anchored(HAnchor.LEFT, 32f, -262.5f).resolve(width, top) else Anchored(HAnchor.RIGHT, -32f, 262.5f).resolve(width, top)
}
