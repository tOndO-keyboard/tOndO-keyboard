package com.foschia.tondo.core

/** Port of PALETTE_ELEMENT.cs. */
enum class PaletteElement {
    GLYPH_SYMBOL, GLYPH_CONSONANT, GLYPH_VOWEL,
    ACTION_BAR_BUTTON_BACKGROUND, ACTION_BAR_BUTTON_ICON, ACTION_BAR_DIRECTIONAL_BACKGROUND, ACTION_BAR_DIRECTIONAL_ICON, ACTION_BAR_BACKGROUND,
    TONDOS_EXTERIOR, TONDOS_INTERIOR, TONDOS_CAVITY,
    PERMANENT_BUTTON_BACKGROUND, PERMANENT_BUTTON_ICON, DONE_BACKGROUND, DONE_ICON, SHIFT_PRESSED_ICON,
    KEYBOARD_BACKGROUND,
    GLYPH_SELECTED, GLYPH_SELECTED_BACKGROUND,
    SHADOW,
}

enum class ColorTheme { LIGHT, DARK }

/** Theme preference, same integer values stored by the Android settings ("ThemeType"). */
enum class ThemeType(val value: Int) {
    FOLLOW_SYSTEM(0), LIGHT(1), DARK(2);

    companion object {
        fun from(v: Int) = entries.firstOrNull { it.value == v } ?: FOLLOW_SYSTEM
    }
}

/**
 * Exact colors taken from Unity/Assets/Data/Themes/{Light,Dark}ThemeColorPaletteDictionary.asset.
 */
class Palette private constructor(private val colors: Map<PaletteElement, Color>) {
    operator fun get(e: PaletteElement): Color = colors.getValue(e)

    companion object {
        private fun c(r: Float, g: Float, b: Float, a: Float = 1f) = Color(r, g, b, a)
        private val TEAL = c(0.38039216f, 0.8117647f, 0.7607843f)
        private val TEAL_DARK = c(0f, 0.69803923f, 0.6627451f)
        private val SELECTED_BG = c(0.78431374f, 0.9254902f, 0.9137255f)

        val LIGHT = Palette(
            mapOf(
                PaletteElement.KEYBOARD_BACKGROUND to c(0.9647059f, 0.972549f, 0.972549f),
                PaletteElement.SHIFT_PRESSED_ICON to TEAL,
                PaletteElement.DONE_ICON to c(0.9647059f, 0.972549f, 0.972549f),
                PaletteElement.DONE_BACKGROUND to TEAL,
                PaletteElement.PERMANENT_BUTTON_ICON to c(0.4f, 0.4f, 0.4f),
                PaletteElement.PERMANENT_BUTTON_BACKGROUND to c(0.90588236f, 0.9254902f, 0.92156863f),
                PaletteElement.TONDOS_CAVITY to TEAL,
                PaletteElement.TONDOS_INTERIOR to c(0.9647059f, 0.972549f, 0.972549f),
                PaletteElement.TONDOS_EXTERIOR to c(0.90588236f, 0.9254902f, 0.92156863f),
                PaletteElement.ACTION_BAR_BACKGROUND to TEAL,
                PaletteElement.ACTION_BAR_DIRECTIONAL_ICON to c(0.90588236f, 0.9254902f, 0.92156863f),
                PaletteElement.ACTION_BAR_DIRECTIONAL_BACKGROUND to c(0.4f, 0.4f, 0.4f),
                PaletteElement.ACTION_BAR_BUTTON_ICON to c(0.4f, 0.4f, 0.4f),
                PaletteElement.ACTION_BAR_BUTTON_BACKGROUND to c(0.90588236f, 0.9254902f, 0.92156863f),
                PaletteElement.GLYPH_VOWEL to c(0.2f, 0.2f, 0.2f),
                PaletteElement.GLYPH_CONSONANT to c(0.4f, 0.4f, 0.4f),
                PaletteElement.GLYPH_SYMBOL to c(0.6f, 0.6f, 0.6f),
                PaletteElement.GLYPH_SELECTED to TEAL,
                PaletteElement.GLYPH_SELECTED_BACKGROUND to SELECTED_BG,
                PaletteElement.SHADOW to c(0.43529412f, 0.5921569f, 0.63529414f, 0.4f),
            ),
        )

        val DARK = Palette(
            mapOf(
                PaletteElement.KEYBOARD_BACKGROUND to c(0.34509805f, 0.34509805f, 0.34509805f),
                PaletteElement.SHIFT_PRESSED_ICON to TEAL_DARK,
                PaletteElement.DONE_ICON to c(0.9647059f, 0.972549f, 0.972549f),
                PaletteElement.DONE_BACKGROUND to TEAL_DARK,
                PaletteElement.PERMANENT_BUTTON_ICON to c(0.7607843f, 0.7607843f, 0.7607843f),
                PaletteElement.PERMANENT_BUTTON_BACKGROUND to c(0.2784314f, 0.2784314f, 0.2784314f),
                PaletteElement.TONDOS_CAVITY to TEAL_DARK,
                PaletteElement.TONDOS_INTERIOR to c(0.34509805f, 0.34509805f, 0.34509805f),
                PaletteElement.TONDOS_EXTERIOR to c(0.2784314f, 0.2784314f, 0.2784314f),
                PaletteElement.ACTION_BAR_BACKGROUND to TEAL_DARK,
                PaletteElement.ACTION_BAR_DIRECTIONAL_ICON to c(0.2784314f, 0.2784314f, 0.2784314f),
                PaletteElement.ACTION_BAR_DIRECTIONAL_BACKGROUND to c(0.7607843f, 0.7607843f, 0.7607843f),
                PaletteElement.ACTION_BAR_BUTTON_ICON to c(0.7607843f, 0.7607843f, 0.7607843f),
                PaletteElement.ACTION_BAR_BUTTON_BACKGROUND to c(0.2784314f, 0.2784314f, 0.2784314f),
                PaletteElement.GLYPH_VOWEL to c(1f, 1f, 1f),
                PaletteElement.GLYPH_CONSONANT to c(0.7607843f, 0.7607843f, 0.7607843f),
                PaletteElement.GLYPH_SYMBOL to c(0.5019608f, 0.5019608f, 0.5019608f),
                PaletteElement.GLYPH_SELECTED to TEAL,
                PaletteElement.GLYPH_SELECTED_BACKGROUND to SELECTED_BG,
                PaletteElement.SHADOW to c(0f, 0f, 0f, 0.5019608f),
            ),
        )

        fun of(theme: ColorTheme) = if (theme == ColorTheme.DARK) DARK else LIGHT
    }
}
