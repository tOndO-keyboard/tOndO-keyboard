package com.foschia.tondokeyboard;

import android.app.Activity;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;

public class PermissionsActivity extends Activity
{
	public static final String PERMISSIONS_EXTRA = "PermissionsExtra";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		String[] permissions = getIntent().getExtras().getStringArray(PERMISSIONS_EXTRA);
		ActivityCompat.requestPermissions(this, permissions, 0);
	}
}