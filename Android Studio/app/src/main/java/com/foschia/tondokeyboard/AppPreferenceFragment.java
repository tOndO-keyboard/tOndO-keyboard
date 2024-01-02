package com.foschia.tondokeyboard;

import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceFragmentCompat;

public abstract class AppPreferenceFragment extends PreferenceFragmentCompat
{

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view,
				savedInstanceState);

		// Set the default white background in the view so as to avoid transparency
		view.setBackgroundColor(
				ContextCompat.getColor(getContext(), R.color.background_material_light));

	}
}
