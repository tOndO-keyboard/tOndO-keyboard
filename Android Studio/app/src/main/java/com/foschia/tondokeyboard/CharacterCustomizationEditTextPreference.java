package com.foschia.tondokeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.preference.EditTextPreference;

import java.util.LinkedList;

public class CharacterCustomizationEditTextPreference extends EditTextPreference implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private Context context;
	private EncryptedSharedPreferences eSharedPreferences;

	private String previousValue = new String();
	private boolean onSharedPreferenceChangeListenerRegistered = false;

	public CharacterCustomizationEditTextPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.context = context;
		setSummaryBaesOnPro();
	}

	public CharacterCustomizationEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
		this.context = context;
		setSummaryBaesOnPro();
	}

	public CharacterCustomizationEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		this.context = context;
		setSummaryBaesOnPro();
	}

	private void setSummaryBaesOnPro()
	{
		boolean proPurchased = wasProPurchased();
		setSummary(proPurchased ? null : (context.getResources().getString(R.string.preferences_summary_character_customization_edit_text_dialog_summary)));
	}

	private boolean wasProPurchased()
	{
		if (eSharedPreferences == null)
		{
			eSharedPreferences = new EncryptedSharedPreferences(getContext());
		}

		return eSharedPreferences.getEncryptedBoolean(UnityInterface.PURCHASED_PRO_PREFERENCES_KEY, false);
	}

	// we need this just to force update the edit text,
	// otherwise the old code would be shown opening the edit text just after a rest
	@Override
	protected void onClick()
	{
		SharedPreferences sharedPreferences = getSharedPreferences();
		unregisterOnSharedPreferenceChangeListener(sharedPreferences);
		String s = sharedPreferences.getString(context.getResources().getString(R.string.preferences_summary_character_customization_edit_text_key), context.getResources().getString(R.string.character_customization_default_value));
		setText(s);
		super.onClick();
		registerOnSharedPreferenceChangeListenerIfNeeded(sharedPreferences);
	}

	@Override
	public void onAttached()
	{
		super.onAttached();
		SharedPreferences sharedPreferences = getSharedPreferences();
		previousValue = sharedPreferences.getString(context.getResources().getString(R.string.preferences_summary_character_customization_edit_text_key), context.getResources().getString(R.string.character_customization_default_value));
		registerOnSharedPreferenceChangeListenerIfNeeded(sharedPreferences);
	}

	@Override
	public void onDetached()
	{
		super.onDetached();
		SharedPreferences sharedPreferences = getSharedPreferences();
		unregisterOnSharedPreferenceChangeListener(sharedPreferences);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (key.equals(context.getResources().getString(R.string.preferences_summary_character_customization_edit_text_key)))
		{
			String value = sharedPreferences.getString(key, context.getResources().getString(R.string.character_customization_default_value));

			//Setting only character in the first group as uppercase as soon as the user change the edit text
			String[] values = value.split("\n\n");
			if (values.length > 0) values[0] = values[0].toUpperCase();
			String newValue = "";
			for (int i = 0; i < values.length; i++)
			{
				newValue = newValue + values[i];
				if (i < values.length - 1)
				{
					newValue = newValue + "\n\n";
				}
			}

			String defaultValue = context.getResources().getString(R.string.character_customization_default_value);
			if (!previousValue.equals(newValue) && !newValue.equals(defaultValue))
			{
				diff_match_patch diffMaker = new diff_match_patch();
				LinkedList<diff_match_patch.Diff> diffs = diffMaker.diff_main(newValue.replace("", " ").trim(), defaultValue.replace("", " ").trim());

				int diffInsertNumer = 0;
				for (diff_match_patch.Diff diff : diffs)
				{
					if (diff.operation == diff_match_patch.Operation.INSERT) diffInsertNumer++;
				}

				int maxDiffValue = context.getResources().getInteger(R.integer.max_character_customization_value);
				boolean proPurchased = wasProPurchased();

				if (diffInsertNumer > maxDiffValue && !proPurchased)
				{
					Toast.makeText(context, context.getResources().getString(R.string.preferences_summary_character_customization_edit_text_dialog_toast), Toast.LENGTH_LONG).show();
					newValue = previousValue;
				}
			}

			previousValue = newValue;

			unregisterOnSharedPreferenceChangeListener(sharedPreferences);

			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString(key, newValue);
			editor.commit();

			registerOnSharedPreferenceChangeListenerIfNeeded(sharedPreferences);
		}
	}

	private void registerOnSharedPreferenceChangeListenerIfNeeded(SharedPreferences sharedPreferences)
	{
		if (onSharedPreferenceChangeListenerRegistered) return;
		onSharedPreferenceChangeListenerRegistered = true;
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	private void unregisterOnSharedPreferenceChangeListener(SharedPreferences sharedPreferences)
	{
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
		onSharedPreferenceChangeListenerRegistered = false;
	}
}
