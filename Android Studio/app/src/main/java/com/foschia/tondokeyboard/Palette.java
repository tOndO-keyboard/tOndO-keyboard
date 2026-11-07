package com.foschia.tondokeyboard;

import android.content.Context;
import android.os.Build;

/** Colour themes, values taken from the original colour palette assets. */
public final class Palette
{
	public final int glyphSymbol, glyphConsonant, glyphVowel;
	public final int barButtonBackground, barButtonIcon, barDirectionalBackground, barDirectionalIcon, barBackground;
	public final int tondosExterior, tondosInterior, tondosCavity;
	public final int permanentButtonBackground, permanentButtonIcon, doneBackground, doneIcon, shiftPressedIcon;
	public final int keyboardBackground, glyphSelected, glyphSelectedBackground, shadow;

	private Palette(int... v)
	{
		int i = 0;
		glyphSymbol = v[i++];
		glyphConsonant = v[i++];
		glyphVowel = v[i++];
		barButtonBackground = v[i++];
		barButtonIcon = v[i++];
		barDirectionalBackground = v[i++];
		barDirectionalIcon = v[i++];
		barBackground = v[i++];
		tondosExterior = v[i++];
		tondosInterior = v[i++];
		tondosCavity = v[i++];
		permanentButtonBackground = v[i++];
		permanentButtonIcon = v[i++];
		doneBackground = v[i++];
		doneIcon = v[i++];
		shiftPressedIcon = v[i++];
		keyboardBackground = v[i++];
		glyphSelected = v[i++];
		glyphSelectedBackground = v[i++];
		shadow = v[i];
	}

	public static final Palette LIGHT = new Palette(
			0xFF999999, 0xFF666666, 0xFF333333,
			0xFFE7ECEB, 0xFF666666, 0xFF666666, 0xFFE7ECEB, 0xFF61CFC2,
			0xFFE7ECEB, 0xFFF6F8F8, 0xFF61CFC2,
			0xFFE7ECEB, 0xFF666666, 0xFF61CFC2, 0xFFF6F8F8, 0xFF61CFC2,
			0xFFF6F8F8, 0xFF61CFC2, 0xFFC8ECE9, 0x666F97A2);

	public static final Palette DARK = new Palette(
			0xFF808080, 0xFFC2C2C2, 0xFFFFFFFF,
			0xFF474747, 0xFFC2C2C2, 0xFFC2C2C2, 0xFF474747, 0xFF00B2A9,
			0xFF474747, 0xFF585858, 0xFF00B2A9,
			0xFF474747, 0xFFC2C2C2, 0xFF00B2A9, 0xFFF6F8F8, 0xFF00B2A9,
			0xFF585858, 0xFF61CFC2, 0xFFC8ECE9, 0x80000000);

	/** True when the system provides Material You (wallpaper based) colours. */
	public static boolean dynamicAvailable()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
	}

	/** Builds a palette from the Material You system colours; falls back to the static ones before Android 12. */
	public static Palette dynamic(Context c, boolean dark)
	{
		if (!dynamicAvailable()) return dark ? DARK : LIGHT;
		if (dark)
		{
			int a3 = c.getColor(android.R.color.system_accent1_300);
			int a4 = c.getColor(android.R.color.system_accent1_400);
			int a1 = c.getColor(android.R.color.system_accent1_100);
			int n50 = c.getColor(android.R.color.system_neutral1_50);
			int n200 = c.getColor(android.R.color.system_neutral1_200);
			int n500 = c.getColor(android.R.color.system_neutral1_500);
			int n700 = c.getColor(android.R.color.system_neutral1_700);
			int n800 = c.getColor(android.R.color.system_neutral1_800);
			return new Palette(
					n500, n200, n50,
					n800, n200, n200, n800, a4,
					n800, n700, a4,
					n800, n200, a4, n50, a4,
					n700, a3, a1, 0x80000000);
		}
		int a1 = c.getColor(android.R.color.system_accent1_100);
		int a5 = c.getColor(android.R.color.system_accent1_500);
		int n50 = c.getColor(android.R.color.system_neutral1_50);
		int n100 = c.getColor(android.R.color.system_neutral1_100);
		int n400 = c.getColor(android.R.color.system_neutral1_400);
		int n600 = c.getColor(android.R.color.system_neutral1_600);
		int n900 = c.getColor(android.R.color.system_neutral1_900);
		return new Palette(
				n400, n600, n900,
				n100, n600, n600, n100, a5,
				n100, n50, a5,
				n100, n600, a5, n50, a5,
				n50, a5, a1, 0x666F97A2);
	}
}
