package com.foschia.tondokeyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public class MainActivity extends AppCompatActivity
{

	public static final String SETTINGS_INTENT_MESSAGE = "com.foschia.tondokeyboard.gotoSettings";

	private EncryptedSharedPreferences eSharedPreferences;
	private CheckBox checkBoxSticky;
	private CheckBox checkBoxVerySticky;
	private TondoBilling tondoBilling;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null && intent.getExtras() != null && getIntent().getExtras().containsKey(SETTINGS_INTENT_MESSAGE))
		{
			StartSettingsActivity();
		}

		initACRA();

		eSharedPreferences = new EncryptedSharedPreferences(this);

		tondoBilling = new TondoBilling(getBaseContext(), eSharedPreferences);
		tondoBilling.SetMainActivity(this);
		tondoBilling.InitializeAndStartBilling();

		setContentView(R.layout.activity_main);

		setupUICallbacks();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		tondoBilling.InitializeAndStartBilling();
	}

	private void initACRA()
	{
		if (BuildConfig.DEBUG_MODE)
		{
			if (!org.acra.ACRA.isInitialised())
			{
				try
				{
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss", Locale.getDefault());
					String currentDateTime = dateFormat.format(Calendar.getInstance().getTime());

					org.acra.config.CoreConfigurationBuilder builder = new org.acra.config.CoreConfigurationBuilder(this)
							.setApplicationLogFileLines(999)
							.setEnabled(true)
							.setReportFormat(org.acra.data.StringFormat.KEY_VALUE_LIST);
					builder.getPluginConfigurationBuilder(org.acra.config.MailSenderConfigurationBuilder.class)
							.setEnabled(true)
							.setMailTo(getResources().getString(R.string.acra_mail_to))
							.setReportFileName("TondoKeyboard_Crash_Report_VersionName_" + BuildConfig.VERSION_NAME + "_DateTime_" + currentDateTime + ".txt")
							.setReportAsFile(true);

					org.acra.config.CoreConfiguration config = builder.build();
					org.acra.ACRA.init(this.getApplication(), config);
				}
				catch (org.acra.config.ACRAConfigurationException e)
				{
					Utils.DebugLog(Utils.LogType.ERROR, "Exception caught while trying to initialize ACRA.\nException message: " + e.getMessage());
				}
			}
		}
	}

	private void setupUICallbacks()
	{
		if (BuildConfig.DEBUG_MODE)
		{
			((TextView) findViewById(R.id.versionText)).setText("Debug version: " + BuildConfig.VERSION_NAME);

			((Button) findViewById(R.id.buttonSendEmail)).setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					SendLogcat();
				}
			});
		}
		else
		{
			View topBarView = findViewById(R.id.topBar);
			((ViewManager) topBarView.getParent()).removeView(topBarView);
		}

		((Button) findViewById(R.id.buttonEnableKeyboard)).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivityForResult(new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS), 0);
			}
		});

		((Button) findViewById(R.id.buttonChooseKeyboard)).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				InputMethodManager imeManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imeManager != null)
				{
					imeManager.showInputMethodPicker();
				}
			}
		});

		((Button) findViewById(R.id.buttonSettings)).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				StartSettingsActivity();
			}
		});
	}

	public void StartSettingsActivity()
	{
		Intent myIntent = new Intent(this, SettingsActivity.class);
		startActivity(myIntent);
	}

	public void SendLogcat()
	{
		if (BuildConfig.DEBUG_MODE)
		{
			if (org.acra.ACRA.isInitialised())
			{
				org.acra.ACRA.getErrorReporter().handleException(null);
			}
		}
	}

	public void EnableShopButton(boolean enable)
	{
		Utils.DebugLog(Utils.LogType.INFO, "MainActivity Billing - EnableShopButton: " + enable);

		Button shopButton = ((Button) findViewById(R.id.buttonShop));

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				shopButton.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
				shopButton.setEnabled(enable);
			}
		});

		Activity activity = this;

		shopButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (enable)
				{
					tondoBilling.LaunchPurchaseFlow(activity);
				}
			}
		});
	}
}