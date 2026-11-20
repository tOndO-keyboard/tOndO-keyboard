package com.foschia.tondo.core

/** Port of CardinalDirection.cs (ordinal values are significant for [CardinalDirectionSorter]). */
enum class CardinalDirection(val value: Int) {
    NULL(-1), CENTER(0), EAST(1), NORTH_EAST(2), NORTH(3), NORTH_WEST(4), WEST(5), SOUTH_WEST(6), SOUTH(7), SOUTH_EAST(8);

    /** Offset of a glyph placed in this direction on a tondo, in design units (y-down). Radius is 100. */
    fun glyphOffset(radius: Float = 100f): Vec = when (this) {
        CENTER, NULL -> Vec.ZERO
        EAST -> Vec(radius, 0f)
        WEST -> Vec(-radius, 0f)
        NORTH -> Vec(0f, -radius)
        SOUTH -> Vec(0f, radius)
        NORTH_EAST -> Vec(radius * 0.5f, -radius * 0.8660254f)
        NORTH_WEST -> Vec(-radius * 0.5f, -radius * 0.8660254f)
        SOUTH_EAST -> Vec(radius * 0.5f, radius * 0.8660254f)
        SOUTH_WEST -> Vec(-radius * 0.5f, radius * 0.8660254f)
    }
}

/** Port of CardinalDirectionSorter. */
class CardinalDirectionSorter(private val starter: CardinalDirection, private val clockwise: Boolean) : Comparator<CardinalDirection> {
    override fun compare(a: CardinalDirection, b: CardinalDirection): Int {
        if (a == b) return 0
        if (a == CardinalDirection.CENTER) return 1
        if (b == CardinalDirection.CENTER) return -1
        if (a == CardinalDirection.NULL) return -1
        if (b == CardinalDirection.NULL) return 1
        val s = starter.value
        var ia = a.value
        var ib = b.value
        val last = CardinalDirection.SOUTH_EAST.value
        return if (!clockwise) {
            ia -= s; if (ia < CardinalDirection.CENTER.value) ia += last + 1
            ib -= s; if (ib < CardinalDirection.CENTER.value) ib += last + 1
            ia.compareTo(ib)
        } else {
            ia += last - s; if (ia > last) ia -= last + 1
            ib += last - s; if (ib > last) ib -= last + 1
            ib.compareTo(ia)
        }
    }
}

/** The six positions of the tondi ("round buttons") on a layout. */
enum class StickPosition { TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT }

/** Main letters, symbols and alternate symbols layouts. */
enum class LayoutType { MAIN, SYMBOLS_1, SYMBOLS_2 }

/** Directions a tondo can select, in the order used for clove computation (validCardinalDirection in the prefab). */
val TONDO_VALID_DIRECTIONS = listOf(
    CardinalDirection.EAST, CardinalDirection.NORTH_EAST, CardinalDirection.NORTH_WEST,
    CardinalDirection.WEST, CardinalDirection.SOUTH_WEST, CardinalDirection.SOUTH_EAST,
)

/** Which glyph of a tondo is a diacriticizer, if any (top-left: NW is "left", top-right: NE is "right"). */
fun diacriticizerDirection(position: StickPosition): CardinalDirection? = when (position) {
    StickPosition.TOP_LEFT -> CardinalDirection.NORTH_WEST
    StickPosition.TOP_RIGHT -> CardinalDirection.NORTH_EAST
    else -> null
}
