package com.foschia.tondokeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

public class DiacriticsCustomizationDialogPreference extends Preference
{
	private final Context context;

	public DiacriticsCustomizationDialogPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.context = context;
	}
	
	public DiacriticsCustomizationDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
		this.context = context;
	}

	public DiacriticsCustomizationDialogPreference(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		this.context = context;
	}

	public DiacriticsCustomizationDialogPreference(Context context)
	{
		super(context);
		this.context = context;
	}


	@Override
	protected void onClick()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.preferences_summary_diacritics_customization_reset_dialog_title)
				.setMessage(R.string.preferences_summary_diacritics_customization_reset_dialog_message)
				.setPositiveButton(R.string.preferences_summary_diacritics_customization_reset_dialog_ok, (dialog, which) ->
				{
					SharedPreferences sharedPreferences = getSharedPreferences();
					SharedPreferences.Editor editor = sharedPreferences.edit();
					editor.putString(context.getResources().getString(R.string.preferences_summary_diacritics_customization_edit_text_key),
							context.getResources().getString(R.string.diacritics_customization_default_value));
					editor.commit();
				})
				.setNegativeButton(R.string.preferences_summary_diacritics_customization_reset_dialog_cancel, (dialog, which) ->
				{

				})
				.create().show();
	}
}