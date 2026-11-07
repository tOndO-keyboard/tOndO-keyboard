package com.foschia.tondokeyboard;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CharacterMapTest
{
	/** The default code, as in strings.xml. */
	private static final String DEFAULT = "EQWRT*\nUYFGH'\"\nIJLP:^\nA@()DS#\n Z,.VCX\nOB!?KMN\n\n$/123*\n£4567'\"\n€890:^\n[@()]%#\n ;,.&><\n_~!?-+=\n\n•\\|½⅓²\nβαδεγ»«\nπλΣΔρ°\n✓₿{}¥§¶\n ®™¢©≥≤\n×√∞∙÷±≠";

	private final CharacterMap map = new CharacterMap(DEFAULT);

	@Test
	public void matchesOriginalLayout()
	{
		CharacterMap.Layout l = CharacterMap.Layout.MAIN;
		CharacterMap.Stick tl = CharacterMap.Stick.TOP_LEFT;
		assertEquals("E", map.get(l, tl, Dir.CENTER));
		assertEquals("T", map.get(l, tl, Dir.EAST));
		assertEquals("R", map.get(l, tl, Dir.SOUTH_EAST));
		assertEquals("W", map.get(l, tl, Dir.SOUTH_WEST));
		assertEquals("Q", map.get(l, tl, Dir.WEST));
		assertEquals("*", map.get(l, tl, Dir.NORTH_EAST));
		assertEquals("", map.get(l, tl, Dir.NORTH_WEST)); // diacriticizer
	}

	@Test
	public void topRightSkipsNorthEast()
	{
		CharacterMap.Layout l = CharacterMap.Layout.MAIN;
		CharacterMap.Stick tr = CharacterMap.Stick.TOP_RIGHT;
		assertEquals("I", map.get(l, tr, Dir.CENTER));
		assertEquals(":", map.get(l, tr, Dir.EAST));
		assertEquals("^", map.get(l, tr, Dir.NORTH_WEST));
		assertEquals("", map.get(l, tr, Dir.NORTH_EAST)); // diacriticizer
	}

	@Test
	public void bottomCentreHasSpaceInTheMiddle()
	{
		CharacterMap.Layout l = CharacterMap.Layout.MAIN;
		CharacterMap.Stick bc = CharacterMap.Stick.BOTTOM_CENTER;
		assertEquals(" ", map.get(l, bc, Dir.CENTER));
		assertEquals("V", map.get(l, bc, Dir.EAST));
		assertEquals("Z", map.get(l, bc, Dir.WEST));
		assertEquals(".", map.get(l, bc, Dir.SOUTH_EAST));
		assertEquals(",", map.get(l, bc, Dir.SOUTH_WEST));
		assertEquals("X", map.get(l, bc, Dir.NORTH_WEST));
		assertEquals("C", map.get(l, bc, Dir.NORTH_EAST));
	}

	@Test
	public void symbolLayouts()
	{
		assertEquals("$", map.get(CharacterMap.Layout.SYMBOLS1, CharacterMap.Stick.TOP_LEFT, Dir.CENTER));
		assertEquals("€", map.get(CharacterMap.Layout.SYMBOLS1, CharacterMap.Stick.TOP_RIGHT, Dir.CENTER));
		assertEquals("•", map.get(CharacterMap.Layout.SYMBOLS2, CharacterMap.Stick.TOP_LEFT, Dir.CENTER));
		assertEquals("©", map.get(CharacterMap.Layout.SYMBOLS2, CharacterMap.Stick.BOTTOM_CENTER, Dir.EAST));
		assertEquals(" ", map.get(CharacterMap.Layout.SYMBOLS2, CharacterMap.Stick.BOTTOM_CENTER, Dir.CENTER));
		assertEquals("≥", map.get(CharacterMap.Layout.SYMBOLS2, CharacterMap.Stick.BOTTOM_CENTER, Dir.NORTH_EAST));
	}

	@Test
	public void missingEntriesAreEmpty()
	{
		assertEquals("", new CharacterMap("").get(CharacterMap.Layout.SYMBOLS2, CharacterMap.Stick.BOTTOM_RIGHT, Dir.EAST));
	}
}
