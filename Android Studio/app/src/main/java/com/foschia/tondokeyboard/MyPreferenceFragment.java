package com.foschia.tondokeyboard;

import android.os.Bundle;

public class MyPreferenceFragment extends AppPreferenceFragment
{

	public static final String FRAGMENT_TAG = "my_preference_fragment";

	public MyPreferenceFragment()
	{
	}

	@Override
	public void onCreatePreferences(Bundle bundle, String rootKey)
	{
		getPreferenceManager().setSharedPreferencesName(EncryptedSharedPreferences.SHARED_PREFERENCES_NAME);
		getPreferenceManager().setSharedPreferencesMode(EncryptedSharedPreferences.SHARED_PREFERENCES_MODE);
		
		setPreferencesFromResource(R.xml.root_preferences, rootKey);
	}
}
