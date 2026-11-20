package com.foschia.tondo.core

/**
 * Persistent key/value storage provided by the platform.
 * Android: the existing EncryptedSharedPreferences ("com.foschia.tondokeyboard.preferences").
 * iOS: UserDefaults of the App Group shared by the container app and the keyboard extension.
 */
interface PreferenceStore {
    fun getString(key: String, defaultValue: String): String
    fun getInt(key: String, defaultValue: Int): Int
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putString(key: String, value: String)
    fun putInt(key: String, value: Int)
    fun putBoolean(key: String, value: Boolean)
    fun contains(key: String): Boolean
}

/** In-memory store, used by tests and previews. */
class MemoryPreferenceStore : PreferenceStore {
    val values = mutableMapOf<String, Any>()
    override fun getString(key: String, defaultValue: String) = values[key] as? String ?: defaultValue
    override fun getInt(key: String, defaultValue: Int) = values[key] as? Int ?: defaultValue
    override fun getBoolean(key: String, defaultValue: Boolean) = values[key] as? Boolean ?: defaultValue
    override fun putString(key: String, value: String) { values[key] = value }
    override fun putInt(key: String, value: Int) { values[key] = value }
    override fun putBoolean(key: String, value: Boolean) { values[key] = value }
    override fun contains(key: String) = key in values
}

/** Same integer values as the Android ListPreference "InputType". */
enum class InputMode(val value: Int) {
    NEEDLE(0), DOUBLE_NEEDLE(1), JOYSTICK(2);

    companion object {
        fun from(v: Int) = entries.firstOrNull { it.value == v } ?: JOYSTICK
    }
}

/**
 * Port of SettingsManager.cs. Keys are unchanged so existing users keep their preferences.
 */
data class TondoSettings(
    val inputMode: InputMode = InputMode.JOYSTICK,
    val themeType: ThemeType = ThemeType.FOLLOW_SYSTEM,
    val reverse: Boolean = false,
    val sticky: Boolean = true,
    val verySticky: Boolean = false,
    val showPopupPreview: Boolean = true,
    val vibrationMillis: Int = 10,
    val externalGlyphSelectionDistanceFactor: Int = 7,
    val topBarWentBottom: Boolean = false,
    val enableSuggestions: Boolean = false,
    val suggestionsLanguage: String = "en_US",
    val disableAutomaticCapitalization: Boolean = false,
    val characterCustomizationString: String = DEFAULT_CHARACTER_CUSTOMIZATION,
    val diacriticsCustomizationString: String = DEFAULT_DIACRITICS_CUSTOMIZATION,
    val topBarHidden: Boolean = false,
) {
    companion object {
        const val KEY_TOP_BAR_HIDDEN = "SettingsManagerPlayerPrefKey_TopBarHidden"

        fun load(store: PreferenceStore): TondoSettings {
            val d = TondoSettings()
            return TondoSettings(
                inputMode = InputMode.from(store.getString("InputType", d.inputMode.value.toString()).toIntOrNull() ?: d.inputMode.value),
                themeType = ThemeType.from(store.getString("ThemeType", d.themeType.value.toString()).toIntOrNull() ?: 0),
                reverse = store.getBoolean("Reverse", d.reverse),
                sticky = store.getBoolean("Sticky", d.sticky),
                verySticky = store.getBoolean("VerySticky", d.verySticky),
                showPopupPreview = store.getBoolean("ShowPopupPreview", d.showPopupPreview),
                vibrationMillis = store.getInt("VibrationMilliseconds", d.vibrationMillis),
                externalGlyphSelectionDistanceFactor = store.getInt("ExternalGlyphSelectionDistanceFactor", d.externalGlyphSelectionDistanceFactor),
                topBarWentBottom = store.getBoolean("TopBarWentBottom", d.topBarWentBottom),
                enableSuggestions = store.getBoolean("EnableSuggestions", d.enableSuggestions),
                suggestionsLanguage = store.getString("SuggestionsLanguage", d.suggestionsLanguage),
                disableAutomaticCapitalization = store.getBoolean("DisableAutomaticCapitalization", d.disableAutomaticCapitalization),
                characterCustomizationString = store.getString("CharacterCustomizationString", d.characterCustomizationString),
                diacriticsCustomizationString = store.getString("DiacriticsCustomizationString", d.diacriticsCustomizationString),
                topBarHidden = store.getBoolean(KEY_TOP_BAR_HIDDEN, d.topBarHidden),
            )
        }
    }
}

const val DEFAULT_CHARACTER_CUSTOMIZATION =
    "EQWRT*\nUYFGH'\"\nIJLP:^\nA@()DS#\n Z,.VCX\nOB!?KMN\n\n" +
        "$/123*\n£4567'\"\n€890:^\n[@()]%#\n ;,.&><\n_~!?-+=\n\n" +
        "•\\|½⅓²\nβαδεγ»«\nπλΣΔρ°\n✓₿{}¥§¶\n ®™¢©≥≤\n×√∞∙÷±≠"

const val DEFAULT_DIACRITICS_CUSTOMIZATION =
    "A áàãāªą àáâäåæ 1 1\nB ḃḅḇƀ ḃḅḇƀ 1 2\nC çćĉčċ çćĉčċ 1 2\nD ðḋḍḏđ ðḋḍḏđ 1 2\nE éèēəęǝ èéêëėɛ 1 1\n" +
        "F 5ƒḟᵮ ƒḟᵮ 1 2\nG 6ġǵĝǧǥ ġǵĝǧǥ 1 2\nH 7ħĥȟ ħĥȟ 1 2\nI íìīĭįĩ ìíîǐïị 1 1\nJ 8ĵǰɟ ĵǰɟ 1 2\nK ḱǩʞ ḱǩʞ 1 2\n" +
        "L 9ĺḷƚłɫ ĺḷƚłɫ 1 2\nM ḿṁṃᵯɯ ḿṁṃᵯɯ 1 2\nN ñńǹňᵰṅ ñńǹňᵰṅ 1 2\nO óòõōǒº òóôöøœ 1 1\nP 0ṕṗƥᵽ ṕṗƥᵽ 1 2\n" +
        "Q ɋʠ ɋʠ 1 2\nR 2ŕřṙɍɹ ŕřṙɍɹ 1 2\nS ßşśṥŝš ßşśṥŝš 1 2\nT 3ṫţŧẗʇ þṫţŧẗʇ 1 2\nU úùũūǔ ùúûüů 1 1\n" +
        "V ṽṿᶌⱴʋʌ ṽṿᶌⱴʋʌ 1 2\nW 1ẃẁŵẘẅ ẃẁŵẘẅ 1 2\nX ẍẋᶍ ẍẋᶍ 1 2\nY 4ẏȳẙỹɏ ýẏȳẙỹɏ 1 2\nZ źẑžżƶᵶ źẑžżƶᵶ 1 2\n" +
        "Ô ő ő 1 1\nÛ ű ű 1 1\n. :&%+- …;!,? 1 1\n1 ¹½⅓¼⅕ ₁⅙⅐⅛⅑⅒ 1 1\n2 ²₂⅔⅖ ²₂⅔⅖ 1 2\n3 ³₃⅗¾⅜ ³₃⅗¾⅜ 1 2\n" +
        "4 ⁴₄⅘ ⁴₄⅘ 1 2\n5 ⁵₅⅝⅚ ⁵₅⅝⅚ 1 2\n6 ⁶₆₆ ⁶₆₆ 1 2\n7 ⁷₇⅞ ⁷₇⅞ 1 2\n8 ⁸₈∞ ⁸₈∞ 1 2\n9 ⁹₉ ⁹₉ 1 2\n0 ⁰₀∅ⁿ ⁰₀∅ⁿ 1 2\n" +
        "# №♯﹟＃†‡ №♯﹟＃†‡ 1 2\n€ £$¢₿¥₽ £$¢₿¥₽ 1 2\n- —_–⁻₋· —_–⁻₋· 1 2\n+ ±×÷⅀⁺₊ ±×÷⅀⁺₊ 1 2\n( [<{«‹⁽₍ [<{«‹⁽₍ 1 2\n" +
        ") ]>}»›⁾₎ ]>}»›⁾₎ 1 2\n* ⁂★†‡●• ⁂★†‡●• 1 2\n\" „“”«» „“”«» 1 2\n' ‘’`‹› ‘’`‹› 1 2\n! ¡‽¿⸘ ¡‽¿⸘ 1 2\n" +
        "? ¿‽¡⸘ ¿‽¡⸘ 1 2\n· ♪♫☆♀♂ ♪♫☆♀♂ 1 2\n• ♪♫☆♀♂ ♪♫☆♀♂ 1 2\n● ♪♫☆♀♂ ♪♫☆♀♂ 1 2\nπ ΩΠμ ΩΠμ 1 2\n¶ §※◊∎ §※◊∎ 1 2\n" +
        "$ ¢₿£€¥¤ ¢₿£€¥¤ 1 2\n£ ₤ƒ₺₹₿¤ ₤ƒ₺₹₿¤ 1 2\n^ ←↑↓→☞ ←↑↓→☞ 1 2\n° ª′″ ª′″ 1 2\n= ∞≠≈~⁼₌ ∞≠≈~⁼₌ 1 2\n" +
        "{ ([<«‹ ([<«‹ 1 2\n} )]>»› )]>»› 1 2\n% ‰℅‱ ‰℅‱ 1 2\n< «≤‹ «≤‹ 1 2\n> »≥› »≥› 1 2\n@ ©®℗™℠ ©®℗™℠ 1 2\n" +
        ", ;.: ;.: 1 2\n/ \\|‖¦ \\|‖¦ 1 2\n: ÷·•● ÷·•● 1 2\n] ({<«‹ ({<«‹ 1 2\n[ ({<«‹ ({<«‹ 1 2\n& @əɛ®™ @əɛ®™ 1 2\n" +
        "~ ≈≠- ≈≠- 1 2\n_ ‒–—― ‒–—― 1 2\n✓ ✔☑✗✘×☒ ✔☑✗✘×☒ 1 2\n✗ ✓✔☑×✘☒ ✓✔☑×✘☒ 1 2\n× ✓✔☑✗✘☒ ✓✔☑✗✘☒ 1 2"
