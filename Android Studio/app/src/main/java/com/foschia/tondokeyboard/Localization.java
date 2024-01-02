package com.foschia.tondokeyboard;

import android.content.Context;

import androidx.annotation.Keep;

import java.util.HashMap;
import java.util.Map;

@Keep
public class Localization
{
	public static final Localization Instance = new Localization();

	private final Map<String, Integer> mIdsCache = new HashMap<>();
	private Context mContext;

	private Localization()
	{
	}

	private int findId(String key)
	{
		if (!isInitialized())
			throw new NullPointerException("Localization instance was not initialized");
		return mContext.getResources().getIdentifier(key, "string", mContext.getPackageName());
	}

	public void init(Context context)
	{
		mContext = context;
	}

	public boolean isInitialized()
	{
		return mContext != null;
	}

	public boolean keyExists(String key)
	{
		return mIdsCache.containsKey(key) || findId(key) > 0;
	}

	public String localize(String key)
	{
		return localizeOrDefault(key, key);
	}

	public String localizeOrDefault(String key, String def)
	{
		int id = 0;
		if (mIdsCache.containsKey(key)) id = mIdsCache.get(key);
		else
		{
			id = findId(key);
			if (id > 0) mIdsCache.put(key, id);
		}
		if (id <= 0) return def;
		return mContext.getResources().getString(id);
	}
}
