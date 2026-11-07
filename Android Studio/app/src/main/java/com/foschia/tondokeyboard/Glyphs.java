package com.foschia.tondokeyboard;

import java.util.ArrayList;
import java.util.List;

public final class Glyphs
{
	private Glyphs() {}

	/** Splits a string into code points (API 21 friendly). */
	public static String[] split(String s)
	{
		List<String> out = new ArrayList<>();
		for (int i = 0; i < s.length(); )
		{
			int cp = s.codePointAt(i);
			out.add(new String(Character.toChars(cp)));
			i += Character.charCount(cp);
		}
		return out.toArray(new String[0]);
	}
}
