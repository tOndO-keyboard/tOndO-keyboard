package com.foschia.tondokeyboard;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

public class ShoppingPreferenceCategory extends PreferenceCategory
{
	private final Context context;

	private TondoBilling tondoBilling;

	public ShoppingPreferenceCategory(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.context = context;
		InitBilling();
	}

	public ShoppingPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
		this.context = context;
		InitBilling();
	}

	public ShoppingPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		this.context = context;
		InitBilling();
	}

	public ShoppingPreferenceCategory(Context context)
	{
		super(context);
		this.context = context;
		InitBilling();
	}

	private void InitBilling()
	{
		Utils.DebugLog(Utils.LogType.INFO, "ShoppingPreferenceCategory - InitBilling");
		EncryptedSharedPreferences eSharedPreferences = new EncryptedSharedPreferences(getContext());
		tondoBilling = new TondoBilling(getContext(), eSharedPreferences);
		tondoBilling.SetShoppingPreferenceCategory(this);
		tondoBilling.InitializeAndStartBilling();
	}


	@Override
	protected void onClick()
	{
		Utils.DebugLog(Utils.LogType.INFO, "ShoppingPreferenceCategory - onClick");
	}

	public void EnableShopButton(boolean enable)
	{
		Utils.DebugLog(Utils.LogType.INFO, "ShoppingPreferenceCategory - EnableShopButton: " + enable);

		setVisible(enable);
		setEnabled(enable);
		
		Preference shopButton = ((Preference) findPreference("preference_pro_Version"));

		shopButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
		{
			@Override
			public boolean onPreferenceClick(Preference preference)
			{
				Utils.DebugLog(Utils.LogType.INFO, "ShoppingPreferenceCategory - onPreferenceClick");

				if (enable)
				{
					tondoBilling.LaunchPurchaseFlow((Activity) context);
				}
				return enable;
			}
		});
	}
}