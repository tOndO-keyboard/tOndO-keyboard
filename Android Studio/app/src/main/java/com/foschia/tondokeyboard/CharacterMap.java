package com.foschia.tondokeyboard;

import java.util.EnumMap;
import java.util.Map;

/** Parses the "keyboard customization code" into the glyph shown at each tondo / direction. */
public final class CharacterMap
{
	public enum Layout {MAIN, SYMBOLS1, SYMBOLS2}

	/** Sticks, in the order used by the customization string. */
	public enum Stick {TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT}

	private static final Dir[] ORDER = {Dir.CENTER, Dir.WEST, Dir.SOUTH_WEST, Dir.SOUTH_EAST, Dir.EAST, Dir.NORTH_EAST, Dir.NORTH_WEST};

	private final Map<Layout, Map<Stick, Map<Dir, String>>> map = new EnumMap<>(Layout.class);

	public CharacterMap(String code)
	{
		String[] layouts = code.split("\n\n", -1);
		for (int i = 0; i < Layout.values().length && i < layouts.length; i++)
		{
			String[] sticks = layouts[i].split("\n", -1);
			Map<Stick, Map<Dir, String>> sm = new EnumMap<>(Stick.class);
			for (int j = 0; j < Stick.values().length && j < sticks.length; j++)
			{
				Stick stick = Stick.values()[j];
				String[] cps = Glyphs.split(sticks[j]);
				Map<Dir, String> dm = new EnumMap<>(Dir.class);
				for (int k = 0; k < ORDER.length && k < cps.length; k++)
				{
					int idx = k;
					// North east of the top right tondo is the place of the diacriticizer: skip it.
					if (stick == Stick.TOP_RIGHT && k == 5) idx++;
					if (idx >= ORDER.length) break;
					if (!dm.containsKey(ORDER[idx])) dm.put(ORDER[idx], cps[k]);
				}
				sm.put(stick, dm);
			}
			map.put(Layout.values()[i], sm);
		}
	}

	/** @return the glyph or an empty string if there is none. */
	public String get(Layout layout, Stick stick, Dir dir)
	{
		Map<Stick, Map<Dir, String>> sm = map.get(layout);
		if (sm == null) return "";
		Map<Dir, String> dm = sm.get(stick);
		if (dm == null) return "";
		String s = dm.get(dir);
		return s == null ? "" : s;
	}
}
