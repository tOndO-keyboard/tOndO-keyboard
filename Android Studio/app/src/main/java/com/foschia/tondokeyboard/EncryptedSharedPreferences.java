package com.foschia.tondokeyboard;

import static android.content.Context.MODE_MULTI_PROCESS;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

public class EncryptedSharedPreferences
{
	public static final String SHARED_PREFERENCES_NAME = "com.foschia.tondokeyboard.preferences";
	public static final int SHARED_PREFERENCES_MODE = MODE_MULTI_PROCESS;

	private final Context currentContext;

	public EncryptedSharedPreferences(Context context)
	{
		currentContext = context;
	}

	private static String encrypt(String input)
	{
		return Base64.encodeToString(input.getBytes(), Base64.DEFAULT);
	}

	private static String decrypt(String input)
	{
		return new String(Base64.decode(input, Base64.DEFAULT));
	}

	private SharedPreferences sharedPreferences()
	{
		if (currentContext == null) return null;

		return currentContext.getSharedPreferences(SHARED_PREFERENCES_NAME, SHARED_PREFERENCES_MODE);
	}

	public void putEncrypted(String key, Boolean value)
	{
		sharedPreferences().edit().putString(encrypt(key), encrypt(Boolean.toString(value))).apply();
	}

	public void putEncrypted(String key, int value)
	{
		sharedPreferences().edit().putString(encrypt(key), encrypt(Integer.toString(value))).apply();
	}

	public void putEncrypted(String key, float value)
	{
		sharedPreferences().edit().putString(encrypt(key), encrypt(Float.toString(value))).apply();
	}

	public void putEncrypted(String key, String value)
	{
		sharedPreferences().edit().putString(encrypt(key), encrypt(value)).apply();
	}

	public boolean getEncryptedBoolean(String key, boolean defaultValue)
	{
		return Boolean.parseBoolean((decrypt(sharedPreferences().getString(encrypt(key), encrypt(Boolean.toString(defaultValue))))));
	}

	public int getEncryptedInt(String key, int defaultValue)
	{
		return Integer.parseInt(decrypt(sharedPreferences().getString(encrypt(key), encrypt(Integer.toString(defaultValue)))));
	}

	public float getEncryptedFloat(String key, float defaultValue)
	{
		return Float.parseFloat(decrypt(sharedPreferences().getString(encrypt(key), encrypt(Float.toString(defaultValue)))));
	}

	public String getEncryptedString(String key, String defaultValue)
	{
		return decrypt(sharedPreferences().getString(encrypt(key), encrypt(defaultValue)));
	}

	public boolean containsEncrypted(String key)
	{
		return sharedPreferences().contains(encrypt(key));
	}

	public void put(String key, Boolean value)
	{
		sharedPreferences().edit().putBoolean(key, value).apply();
	}

	public void put(String key, int value)
	{
		sharedPreferences().edit().putInt(key, value).apply();
	}

	public void put(String key, String value)
	{
		sharedPreferences().edit().putString(key, value).apply();
	}

	public void put(String key, float value)
	{
		sharedPreferences().edit().putFloat(key, value).apply();
	}

	public boolean getBoolean(String key, boolean defaultValue)
	{
		return sharedPreferences().getBoolean(key, defaultValue);
	}

	public int getInt(String key, int defaultValue)
	{
		return sharedPreferences().getInt(key, defaultValue);
	}

	public float getFloat(String key, float defaultValue)
	{
		return sharedPreferences().getFloat(key, defaultValue);
	}

	public String getString(String key, String defaultValue)
	{
		return sharedPreferences().getString(key, defaultValue);
	}

	public boolean contains(String key)
	{
		return sharedPreferences().contains(key);
	}
}
