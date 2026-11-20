package com.foschia.tondo.core

import com.foschia.tondo.core.DrawOp.CircleShadow
import com.foschia.tondo.core.DrawOp.FillCircle
import com.foschia.tondo.core.PaletteElement as P
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Builds the [Scene] of the current frame and, at the same time, the interactive regions
 * (immediate-mode GUI). Visual constants come from Keyboard.prefab.
 */
internal class SceneBuilder(private val e: KeyboardEngine) {
    private val ops = ArrayList<DrawOp>(512)
    private val overlay = ArrayList<DrawOp>()
    private val controls = ArrayList<Control>()
    private val swipes = ArrayList<KeyboardEngine.SwipeArea>()
    private val pal = e.palette
    private val w = e.widthUnits
    private val baseY = max(0f, e.heightUnits - e.contentHeight)

    private fun c(el: P) = pal[el]

    fun build(): Scene {
        val barH = if (e.topBarHidden) Dimensions.HIDDEN_TOP_HEIGHT else Dimensions.TOP_BAR_HEIGHT
        val barTop: Float
        val bottomTop: Float
        if (e.barOnBottom) {
            bottomTop = baseY
            barTop = baseY + Dimensions.BOTTOM_HEIGHT
        } else {
            barTop = baseY
            bottomTop = baseY + barH
        }

        // Background behind the whole keyboard (TopBarPositionController sets the camera color).
        val bg = c(P.KEYBOARD_BACKGROUND)
        ops += DrawOp.FillRect(Rect(0f, 0f, w, e.heightUnits), bg)

        val bottomRect = Rect(0f, bottomTop, w, bottomTop + Dimensions.BOTTOM_HEIGHT)
        when (e.view) {
            KeyboardView.MAIN -> drawMain(bottomRect)
            KeyboardView.EMOJI -> drawEmoji(bottomRect)
            KeyboardView.MIC -> drawMic(bottomRect)
            KeyboardView.MACROS -> drawMacros(bottomRect)
        }

        if (e.topBarHidden) {
            ops += DrawOp.FillRect(Rect(0f, barTop, w, barTop + barH), c(P.ACTION_BAR_BACKGROUND))
        } else {
            drawTopBar(Rect(0f, barTop, w, barTop + barH))
        }

        // Swipe up/down anywhere to show/hide the action bar ("Swipe to Hide").
        val whole = Rect(0f, baseY, w, baseY + e.contentHeight)
        swipes += KeyboardEngine.SwipeArea(whole, KeyboardEngine.SwipeDirection.DOWN, 0, 200f, 500, true) { e.setTopBarHidden(true) }
        swipes += KeyboardEngine.SwipeArea(whole, KeyboardEngine.SwipeDirection.UP, 0, 100f, 400, true) { e.setTopBarHidden(false) }

        drawToast()
        ops += overlay
        e.registerFrame(controls, swipes)
        return Scene(e.scale, w, e.heightUnits, bg, ops)
    }

    // region primitives
    private fun shadowSmall(center: Vec, dy: Float = 10f) {
        ops += CircleShadow(center + Vec(0f, dy), 54f, 12f, c(P.SHADOW))
    }

    private fun circleButton(
        center: Vec,
        bg: Color,
        icon: String?,
        iconTint: Color,
        size: Float = Dimensions.BUTTON_SIZE,
        iconSize: Float = Dimensions.ICON_SIZE,
        shadow: Boolean = true,
        shadowDy: Float = 10f,
        pressed: Boolean = false,
    ) {
        if (shadow) shadowSmall(center, shadowDy)
        ops += FillCircle(center, size / 2f, if (pressed) bg.lerp(c(P.SHADOW).withAlpha(1f), 0.12f) else bg)
        if (icon != null) ops += DrawOp.Icon(icon, Rect.centered(center, iconSize), iconTint)
    }

    private fun button(id: String, rect: Rect, onTap: () -> Unit, onLongPress: (() -> Unit)? = null, triggerOnDown: Boolean = false, repeating: Boolean = false, scroller: Scroller? = null, longPressMillis: Long = 500) {
        controls += Control.Button(id, rect, onTap, onLongPress, longPressMillis, triggerOnDown, scroller, repeating)
    }

    private fun isPressed(id: String) = id in e.pressedButtons
    // endregion

    // region main view
    private fun drawMain(area: Rect) {
        val layout = BottomLayout(w, area.top, e.landscape)
        val letters = e.layoutType == LayoutType.MAIN

        for (pos in StickPosition.entries) drawTondo(pos, layout.tondo(pos))

        // Shift / symbols column, joined by a pill like in the original design.
        val shiftC = layout.shift
        val symC = layout.symbols
        val pill = Rect(shiftC.x - 58f, shiftC.y, shiftC.x + 58f, symC.y)
        ops += DrawOp.FillRect(pill, c(P.PERMANENT_BUTTON_BACKGROUND))
        if (letters) {
            val icon = if (e.shiftState == ShiftState.DOUBLE_PRESSED) "shift double press" else "shift"
            val tint = if (e.shiftState == ShiftState.NOT_PRESSED) c(P.PERMANENT_BUTTON_ICON) else c(P.GLYPH_SELECTED)
            circleButton(shiftC, c(P.PERMANENT_BUTTON_BACKGROUND), icon, tint, pressed = isPressed("shift"))
            button("shift", Rect.centered(shiftC, Dimensions.BUTTON_SIZE), { e.onShiftTap() }, triggerOnDown = false)
        } else {
            circleButton(shiftC, c(P.PERMANENT_BUTTON_BACKGROUND), "symbols2", c(if (e.layoutType == LayoutType.SYMBOLS_2) P.GLYPH_SELECTED else P.PERMANENT_BUTTON_ICON), pressed = isPressed("symbols2"))
            button("symbols2", Rect.centered(shiftC, Dimensions.BUTTON_SIZE), { e.toggleAlternateSymbols() })
        }
        circleButton(symC, c(P.PERMANENT_BUTTON_BACKGROUND), if (letters) "symbols" else "ABC", c(P.PERMANENT_BUTTON_ICON), shadowDy = 15f, pressed = isPressed("symbols"))
        button("symbols", Rect.centered(symC, Dimensions.BUTTON_SIZE), { e.onSymbolsTap() })
        val column = Rect(shiftC.x - 58f, shiftC.y - 58f, shiftC.x + 58f, symC.y + 58f)
        swipes += KeyboardEngine.SwipeArea(column, KeyboardEngine.SwipeDirection.RIGHT, if (letters) 1 else 2, 200f, 500, false) {
            if (letters) e.onShiftTap() else e.toggleAlternateSymbols()
        }
        swipes += KeyboardEngine.SwipeArea(column, KeyboardEngine.SwipeDirection.DOWN, 1, 200f, 500, false) { e.onSymbolsTap() }

        drawBackspaceDone(layout.backspace, layout.done)
        drawHideArrow(layout.hideArrow(e.barOnBottom))
    }

    private fun drawBackspaceDone(bsC: Vec, doneC: Vec) {
        ops += DrawOp.FillRect(Rect(bsC.x - 58f, bsC.y, bsC.x + 58f, doneC.y), c(P.PERMANENT_BUTTON_BACKGROUND))
        circleButton(bsC, c(P.PERMANENT_BUTTON_BACKGROUND), "back", c(P.PERMANENT_BUTTON_ICON), pressed = isPressed("backspace"))
        button("backspace", Rect.centered(bsC, Dimensions.BUTTON_SIZE), { e.commitBackspace() }, repeating = true)
        val doneIcon = when (e.doneAction) {
            DoneAction.DONE -> "done_done"
            DoneAction.NEWLINE -> "done_newline"
            DoneAction.SEARCH -> "done_search"
            DoneAction.SEND -> "done_send"
        }
        circleButton(doneC, c(P.DONE_BACKGROUND), doneIcon, c(P.DONE_ICON), shadowDy = 15f, pressed = isPressed("done"))
        button("done", Rect.centered(doneC, Dimensions.BUTTON_SIZE), { e.commitDone() })
        val column = Rect(bsC.x - 58f, bsC.y - 58f, bsC.x + 58f, doneC.y + 58f)
        swipes += KeyboardEngine.SwipeArea(column, KeyboardEngine.SwipeDirection.LEFT, 1, 200f, 500, false) { e.deleteLastWord() }
        swipes += KeyboardEngine.SwipeArea(column, KeyboardEngine.SwipeDirection.DOWN, 1, 100f, 400, false) { e.commitDone() }
    }

    private fun drawHideArrow(center: Vec) {
        val hidden = e.topBarHidden
        // "Hide" shows the arrow flipped (pointing towards the bar), "Unhide" the plain one.
        val flip = if (e.barOnBottom) hidden else !hidden
        val rect = Rect.centered(center, 64f * 1.5f, 58f * 1.5f)
        ops += DrawOp.Icon("hide", rect, c(P.GLYPH_CONSONANT), flipY = flip)
        button("hide", rect.inset(-10f), { e.setTopBarHidden(!hidden) })
    }

    private fun drawTondo(pos: StickPosition, center: Vec) {
        val s = Dimensions.TONDO_SCALE
        val r = Dimensions.TONDO_SIZE / 2f * s
        val touch = e.tondoTouches[pos]
        val offset = e.stickOffsets[pos] ?: Vec.ZERO
        val cc = center + offset
        val mode = e.settings.inputMode

        ops += CircleShadow(center + Vec(0f, 17.5f), r * 0.93f, 18f, c(P.SHADOW))
        ops += FillCircle(center, r, c(P.TONDOS_CAVITY))
        ops += FillCircle(cc, r * 1.006f, c(P.TONDOS_EXTERIOR))

        val diacritic = diacriticizerDirection(pos)
        if (diacritic != null) {
            // Indent shadow (shadow_indent.png): a 60° niche from the center button to the rim.
            val start = if (pos == StickPosition.TOP_LEFT) -150f else -90f
            ops += DrawOp.FillSector(cc, 58f * s, r * 1.006f, start, 60f, c(P.SHADOW).let { it.withAlpha(it.a * 0.231f) })
        }

        // Needles.
        if (touch != null && touch.distance > 0f && mode != InputMode.JOYSTICK) {
            val rad = touch.angle * KeyboardEngine.DEG2RAD
            val dir = Vec(cos(rad), -sin(rad))
            val col = c(P.GLYPH_SELECTED_BACKGROUND)
            if (mode == InputMode.NEEDLE) {
                val len = min(touch.distance, Dimensions.TONDO_SIZE * 0.5f)
                ops += DrawOp.Capsule(cc, cc + dir * len, 56f, col)
            } else {
                val len = min(touch.distance * 2f, Dimensions.TONDO_SIZE) / 2f
                ops += DrawOp.Capsule(cc - dir * len, cc + dir * len, 56f, col)
            }
            ops += FillCircle(cc, Dimensions.NEEDLE_WIDTH / 2f, col)
        }

        ops += CircleShadow(cc + Vec(0f, 10f), 54f, 12f, c(P.SHADOW))
        ops += FillCircle(cc, Dimensions.CENTER_BUTTON_SIZE / 2f, c(P.TONDOS_INTERIOR))

        val showPopup = e.settings.showPopupPreview
        val needleSticky = e.settings.sticky && mode != InputMode.JOYSTICK
        for (dir in listOf(CardinalDirection.CENTER) + TONDO_VALID_DIRECTIONS) {
            val gp = cc + dir.glyphOffset(Dimensions.GLYPH_RADIUS)
            val isDiacritic = dir == diacritic
            val temp = touch?.takeIf { it.alternatesShown && !isDiacritic }?.temporaryLabels?.get(dir)
            val raw = when {
                temp != null -> temp
                isDiacritic -> e.diacriticLabel(pos == StickPosition.TOP_LEFT)
                else -> e.glyphLabel(pos, dir)
            }
            val label = if (raw == " ") "␣" else raw
            val highlighted = touch != null && touch.selected == dir
            val baseColor = when {
                temp != null -> c(P.GLYPH_CONSONANT)
                dir == CardinalDirection.CENTER -> c(P.GLYPH_VOWEL)
                isDiacritic -> c(P.GLYPH_SYMBOL)
                label.length == 1 && label[0].isLetterOrDigit() -> c(P.GLYPH_CONSONANT)
                else -> c(P.GLYPH_SYMBOL)
            }
            if (highlighted && !showPopup && !needleSticky) {
                ops += FillCircle(gp, Dimensions.GLYPH_HIGHLIGHT_SIZE / 2f, c(P.GLYPH_SELECTED_BACKGROUND))
            }
            if (label.isNotEmpty()) {
                ops += DrawOp.Text(label, gp, Dimensions.GLYPH_FONT_SIZE, if (highlighted) c(P.GLYPH_SELECTED) else baseColor, bold = label == "␣")
            }
            if (highlighted && showPopup && label.isNotEmpty()) drawPreviewPopup(pos, gp, label)
        }
        controls += Control.Tondo(pos, center)
    }

    private fun drawPreviewPopup(pos: StickPosition, glyph: Vec, label: String) {
        val topRow = pos == StickPosition.TOP_LEFT || pos == StickPosition.TOP_CENTER || pos == StickPosition.TOP_RIGHT
        val color = c(P.TONDOS_INTERIOR)
        if (topRow && (e.topBarHidden || e.barOnBottom)) {
            val k = 0.96f
            val bgRect = Rect(glyph.x - 49f * k, glyph.y - 98f * k + 15f * k, glyph.x + 49f * k, glyph.y + 15f * k)
            overlay += DrawOp.Icon("preview popup small bg", bgRect.inset(-16f * k), color)
            overlay += DrawOp.Text(label, Vec(glyph.x, bgRect.top + 10f * k + 32.5f * k), Dimensions.GLYPH_FONT_SIZE, c(P.GLYPH_CONSONANT))
        } else {
            val k = 0.97f
            val bottom = glyph.y + 15f * k
            val bgRect = Rect(glyph.x - 71.5f * k, bottom - 215f * k, glyph.x + 71.5f * k, bottom)
            overlay += DrawOp.Icon("preview popup bg", bgRect, color)
            val popupTop = glyph.y - 188f * k
            overlay += DrawOp.Text(label, Vec(glyph.x, popupTop + 30f * k + 32.5f * k), Dimensions.GLYPH_FONT_SIZE, c(P.GLYPH_CONSONANT))
        }
    }
    // endregion

    // region action bar
    private fun drawTopBar(area: Rect) {
        val onTop = !e.barOnBottom
        val bar = Rect(Dimensions.BAR_INSET, area.top, w - Dimensions.BAR_INSET, area.bottom)
        val shadowDy = if (onTop) 7.5f else -7.5f
        ops += DrawOp.RoundRectShadow(bar.offset(0f, shadowDy), Dimensions.BAR_RADIUS, 14f, c(P.SHADOW))
        ops += DrawOp.FillRect(Rect(0f, area.top, w, area.bottom), c(P.KEYBOARD_BACKGROUND))
        ops += DrawOp.FillRoundRect(bar, Dimensions.BAR_RADIUS, c(P.ACTION_BAR_BACKGROUND), roundTop = !onTop, roundBottom = onTop)

        val cy = area.top + area.height / 2f
        when {
            e.suggestionsMode -> drawSuggestions(area, cy)
            e.controlMode -> drawButtonsRow(cy, ctrlButtons())
            e.view == KeyboardView.EMOJI -> drawButtonsRow(cy, skinToneButtons())
            else -> drawButtonsRow(cy, mainButtons())
        }
    }

    private class BarButton(
        val id: String,
        val draw: (Vec) -> Unit,
        val onTap: () -> Unit,
        val onLongPress: (() -> Unit)? = null,
        val directional: Boolean = false,
    )

    private fun simple(id: String, icon: String, tint: Color = pal[P.ACTION_BAR_BUTTON_ICON], lock: Boolean = false, onTap: () -> Unit) =
        BarButton(id, { center ->
            circleButton(center, c(P.ACTION_BAR_BUTTON_BACKGROUND), icon, tint, iconSize = Dimensions.BUTTON_SIZE, pressed = isPressed(id))
            if (lock) ops += DrawOp.Icon("lock", Rect.centered(center + Vec(20f, -20f), 40f), c(P.ACTION_BAR_BUTTON_ICON))
        }, onTap)

    private fun mainButtons(): List<BarButton> = listOfNotNull(
        simple("ctrl", "ctrl") { e.toggleControlMode() },
        simple("settings", "settings") { e.host.openSettings() },
        simple("chooser", "keys") { e.host.openKeyboardChooser() },
        BarButton("directional", { center -> drawDirectional(center) }, {}, directional = true),
        simple("macros", if (e.view == KeyboardView.MACROS) "ABC" else "macro") { e.toggleView(KeyboardView.MACROS) },
        if (e.host.isSpeechRecognitionAvailable()) simple("mic", if (e.view == KeyboardView.MIC) "ABC" else "microphone") { e.toggleView(KeyboardView.MIC) } else null,
        simple("emoji", "emote") { e.toggleView(KeyboardView.EMOJI) },
    )

    private fun ctrlButtons(): List<BarButton> = listOf(
        simple("ctrl", "ctrl", c(P.GLYPH_SELECTED)) { e.toggleControlMode() },
        simple("copy", "CTRL_copy") { e.host.performCtrl(CtrlAction.COPY_TEXT) },
        simple("paste", "CTRL_paste") { e.host.performCtrl(CtrlAction.PASTE_TEXT) },
        simple("cut", "CTRL_cut") { e.host.performCtrl(CtrlAction.CUT_TEXT) },
        simple("selectall", "CTRL_select", lock = !e.isPro) { if (e.isPro) e.host.performCtrl(CtrlAction.SELECT_ALL) else e.proLockTap() },
        simple("undo", "CTRL_undo") { e.host.performCtrl(CtrlAction.UNDO) },
        simple("redo", "CTRL_redo") { e.host.performCtrl(CtrlAction.REDO) },
    )

    private fun skinToneButtons(): List<BarButton> {
        val tones = listOf(SkinTone.NONE, SkinTone.LIGHT, SkinTone.MEDIUM_LIGHT, SkinTone.MEDIUM, SkinTone.MEDIUM_DARK, SkinTone.DARK)
        return tones.mapIndexed { i, tone ->
            BarButton("tone$i", { center ->
                val t1 = e.emojis.skinTone1 == tone
                val t2 = e.emojis.skinTone2 == tone
                circleButton(center, c(P.ACTION_BAR_BUTTON_BACKGROUND), null, Color.WHITE, pressed = isPressed("tone$i"))
                val selected = t1 || t2
                val size = Dimensions.BUTTON_SIZE * if (selected) 1f else 0.8f
                val rect = Rect.centered(center, size)
                // SelectedTonesSplitVisualizer: upper half shows tone 1, lower half tone 2.
                ops += DrawOp.Image("emoji/emoji_skin_tone_icon_${i + 1}", rect)
                val bgCol = c(P.ACTION_BAR_BUTTON_BACKGROUND)
                if (t1 && !t2) ops += DrawOp.FillSector(center, 0f, size / 2f + 1f, -45f, 180f, bgCol)
                if (t2 && !t1) ops += DrawOp.FillSector(center, 0f, size / 2f + 1f, 135f, 180f, bgCol)
            }, onTap = {
                e.emojis.skinTone1 = tone
                e.emojis.skinTone2 = tone
                e.host.requestRender()
            }, onLongPress = {
                // SkinToneButton.OnLongPressTrigger: None affects both, the others set the second tone.
                if (tone == SkinTone.NONE) {
                    e.emojis.skinTone1 = tone; e.emojis.skinTone2 = tone
                } else if (e.emojis.skinTone1 == SkinTone.NONE || e.emojis.skinTone2 == SkinTone.NONE) {
                    e.emojis.skinTone1 = tone; e.emojis.skinTone2 = tone
                } else {
                    e.emojis.skinTone2 = tone
                }
                e.host.requestRender()
            })
        } + simple("emoji", "ABC") { e.toggleView(KeyboardView.EMOJI) }
    }

    private fun drawButtonsRow(cy: Float, buttons: List<BarButton>) {
        val n = buttons.size
        val size = Dimensions.BUTTON_SIZE
        val centers: List<Float> = if (!e.landscape) {
            // HorizontalLayoutGroup with childForceExpandWidth (portrait).
            val pad = Dimensions.BAR_PADDING
            val sp = Dimensions.BAR_SPACING
            val cell = (w - 2 * pad - sp * (n - 1)) / n
            List(n) { pad + cell / 2f + it * (cell + sp) }
        } else {
            val sp = Dimensions.BAR_SPACING_LANDSCAPE
            val total = n * size + (n - 1) * sp
            val start = w - Dimensions.BAR_PADDING - total
            List(n) { start + size / 2f + it * (size + sp) }
        }
        buttons.forEachIndexed { i, b ->
            val center = Vec(centers[i], cy)
            b.draw(center)
            if (b.directional) controls += Control.Directional(center)
            else button(b.id, Rect.centered(center, size + 10f), b.onTap, b.onLongPress)
        }
    }

    private fun drawDirectional(center: Vec) {
        ops += FillCircle(center, Dimensions.BUTTON_SIZE / 2f, c(P.ACTION_BAR_BUTTON_BACKGROUND))
        val cc = center + e.directionalOffset
        val s = Dimensions.BUTTON_SIZE * 1.006f
        shadowSmall(cc)
        ops += FillCircle(cc, s / 2f, c(P.ACTION_BAR_DIRECTIONAL_BACKGROUND))
        ops += DrawOp.Icon("move", Rect.centered(cc, s), c(P.ACTION_BAR_DIRECTIONAL_ICON))
    }

    private fun drawSuggestions(area: Rect, cy: Float) {
        val backC = Vec(Dimensions.BAR_PADDING + 40f, cy)
        circleButton(backC, c(P.PERMANENT_BUTTON_BACKGROUND), null, Color.WHITE, size = 80f)
        ops += DrawOp.Icon("back", Rect.centered(backC, 100f), c(P.PERMANENT_BUTTON_ICON))
        button("suggest_back", Rect.centered(backC, 112f), {
            e.suggestions = emptyList()
            e.showSuggestions(false)
        })
        val left = Dimensions.BAR_PADDING + 80f + 5f
        val slotW = (w - left - Dimensions.BAR_PADDING) / 3f
        for (i in 0 until 3) {
            val s = e.suggestions.getOrNull(i) ?: continue
            val x0 = left + i * slotW
            if (i > 0) ops += DrawOp.FillRect(Rect(x0 - 1f, cy - 56f, x0 + 1f, cy + 56f), c(P.PERMANENT_BUTTON_BACKGROUND))
            ops += DrawOp.Text(s, Vec(x0 + slotW / 2f, cy), 50f, c(P.PERMANENT_BUTTON_BACKGROUND))
            button("suggestion$i", Rect(x0, area.top, x0 + slotW, area.bottom), {
                e.host.commitCorrection(s)
                e.suggestions = emptyList()
                e.showSuggestions(false)
            })
        }
    }
    // endregion

    // region emoji view
    private fun drawEmoji(area: Rect) {
        val gridRect = Rect(0f, area.top, w, area.bottom - Dimensions.EMOJI_CATEGORY_BAR)
        val pitch = Dimensions.EMOJI_SIZE + Dimensions.EMOJI_SPACING
        val pad = Dimensions.EMOJI_PADDING
        val cols = max(1, floor((w - 2 * pad + Dimensions.EMOJI_SPACING) / pitch).toInt())
        val gx = (w - (cols * pitch - Dimensions.EMOJI_SPACING)) / 2f

        val recentSlots = if (e.isPro) EmojiData.RECENT_SLOTS_PRO else EmojiData.RECENT_SLOTS_FREE + 1
        e.emojis.capacity = EmojiData.RECENT_SLOTS_PRO
        val recent = e.emojis.descendingKeys().take(if (e.isPro) EmojiData.RECENT_SLOTS_PRO else EmojiData.RECENT_SLOTS_FREE)
        class Group(val icon: String, val codes: List<String?>)
        val groups = ArrayList<Group>()
        val recentCodes: List<String?> = if (!e.isPro && e.emojis.count > EmojiData.RECENT_SLOTS_FREE) recent + listOf(null) else recent
        groups += Group(EmojiData.RECENT_ICON, recentCodes.take(recentSlots))
        EmojiData.categories.forEach { groups += Group(it.icon, it.codes) }

        val scroller = e.emojiScroller
        val groupTops = ArrayList<Float>()
        var y = pad
        for (g in groups) {
            groupTops += y
            val rows = (g.codes.size + cols - 1) / cols
            y += max(rows, if (g === groups[0]) 1 else 0) * pitch - Dimensions.EMOJI_SPACING + Dimensions.EMOJI_GROUP_SPACING
        }
        scroller.contentHeight = y
        scroller.viewportHeight = gridRect.height

        ops += DrawOp.PushClip(gridRect)
        val top = gridRect.top - scroller.offset
        for ((gi, g) in groups.withIndex()) {
            val gTop = top + groupTops[gi]
            g.codes.forEachIndexed { i, code ->
                val cy = gTop + (i / cols) * pitch + Dimensions.EMOJI_SIZE / 2f
                if (cy < gridRect.top - pitch || cy > gridRect.bottom + pitch) return@forEachIndexed
                val cx = gx + (i % cols) * pitch + Dimensions.EMOJI_SIZE / 2f
                val rect = Rect.centered(Vec(cx, cy), Dimensions.EMOJI_SIZE)
                if (code == null) {
                    // ProLock slot (OverrideClick): lock icon, toast then redirect to settings.
                    ops += DrawOp.Icon("lock", Rect.centered(rect.center, 45f), c(P.GLYPH_SYMBOL))
                    button("emoji_lock", rect, { e.proLockTap() }, scroller = scroller)
                } else {
                    val (primary, fallback) = Emoji.imageNames(code)
                    ops += DrawOp.Image(primary, rect, alpha = if (isPressed("emoji:$gi:$i")) 0.6f else 1f, fallback = fallback)
                    if (rect.bottom > gridRect.top && rect.top < gridRect.bottom) {
                        button("emoji:$gi:$i", rect, { e.commitEmoji(code) }, scroller = scroller)
                    }
                }
            }
        }
        ops += DrawOp.PopClip
        // The whole grid scrolls, even between cells.
        button("emoji_grid", gridRect, {}, scroller = scroller)
        moveGridBelowCells()

        // Category bar.
        val barTop = area.bottom - Dimensions.EMOJI_CATEGORY_BAR
        ops += DrawOp.FillRect(Rect(0f, barTop, w, area.bottom), c(P.KEYBOARD_BACKGROUND))
        val catW = min(955f, w - 125f) / groups.size
        val current = groupTops.indexOfLast { it <= scroller.offset + 1f }.coerceAtLeast(0)
        groups.forEachIndexed { i, g ->
            val center = Vec(catW * i + catW / 2f, barTop + 55f)
            val selected = i == current
            ops += DrawOp.Icon(g.icon, Rect.centered(center, 80f), c(P.GLYPH_SELECTED).withAlpha(if (selected) 1f else 0.5f))
            if (selected) ops += DrawOp.FillRoundRect(Rect(center.x - 35f, barTop + 103f, center.x + 35f, barTop + 118f), 7.5f, c(P.GLYPH_SELECTED))
            button("cat$i", Rect(catW * i, barTop, catW * (i + 1), area.bottom), { scroller.scrollTo(groupTops[i], e.now) })
        }
        val bsC = Vec(w - 25f - 50f, area.bottom - 15f - 50f)
        ops += DrawOp.Icon("back", Rect.centered(bsC, 100f), c(P.PERMANENT_BUTTON_ICON))
        button("backspace", Rect.centered(bsC, 110f), { e.commitBackspace() }, repeating = true)
        swipes += KeyboardEngine.SwipeArea(Rect.centered(bsC, 110f), KeyboardEngine.SwipeDirection.LEFT, 1, 200f, 500, false) { e.deleteLastWord() }
    }

    /** Emoji cells are registered before the grid scroll area: move the grid to the bottom of the stack. */
    private fun moveGridBelowCells() {
        val idx = controls.indexOfLast { it is Control.Button && it.id == "emoji_grid" }
        if (idx < 0) return
        val grid = controls.removeAt(idx)
        val firstCell = controls.indexOfFirst { it is Control.Button && (it.id.startsWith("emoji:") || it.id == "emoji_lock") }
        controls.add(if (firstCell < 0) controls.size else firstCell, grid)
    }
    // endregion

    // region notes view (macros)
    private fun drawMacros(area: Rect) {
        val left = 50f
        val iconW = 80f
        val contentL = left + iconW + 20f
        val contentR = w - 50f
        var y = area.top + 30f

        // Clipboard row.
        ops += DrawOp.Icon("clipboard_icon", Rect(left, y + 10f, left + iconW, y + 90f), c(P.GLYPH_CONSONANT))
        val clipRect = Rect(contentL, y, contentR, y + 100f)
        if (e.clipboard.isNotEmpty()) {
            ops += DrawOp.FillRoundRect(clipRect, 30f, c(P.TONDOS_EXTERIOR))
            if (e.clipboardFunctionActive) {
                ops += DrawOp.Icon("pin_icon", Rect.centered(clipRect.center, 100f), c(P.GLYPH_VOWEL))
            } else {
                ops += DrawOp.Text(e.clipboard.trim(), Vec(clipRect.left + 25f, clipRect.center.y), 40f, c(P.GLYPH_VOWEL), align = TextAlign.START, maxWidth = clipRect.width - 50f, maxLines = 1)
            }
            button("clipboard", clipRect, { e.onClipboardTap() }, onLongPress = {
                e.clipboardFunctionActive = !e.clipboardFunctionActive
                e.functionActiveMacro = null
                e.host.requestRender()
            })
        }
        y += 150f

        // Pinned notes, two per row (PinButtonsContainer), scrollable.
        ops += DrawOp.Icon("pin_icon", Rect(left, y + 10f, left + iconW, y + 90f), c(P.GLYPH_CONSONANT))
        val listRect = Rect(contentL, y, contentR, area.bottom - 20f)
        val macros = e.visibleMacros()
        val scroller = e.macrosScroller
        val colW = (listRect.width - 20f) / 2f
        val rows = macros.chunked(2)
        val rowHeights = rows.map { r -> if (r.any { e.macros.isTutorial(it) }) 300f else 100f }
        scroller.viewportHeight = listRect.height
        scroller.contentHeight = rowHeights.sum() + 20f * max(0, rows.size - 1)
        ops += DrawOp.PushClip(listRect)
        var ry = listRect.top - scroller.offset
        rows.forEachIndexed { ri, row ->
            val h = rowHeights[ri]
            row.forEachIndexed { ci, macro ->
                val x0 = listRect.left + ci * (colW + 20f)
                val rect = Rect(x0, ry, x0 + colW, ry + h)
                ops += DrawOp.FillRoundRect(rect, 30f, c(P.TONDOS_EXTERIOR))
                if (e.functionActiveMacro == macro) {
                    ops += DrawOp.Icon("MACRO_delete", Rect.centered(rect.center, 100f), c(P.GLYPH_VOWEL))
                } else {
                    val lines = max(1, ((h - 30f) / 42f).toInt())
                    ops += DrawOp.Text(macro.trim(), Vec(rect.left + 25f, rect.center.y), 35f, c(P.GLYPH_VOWEL), align = TextAlign.START, maxWidth = rect.width - 50f, maxLines = lines)
                }
                if (rect.bottom > listRect.top && rect.top < listRect.bottom) {
                    button("macro:$ri:$ci", rect, { e.onMacroTap(macro) }, onLongPress = {
                        e.functionActiveMacro = if (e.functionActiveMacro == macro) null else macro
                        e.clipboardFunctionActive = false
                        e.host.requestRender()
                    }, scroller = scroller)
                }
            }
            ry += h + 20f
        }
        ops += DrawOp.PopClip
    }
    // endregion

    // region mic view
    private fun drawMic(area: Rect) {
        val center = area.center
        val teal = c(P.ACTION_BAR_BACKGROUND)
        for (ring in e.micRings) {
            val t = (e.now - ring.start).toFloat()
            val size = 300f + ring.speed * t / 1000f * 10f
            val alpha = (1f - t / ring.fade).coerceIn(0f, 1f)
            ops += DrawOp.StrokeCircle(center, size / 2f, 8f, teal.withAlpha(alpha))
        }
        ops += FillCircle(center, 150f, teal)
        if (e.listeningToSpeech) {
            ops += DrawOp.Icon("microphone", Rect.centered(center, 300f), c(P.TONDOS_INTERIOR))
        } else {
            // SinusoidalIcon: three bouncing dots while waiting for the recognizer.
            val omegaT = 0.25f * (e.now / 1000f) * 2f * PI.toFloat() * 4f
            for (i in 0 until 3) {
                val dy = 10f * sin(omegaT - 0.1f * i * 10f)
                ops += FillCircle(center + Vec(-80f + 80f * i, dy), 20f, c(P.TONDOS_INTERIOR))
            }
        }
        button("mic_center", Rect.centered(center, 300f), { e.toggleView(KeyboardView.MIC) })
    }
    // endregion

    private fun drawToast() {
        val text = e.toastText ?: return
        val t = (e.now - e.toastStart).toFloat()
        val alpha = when {
            t < 100f -> t / 100f
            t > KeyboardEngine.TOAST_DURATION - 100f -> ((KeyboardEngine.TOAST_DURATION - t) / 100f).coerceAtLeast(0f)
            else -> 1f
        }
        val rect = Rect.centered(Vec(w / 2f, baseY + e.contentHeight - 150f), 450f, 130f)
        overlay += DrawOp.FillRoundRect(rect, 30f, Color(1f, 1f, 1f, 0.686f * alpha))
        overlay += DrawOp.Text(text, rect.center, 30f, Color(0.2f, 0.2f, 0.2f, alpha), maxWidth = rect.width - 30f, maxLines = 3)
    }
}
