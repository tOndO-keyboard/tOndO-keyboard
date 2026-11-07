package com.foschia.tondokeyboard;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/** Pinned clipboards ("macros" / "notes"). */
public class MacroStore
{
	private static final String KEY_MACROS = "MacroStore_macros";
	private static final String KEY_USAGES = "MacroStore_usages";
	private static final String KEY_TUTORIAL = "MacroStore_tutorial";
	private static final String KEY_INITIALIZED = "MacroStore_initialized";
	public static final int PRO_CAPACITY = 20;
	public static final int FREE_CAPACITY = 6;

	private static MacroStore instance;

	private final List<String> macros = new ArrayList<>();
	private final List<Integer> usages = new ArrayList<>();
	private final List<Boolean> tutorial = new ArrayList<>();
	private final List<Runnable> listeners = new ArrayList<>();

	public static MacroStore get()
	{
		if (instance == null) instance = new MacroStore();
		return instance;
	}

	private MacroStore()
	{
		NativeInterface ni = NativeInterface.getInstance();
		try
		{
			JSONArray m = new JSONArray(ni.PreferencesGetString(KEY_MACROS, "[]"));
			JSONArray u = new JSONArray(ni.PreferencesGetString(KEY_USAGES, "[]"));
			JSONArray t = new JSONArray(ni.PreferencesGetString(KEY_TUTORIAL, "[]"));
			for (int i = 0; i < m.length(); i++)
			{
				macros.add(m.getString(i));
				usages.add(i < u.length() ? u.getInt(i) : 0);
				tutorial.add(i < t.length() && t.getBoolean(i));
			}
		}
		catch (JSONException ignored)
		{
		}
		if (!ni.PreferencesGetBoolean(KEY_INITIALIZED, false))
		{
			Localization l = Localization.Instance;
			if (l.isInitialized())
			{
				add(l.localize("macros_tutorial_pro"), true);
				add(l.localize("macros_tutorial_right"), true);
				add(l.localize("macros_tutorial_left"), true);
				ni.PreferencesPut(KEY_INITIALIZED, true);
			}
		}
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
		save();
		for (Runnable r : new ArrayList<>(listeners)) r.run();
	}

	public int capacity()
	{
		return NativeInterface.getInstance().isPro() ? PRO_CAPACITY : FREE_CAPACITY;
	}

	public int size()
	{
		return macros.size();
	}

	public boolean contains(String text)
	{
		return macros.contains(text);
	}

	public boolean isTutorial(String text)
	{
		int i = macros.indexOf(text);
		return i >= 0 && tutorial.get(i);
	}

	/** Macros, most used first, limited to the current capacity. */
	public List<String> list()
	{
		List<Integer> idx = new ArrayList<>();
		for (int i = 0; i < macros.size(); i++) idx.add(i);
		java.util.Collections.sort(idx, (a, b) -> Integer.compare(usages.get(b), usages.get(a)));
		List<String> out = new ArrayList<>();
		for (int i : idx)
		{
			if (out.size() >= capacity()) break;
			out.add(macros.get(i));
		}
		return out;
	}

	/** @return false if there is no room. */
	public boolean add(String text, boolean isTutorial)
	{
		if (macros.contains(text)) return true;
		if (macros.size() >= PRO_CAPACITY) return false;
		macros.add(text);
		usages.add(0);
		tutorial.add(isTutorial);
		changed();
		return true;
	}

	public boolean canAddMore()
	{
		return macros.size() < capacity();
	}

	public void remove(String text)
	{
		int i = macros.indexOf(text);
		if (i < 0) return;
		macros.remove(i);
		usages.remove(i);
		tutorial.remove(i);
		changed();
	}

	public void use(String text)
	{
		int i = macros.indexOf(text);
		if (i < 0) return;
		usages.set(i, usages.get(i) + 1);
		changed();
	}

	private void save()
	{
		NativeInterface ni = NativeInterface.getInstance();
		ni.PreferencesPut(KEY_MACROS, new JSONArray(macros).toString());
		ni.PreferencesPut(KEY_USAGES, new JSONArray(usages).toString());
		ni.PreferencesPut(KEY_TUTORIAL, new JSONArray(tutorial).toString());
	}
}
