package com.foschia.tondokeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import androidx.preference.EditTextPreference;

public class DiacriticsCustomizationEditTextPreference extends EditTextPreference
{
	private Context context;

	private String previousValue = new String();
	private boolean onSharedPreferenceChangeListenerRegistered = false;

	public DiacriticsCustomizationEditTextPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.context = context;
	}

	public DiacriticsCustomizationEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
		this.context = context;
	}

	public DiacriticsCustomizationEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		this.context = context;
	}

	// we need this just to force update the edit text,
	// otherwise the old code would be shown opening the edit text just after a rest
	@Override
	protected void onClick()
	{
		SharedPreferences sharedPreferences = getSharedPreferences();
		String s = sharedPreferences.getString(context.getResources().getString(R.string.preferences_summary_diacritics_customization_edit_text_key), context.getResources().getString(R.string.diacritics_customization_default_value));
		setText(s);
		super.onClick();
	}
}