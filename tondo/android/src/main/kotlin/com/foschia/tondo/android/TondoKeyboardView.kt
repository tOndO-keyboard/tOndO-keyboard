package com.foschia.tondo.android

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import com.foschia.tondo.core.KeyboardEngine
import com.foschia.tondo.core.TouchAction
import com.foschia.tondo.core.TouchEvent

/**
 * The keyboard view used by the input method service: forwards touches to the shared
 * [KeyboardEngine] and paints the scene it produces. Replaces the UnityPlayer view.
 */
@SuppressLint("ViewConstructor")
class TondoKeyboardView(context: Context, val engine: KeyboardEngine) : View(context) {
    private val painter = CanvasPainter(context.assets)
    private var animating = false

    init {
        isHapticFeedbackEnabled = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        engine.setViewSize(w.toFloat(), h.toFloat())
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        engine.setViewSize(width.toFloat(), height.toFloat())
        animating = engine.tick(SystemClock.uptimeMillis())
        painter.paint(canvas, engine.render())
        if (animating) postInvalidateOnAnimation()
    }

    /** Called by the host when the engine requests a new frame. */
    fun requestRender() {
        if (!animating) postInvalidateOnAnimation()
    }

    fun trimMemory() = painter.trimMemory()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val time = event.eventTime
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = event.actionIndex
                engine.onTouch(TouchEvent(event.getPointerId(i), TouchAction.DOWN, event.getX(i), event.getY(i), time))
            }
            MotionEvent.ACTION_MOVE -> for (i in 0 until event.pointerCount) {
                engine.onTouch(TouchEvent(event.getPointerId(i), TouchAction.MOVE, event.getX(i), event.getY(i), time))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val i = event.actionIndex
                engine.onTouch(TouchEvent(event.getPointerId(i), TouchAction.UP, event.getX(i), event.getY(i), time))
            }
            MotionEvent.ACTION_CANCEL -> for (i in 0 until event.pointerCount) {
                engine.onTouch(TouchEvent(event.getPointerId(i), TouchAction.CANCEL, event.getX(i), event.getY(i), time))
            }
        }
        postInvalidateOnAnimation()
        return true
    }
}
