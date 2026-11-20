package com.foschia.tondo.core

/** Shift states, port of ShiftButton.ShiftState. */
enum class ShiftState { NOT_PRESSED, PRESSED, DOUBLE_PRESSED }

/** Port of KeyboardController.View. */
enum class KeyboardView { MAIN, EMOJI, MIC, MACROS }

/**
 * Interactive region produced while building a frame (immediate-mode style): the scene builder
 * draws an element and registers what happens when it is touched. Input is routed to the regions
 * of the last rendered frame.
 */
internal sealed class Control {
    abstract val bounds: Rect
    open fun hit(p: Vec) = p in bounds

    /** A tondo (round letter button). */
    class Tondo(val position: StickPosition, val center: Vec) : Control() {
        override val bounds = Rect.centered(center, Dimensions.TONDO_SIZE)
    }

    /** The cursor joystick in the action bar. */
    class Directional(val center: Vec) : Control() {
        override val bounds = Rect.centered(center, Dimensions.BUTTON_SIZE + 20f)
    }

    /** Port of KeyboardButton: tap on release (or on press), optional long press. */
    class Button(
        val id: String,
        override val bounds: Rect,
        val onTap: () -> Unit,
        val onLongPress: (() -> Unit)? = null,
        val longPressMillis: Long = 500,
        val triggerOnDown: Boolean = false,
        /** Inside a scrollable list: a vertical drag scrolls instead of tapping. */
        val scroller: Scroller? = null,
        /** Repeat while pressed (backspace). */
        val repeating: Boolean = false,
        val haptic: Boolean = true,
    ) : Control()
}

/** Vertical scroll state with a bit of inertia, used by the emoji grid and the pinned notes. */
internal class Scroller {
    var offset = 0f
    var contentHeight = 0f
    var viewportHeight = 0f
    var velocity = 0f
    private var animFrom = 0f
    private var animTo = 0f
    private var animStart = -1L
    private val animDuration = 200L

    val maxOffset get() = (contentHeight - viewportHeight).coerceAtLeast(0f)

    fun dragBy(dy: Float) {
        animStart = -1
        offset = (offset - dy).coerceIn(0f, maxOffset)
    }

    fun scrollTo(target: Float, now: Long) {
        animFrom = offset
        animTo = target.coerceIn(0f, maxOffset)
        animStart = now
        velocity = 0f
    }

    /** @return true while still moving. */
    fun tick(now: Long, dtMillis: Long): Boolean {
        if (animStart >= 0) {
            val t = ((now - animStart).toFloat() / animDuration).coerceIn(0f, 1f)
            offset = MathUtils.lerp(animFrom, animTo, t)
            if (t >= 1f) animStart = -1
            return true
        }
        if (velocity != 0f) {
            offset = (offset - velocity * dtMillis).coerceIn(0f, maxOffset)
            velocity *= 0.92f
            if (kotlin.math.abs(velocity) < 0.02f || offset <= 0f || offset >= maxOffset) velocity = 0f
            return true
        }
        return false
    }
}
