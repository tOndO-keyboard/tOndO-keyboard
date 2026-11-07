package com.foschia.tondokeyboard;

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
}
