package com.foschia.tondokeyboard;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity
{
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				onBackPressed();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_activity);
		if (savedInstanceState == null)
		{
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.settings, new SettingsFragment())
					.commit();
		}
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		//changing action bar title color
		String actionBarTitle = getResources().getString(R.string.title_activity_settings);
		String actionBarColor = String.format("#%06X", 0xFFFFFF & getResources().getColor(R.color.White));
		actionBar.setTitle(Html.fromHtml("<font color='" + actionBarColor + "'>" + actionBarTitle + "</font>"));

		//changing action bar back button color
		final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_material);
		upArrow.setColorFilter(getResources().getColor(R.color.White), PorterDuff.Mode.SRC_ATOP);
		getSupportActionBar().setHomeAsUpIndicator(upArrow);
	}
	
}