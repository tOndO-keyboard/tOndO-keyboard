package com.foschia.tondokeyboard;

import android.content.Context;

/** Keyboard settings, loaded from the shared preferences every time the input view starts. */
public class KbSettings
{
	public static final int INPUT_NEEDLE = 0, INPUT_DOUBLE_NEEDLE = 1, INPUT_JOYSTICK = 2;
	public static final int THEME_FOLLOW_SYSTEM = 0, THEME_LIGHT = 1, THEME_DARK = 2;
	public static final String KEY_TOP_BAR_HIDDEN = "TopBarHidden";

	public int inputType = INPUT_JOYSTICK;
	public int themeType = THEME_FOLLOW_SYSTEM;
	public boolean reverse = false;
	public boolean sticky = true;
	public boolean verySticky = false;
	public boolean showPopupPreview = true;
	public int vibrationMillis = 10;
	public int externalGlyphSelectionDistanceFactor = 7;
	public boolean topBarWentBottom = false;
	public boolean topBarHidden = false;
	public boolean enableSuggestions = false;
	public String suggestionsLanguage = "en_US";
	public boolean disableAutomaticCapitalization = false;
	public String characterCustomization;
	public String diacriticsCustomization;

	public void load(Context context)
	{
		NativeInterface ni = NativeInterface.getInstance();
		inputType = parseInt(ni.PreferencesGetString("InputType", String.valueOf(INPUT_JOYSTICK)), INPUT_JOYSTICK);
		themeType = parseInt(ni.PreferencesGetString("ThemeType", String.valueOf(THEME_FOLLOW_SYSTEM)), THEME_FOLLOW_SYSTEM);
		reverse = ni.PreferencesGetBoolean("Reverse", false);
		sticky = ni.PreferencesGetBoolean("Sticky", true);
		verySticky = ni.PreferencesGetBoolean("VerySticky", false);
		showPopupPreview = ni.PreferencesGetBoolean("ShowPopupPreview", true);
		vibrationMillis = ni.PreferencesGetInt("VibrationMilliseconds", 10);
		externalGlyphSelectionDistanceFactor = ni.PreferencesGetInt("ExternalGlyphSelectionDistanceFactor", 7);
		topBarWentBottom = ni.PreferencesGetBoolean("TopBarWentBottom", false);
		topBarHidden = ni.PreferencesGetBoolean(KEY_TOP_BAR_HIDDEN, false);
		enableSuggestions = ni.PreferencesGetBoolean("EnableSuggestions", false);
		suggestionsLanguage = ni.PreferencesGetString("SuggestionsLanguage", "en_US");
		disableAutomaticCapitalization = ni.PreferencesGetBoolean("DisableAutomaticCapitalization", false);
		characterCustomization = ni.PreferencesGetString(
				context.getString(R.string.preferences_summary_character_customization_edit_text_key),
				context.getString(R.string.character_customization_default_value));
		diacriticsCustomization = ni.PreferencesGetString(
				context.getString(R.string.preferences_summary_diacritics_customization_edit_text_key),
				context.getString(R.string.diacritics_customization_default_value));
	}

	public void setTopBarHidden(boolean hidden)
	{
		topBarHidden = hidden;
		NativeInterface.getInstance().PreferencesPut(KEY_TOP_BAR_HIDDEN, hidden);
	}

	private static int parseInt(String s, int def)
	{
		try
		{
			return Integer.parseInt(s);
		}
		catch (NumberFormatException e)
		{
			return def;
		}
	}
}
