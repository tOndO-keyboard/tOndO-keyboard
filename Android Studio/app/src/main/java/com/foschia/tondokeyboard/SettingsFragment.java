package com.foschia.tondokeyboard;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

public class SettingsFragment extends PreferenceFragmentCompat implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback
{
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
	{
		getPreferenceManager().setSharedPreferencesName(EncryptedSharedPreferences.SHARED_PREFERENCES_NAME);
		getPreferenceManager().setSharedPreferencesMode(EncryptedSharedPreferences.SHARED_PREFERENCES_MODE);

		setPreferencesFromResource(R.xml.root_preferences, rootKey);
	}

	@Override
	public Fragment getCallbackFragment()
	{
		return this;
	}

	@Override
	public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat,
										   PreferenceScreen preferenceScreen)
	{
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		MyPreferenceFragment fragment = new MyPreferenceFragment();
		Bundle args = new Bundle();
		args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
		fragment.setArguments(args);
		ft.add(R.id.settings, fragment, preferenceScreen.getKey());
		ft.addToBackStack(preferenceScreen.getKey());
		ft.commit();
		return true;
	}
}