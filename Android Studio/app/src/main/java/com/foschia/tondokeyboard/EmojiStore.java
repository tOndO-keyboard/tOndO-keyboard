package com.foschia.tondokeyboard;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Persists the recently / most used emojis and the selected skin tones. */
public class EmojiStore
{
	private static final String KEY_EMOJIS = "EmojiStore_emojis";
	private static final String KEY_USAGES = "EmojiStore_usages";
	private static final String KEY_TONE1 = "EmojiStore_tone1";
	private static final String KEY_TONE2 = "EmojiStore_tone2";
	public static final int FREE_CAPACITY = 10;
	public static final int PRO_CAPACITY = 20;

	private static EmojiStore instance;

	private final List<String> emojis = new ArrayList<>();
	private final List<Integer> usages = new ArrayList<>();
	private int tone1, tone2;
	private final List<Runnable> listeners = new ArrayList<>();

	public static EmojiStore get(Context ignored)
	{
		if (instance == null) instance = new EmojiStore();
		return instance;
	}

	private EmojiStore()
	{
		NativeInterface ni = NativeInterface.getInstance();
		try
		{
			JSONArray e = new JSONArray(ni.PreferencesGetString(KEY_EMOJIS, "[]"));
			JSONArray u = new JSONArray(ni.PreferencesGetString(KEY_USAGES, "[]"));
			for (int i = 0; i < e.length() && i < u.length(); i++)
			{
				emojis.add(e.getString(i));
				usages.add(u.getInt(i));
			}
		}
		catch (JSONException ignored)
		{
		}
		tone1 = ni.PreferencesGetInt(KEY_TONE1, 0);
		tone2 = ni.PreferencesGetInt(KEY_TONE2, 0);
	}

	public void addListener(Runnable r)
	{
		if (!listeners.contains(r)) listeners.add(r);
	}

	public void removeListener(Runnable r)
	{
		listeners.remove(r);
	}

	private void changed()
	{
		for (Runnable r : new ArrayList<>(listeners)) r.run();
	}

	public int getSkinTone()
	{
		return tone1;
	}

	public int getSkinTone2()
	{
		return tone2;
	}

	/** Tap: sets both tones. */
	public void setSkinTone(int tone)
	{
		tone1 = tone2 = tone;
		persistTones();
		changed();
	}

	/** Long press: sets only the second tone (for emojis with two people). */
	public void setSkinTone2(int tone)
	{
		tone2 = tone;
		persistTones();
		changed();
	}

	private void persistTones()
	{
		NativeInterface ni = NativeInterface.getInstance();
		ni.PreferencesPut(KEY_TONE1, tone1);
		ni.PreferencesPut(KEY_TONE2, tone2);
	}

	public int capacity()
	{
		return NativeInterface.getInstance().isPro() ? PRO_CAPACITY : FREE_CAPACITY;
	}

	/** Most used first. */
	public List<String> mostUsed()
	{
		List<Integer> idx = new ArrayList<>();
		for (int i = 0; i < emojis.size(); i++) idx.add(i);
		Collections.sort(idx, (a, b) -> Integer.compare(usages.get(b), usages.get(a)));
		List<String> out = new ArrayList<>();
		for (int i : idx)
		{
			if (out.size() >= capacity()) break;
			out.add(emojis.get(i));
		}
		return out;
	}

	public void use(String emoji)
	{
		int i = emojis.indexOf(emoji);
		if (i >= 0) usages.set(i, usages.get(i) + 1);
		else
		{
			if (emojis.size() >= PRO_CAPACITY)
			{
				int min = 0;
				for (int k = 1; k < usages.size(); k++) if (usages.get(k) < usages.get(min)) min = k;
				emojis.remove(min);
				usages.remove(min);
			}
			emojis.add(emoji);
			usages.add(1);
		}
		save();
		changed();
	}

	private void save()
	{
		NativeInterface ni = NativeInterface.getInstance();
		ni.PreferencesPut(KEY_EMOJIS, new JSONArray(emojis).toString());
		ni.PreferencesPut(KEY_USAGES, new JSONArray(usages).toString());
	}
}
