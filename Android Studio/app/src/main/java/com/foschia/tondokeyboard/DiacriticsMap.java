package com.foschia.tondokeyboard;

import java.util.HashMap;
import java.util.Map;

/**
 * Alternate glyphs (diacritics) available for each preceding character, plus the remembered
 * last choice (shown on the diacriticizer glyph).
 */
public class DiacriticsMap
{
	static class Entry
	{
		String[] left = new String[0];
		String[] right = new String[0];
		int lastLeft = 0, lastRight = 0;
	}

	private final Map<String, Entry> entries = new HashMap<>();

	public DiacriticsMap(String code)
	{
		if (code == null) return;
		for (String line : code.split("\n"))
		{
			String[] parts = line.split(" ");
			if (parts.length == 0 || parts[0].isEmpty()) continue;
			String key = parts[0].toLowerCase();
			if (entries.containsKey(key)) continue;
			Entry e = new Entry();
			if (parts.length > 1) e.left = split(parts[1]);
			if (parts.length > 2) e.right = split(parts[2]);
			e.lastLeft = loadIndex(key, true, parts.length > 3 ? parts[3] : null);
			e.lastRight = loadIndex(key, false, parts.length > 4 ? parts[4] : null);
			entries.put(key, e);
		}
	}

	private static String[] split(String s)
	{
		String[] r = Glyphs.split(s);
		for (int i = 0; i < r.length; i++) r[i] = r[i].toLowerCase();
		return r;
	}

	private static String prefKey(String key, boolean left)
	{
		return "DiacriticsLast_" + key + (left ? "_l" : "_r");
	}

	private static int loadIndex(String key, boolean left, String codeValue)
	{
		int def = 0;
		if (codeValue != null)
		{
			try
			{
				def = Integer.parseInt(codeValue);
			}
			catch (NumberFormatException ignored)
			{
			}
		}
		return NativeInterface.getInstance().PreferencesGetInt(prefKey(key, left), def);
	}

	private static boolean isUpper(String preceding)
	{
		return Character.isUpperCase(preceding.codePointAt(0));
	}

	private static String lastChar(String preceding)
	{
		if (preceding == null || preceding.isEmpty()) return "";
		int cp = preceding.codePointBefore(preceding.length());
		return new String(Character.toChars(cp));
	}

	/** Alternate glyphs for the character before the cursor, with the case of that character, or null. */
	public String[] alternates(String preceding, boolean left)
	{
		preceding = lastChar(preceding);
		if (preceding.isEmpty()) return null;
		Entry e = entries.get(preceding.toLowerCase());
		if (e == null) return null;
		String[] src = left ? e.left : e.right;
		String[] res = new String[src.length];
		boolean upper = isUpper(preceding);
		for (int i = 0; i < src.length; i++) res[i] = upper ? src[i].toUpperCase() : src[i].toLowerCase();
		return res;
	}

	/** The remembered alternate for the preceding character or null. */
	public String lastSelected(String preceding, boolean left)
	{
		preceding = lastChar(preceding);
		if (preceding.isEmpty()) return null;
		Entry e = entries.get(preceding.toLowerCase());
		if (e == null) return null;
		String[] src = left ? e.left : e.right;
		if (src.length == 0) return null;
		int idx = left ? e.lastLeft : e.lastRight;
		if (idx >= src.length || idx < 0) idx = 0;
		String s = src[idx];
		if (s.isEmpty()) return null;
		return isUpper(preceding) ? s.toUpperCase() : s.toLowerCase();
	}

	public void setLastSelectedIndex(String preceding, int index, boolean left)
	{
		preceding = lastChar(preceding);
		if (preceding.isEmpty()) return;
		String key = preceding.toLowerCase();
		Entry e = entries.get(key);
		if (e == null) return;
		if (left) e.lastLeft = index;
		else e.lastRight = index;
		NativeInterface.getInstance().PreferencesPut(prefKey(key, left), index);
	}
}
