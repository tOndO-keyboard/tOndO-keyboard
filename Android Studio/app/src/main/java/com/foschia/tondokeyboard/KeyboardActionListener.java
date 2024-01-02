package com.foschia.tondokeyboard;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Vibrator;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;

import com.unity3d.player.UnityPlayer;

import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class KeyboardActionListener extends InputMethodService
{
	public static int TEXT_INPUT = 1;
	public static int NUMERICAL_INPUT = 2;
	public static int DATE_INPUT = 4;
	public static int PASSWORD_INPUT = 8;


	public enum Direction
	{
		UP(KeyEvent.KEYCODE_DPAD_UP),
		DOWN(KeyEvent.KEYCODE_DPAD_DOWN),
		LEFT(KeyEvent.KEYCODE_DPAD_LEFT),
		RIGHT(KeyEvent.KEYCODE_DPAD_RIGHT);

		private final int key;

		Direction(int key)
		{
			this.key = key;
		}

		public int asKey()
		{
			return key;
		}
	}

	public static final int DONE_ACTION_FLAG_DONE = 0;
	public static final int DONE_ACTION_FLAG_NEWLINE = 1;
	public static final int DONE_ACTION_FLAG_SEARCH = 2;
	public static final int DONE_ACTION_FLAG_SEND = 3;
	private static final float maxHeightProportion = 1.82f;

	private static final String[] sCommonSeparators =
			new String[]{".", ";", ",", "?", "!", "-", "'", "\""};

	protected UnityPlayer mUnityPlayer;
	private float keyboardProportion = 1.5f;
	private View currentView;
	private Vibrator vibrator;

	private EncryptedSharedPreferences eSharedPreferences;

	private TondoBilling tondoBilling;

	private boolean mIsCommittingAsSession = false;

	public View getCurrentView()
	{
		return currentView;
	}

	@Override
	public View onCreateInputView()
	{
		Utils.DebugLog(Utils.LogType.INFO, "onCreateInputView started");

		initACRA();

		LayoutInflater layoutInflater = getLayoutInflater();
		if (layoutInflater == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "onCreateInputView interrupted as layoutInflater is null");
			return null;
		}

		currentView = layoutInflater.inflate(R.layout.input, null);
		if (currentView == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "onCreateInputView interrupted as currentView is null");
			return null;
		}

		if (vibrator == null) vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

		UnityInterface.getInstance().init(this);
		Localization.Instance.init(this);

		Utils.DebugLog(Utils.LogType.INFO, "onCreateInputView completed");

		return currentView;
	}

	@Override
	public void onStartInputView(EditorInfo info, boolean restarting)
	{
		Utils.DebugLog(Utils.LogType.INFO, "onStartInputView started");

		if (currentView == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "onStartInputView started but currentView is null");
		}

		if (mUnityPlayer == null)
		{
			mUnityPlayer = new UnityPlayer(this);
			int glesMode = mUnityPlayer.getSettings().getInt("gles_mode", 1);
			boolean trueColor8888 = false;
			mUnityPlayer.init(glesMode, trueColor8888);
		}
		if (mUnityPlayer == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "onStartInputView interrupted as mUnityPlayer is null");
			return;
		}

		eSharedPreferences = new EncryptedSharedPreferences(this);
		Boolean barOnBottom = eSharedPreferences.getBoolean("TopBarWentBottom", false);

		if (tondoBilling == null)
		{
			tondoBilling = new TondoBilling(getBaseContext(), eSharedPreferences);
			tondoBilling.InitializeAndStartBilling();
		}

		//To Do: maybe we should set this directly by unity adding a new method to the interface

		boolean useDarkTheme = useDarkTheme(eSharedPreferences);
		if (currentView != null)
		{
			if (useDarkTheme)
			{
				currentView.setBackgroundColor(barOnBottom ? getResources().getColor(R.color.KeyboardBackgroundDark) : getResources().getColor(R.color.KeyboardActionBarBackgroundDark));
			}
			else
			{
				currentView.setBackgroundColor(barOnBottom ? getResources().getColor(R.color.KeyboardBackgroundLight) : getResources().getColor(R.color.KeyboardActionBarBackgroundLight));
			}
		}
		//this set navigation bar background colour to the same colour as keyboard background but sometimes keyboard get stuck under the navigation bar
		//currentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			Dialog dialog = getWindow();
			if (dialog != null)
			{
				Window window = dialog.getWindow();
				if (window != null)
				{
					//To Do: maybe we should set this directly by unity adding a new method to the interface
					if (useDarkTheme)
					{
						window.setNavigationBarColor(getResources().getColor(barOnBottom ? R.color.KeyboarActionBarDirectionalBackgroundDark : R.color.KeyboardBackgroundDark));
					}
					else
					{
						window.setNavigationBarColor(getResources().getColor(barOnBottom ? R.color.KeyboarActionBarDirectionalBackgroundLight : R.color.KeyboardBackgroundLight));
					}

					View decorView = window.getDecorView();
					if (decorView != null)
					{
						int flags = decorView.getSystemUiVisibility();
						if (barOnBottom)
						{
							flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
						}
						else
						{
							if (useDarkTheme)
							{
								flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
							}
							else
							{
								flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
							}
						}
						decorView.setSystemUiVisibility(flags);
					}
				}
			}
		}

		FrameLayout currentLayout = null;
		if (currentView != null)
		{
			currentLayout = (FrameLayout) currentView.findViewById(R.id.frame_layout);
		}
		if (currentLayout == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "Cannot find current frame layout");
		}

		View playerView = mUnityPlayer.getView();
		if (playerView == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "Player view is null");
		}


		if (!restarting)
		{
			ViewGroup playerParentView = (ViewGroup) playerView.getParent();
			if (currentLayout != null && playerView != null)
			{
				if (playerParentView != null)
				{
					playerParentView.removeView(playerView);
				}
				WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(currentLayout.getWidth(), currentLayout.getHeight());
				currentLayout.addView(playerView, 0, layoutParams);
				setFrameLayoutParams(currentLayout);
			}
		}

		mUnityPlayer.resume();
		mUnityPlayer.windowFocusChanged(true);

		String precedingCharacter = GetPrecedingCharacter(1);
		UnityPlayer.UnitySendMessage("Native Interface", "OnStartInputView", precedingCharacter);

		int inputType = info.inputType & InputType.TYPE_MASK_CLASS;
		boolean numericalInputNeeded = inputType == InputType.TYPE_CLASS_NUMBER ||
				inputType == InputType.TYPE_CLASS_DATETIME ||
				inputType == InputType.TYPE_CLASS_PHONE;
		boolean passwordInputNeeded = inputType == InputType.TYPE_CLASS_TEXT &&
				(info.inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD) > 0;

		int result = numericalInputNeeded ? NUMERICAL_INPUT : TEXT_INPUT;
		if (passwordInputNeeded) result |= PASSWORD_INPUT;

		UnityPlayer.UnitySendMessage("Native Interface", "OnSetInputType", String.valueOf(result));

		Utils.DebugLog(Utils.LogType.INFO, "onStartInputView completed.");
	}

	private boolean useDarkTheme(EncryptedSharedPreferences eSharedPreferences)
	{
		String themeType = eSharedPreferences.getString("ThemeType", "0");
		//To Do: this really sucks.
		if (themeType.equals("0"))
		{
			return GetNightModeOn();
		}
		else
		{
			return themeType.equals("2");
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		Utils.DebugLog(Utils.LogType.INFO, "onConfigurationChanged with orientation: " + orientationToString(newConfig.orientation));

		super.onConfigurationChanged(newConfig);

		if (mUnityPlayer == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "onConfigurationChanged interrupted as mUnityPlayer is null");
			return;
		}

		mUnityPlayer.configurationChanged(newConfig);
		UnityPlayer.UnitySendMessage("Native Interface", "OnConfigurationChanged", "");
	}

	private String orientationToString(int orientation)
	{
		switch (orientation)
		{
			case Configuration.ORIENTATION_UNDEFINED:
				return "UNDEFINED";
			case Configuration.ORIENTATION_PORTRAIT:
				return "PORTRAIT";
			case Configuration.ORIENTATION_LANDSCAPE:
				return "LANDSCAPE";
			case Configuration.ORIENTATION_SQUARE:
				return "SQUARE";
			default:
				return orientation + "";
		}
	}

	@Override
	public void onLowMemory()
	{
		Utils.DebugLog(Utils.LogType.INFO, "onLowMemory called");

		if (mUnityPlayer == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "onLowMemory called but mUnityPlayer is null");
		}
		else
		{
			mUnityPlayer.lowMemory();
		}
		super.onLowMemory();
	}

	@Override
	public void onTrimMemory(int level)
	{
		Utils.DebugLog(Utils.LogType.INFO, "onTrimMemory called with level: " + level);

		if (mUnityPlayer == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "onTrimMemory called but mUnityPlayer is null");
		}
		else
		{
			switch (level)
			{
				case TRIM_MEMORY_RUNNING_MODERATE:
				case TRIM_MEMORY_RUNNING_LOW:
					break;

				case TRIM_MEMORY_RUNNING_CRITICAL:
				case TRIM_MEMORY_UI_HIDDEN:
				case TRIM_MEMORY_BACKGROUND:
				case TRIM_MEMORY_MODERATE:
				case TRIM_MEMORY_COMPLETE:
				default:
					mUnityPlayer.lowMemory();
					break;
			}
		}
		super.onTrimMemory(level);
	}

	@Override
	public void onWindowHidden()
	{
		Utils.DebugLog(Utils.LogType.INFO, "onWindowHidden called");

		if (mUnityPlayer == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "onWindowHidden called but mUnityPlayer is null");
		}
		else
		{
			mUnityPlayer.pause();
		}
	}

	@Override
	public void onDestroy()
	{
		Utils.DebugLog(Utils.LogType.INFO, "onDestroy called");

		if (mUnityPlayer == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "onDestroy called but mUnityPlayer is null");
		}
		else
		{
			mUnityPlayer.destroy();
		}

		super.onDestroy();
	}

	@Override
	public void updateFullscreenMode()
	{
		Utils.DebugLog(Utils.LogType.INFO, "updateFullscreenMode called");

		// this is a workaround to fix a InputMethodService bug that prevents text selection on landscape.
		// if InputMethodService updateFullscreenMode is called when the device is in landscape, isFullscreen flag goes to true and onCreateExtractTextView is called.
		// the newly created ExtractTextView then gets hidden if editor type asks not to show ime in full screen, but at that point text selection is already set on the hidden ExtractTextView

		EditorInfo editorInfo = getCurrentInputEditorInfo();
		if (editorInfo != null)
		{
			if (editorInfo.inputType == InputType.TYPE_NULL || (editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_EXTRACT_UI) != 0)
			{
				Utils.DebugLog(Utils.LogType.INFO, "Override updateFullscreenMode is avoiding to call super.updateFullscreenMode");
				return;
			}
		}

		Utils.DebugLog(Utils.LogType.INFO, "Override updateFullscreenMode is going to call super.updateFullscreenMode");
		super.updateFullscreenMode();
	}

	@Override
	public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd)
	{
		super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
		String precedingCharacter = GetPrecedingCharacter(1);
		UnityPlayer.UnitySendMessage("Native Interface", "OnCursorPositionChanged", precedingCharacter);
	}

	/*@Override
	public void onDisplayCompletions(CompletionInfo[] infos)
	{

	}*/

	/*@Override
	public View onCreateCandidatesView()
	{
		super.onCreateCandidatesView();
		LayoutInflater layoutInflater = getLayoutInflater();
		View candidateView = layoutInflater.inflate(R.layout.candidate_view, null);
		LinearLayout layout = (LinearLayout) candidateView.findViewById(R.id.linear_layout);

		setCandidatesViewShown(true);

		return candidateView;
	}*/

	/*@Override
	public void onComputeInsets(InputMethodService.Insets outInsets)
	{
		//this is a workaround to fix a problem when using the candidate view
		super.onComputeInsets(outInsets);
		if (!isFullscreenMode())
		{
			outInsets.contentTopInsets = outInsets.visibleTopInsets;
		}
	}*/

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
					builder.getPluginConfigurationBuilder(org.acra.config.NotificationConfigurationBuilder.class)
							.setEnabled(true)
							.setSendOnClick(true)
							.setResTitle(R.string.acra_notification_title)
							.setResText(R.string.acra_notification_text)
							.setResChannelName(R.string.acra_notification_channel);
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
					Utils.DebugLog(Utils.LogType.ERROR, "Exception caught while trying to get dictionary bytes.\nException message: " + e.getMessage());
				}
			}
		}
	}

	private void setFrameLayoutParams()
	{
		Utils.DebugLog(Utils.LogType.INFO, "setFrameLayoutParams called");

		if (currentView == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "setFrameLayoutParams interrupted as currentView is null");
			return;
		}
		FrameLayout layout = currentView.findViewById(R.id.frame_layout);
		setFrameLayoutParams(layout);
	}

	private void setFrameLayoutParams(FrameLayout layout)
	{
		Utils.DebugLog(Utils.LogType.INFO, "setFrameLayoutParams(layout) started. keyboardProportion: " + keyboardProportion);

		if (layout == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "setFrameLayoutParams interrupted as layout is null");
			return;
		}

		Context applicationContext = getApplicationContext();
		if (applicationContext == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "setFrameLayoutParams interrupted as applicationContext is null");
			return;
		}

		Resources resources = applicationContext.getResources();
		if (resources == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "setFrameLayoutParams interrupted as resources is null");
			return;
		}

		DisplayMetrics metrics = resources.getDisplayMetrics();
		if (metrics == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "setFrameLayoutParams interrupted as metrics is null");
			return;
		}
		Configuration configuration = resources.getConfiguration();
		if (configuration == null)
		{
			Utils.DebugLog(Utils.LogType.WARNING, "setFrameLayoutParams interrupted as configuration is null");
			return;
		}

		int orientation = configuration.orientation;

		//metric.widthPixels and metrics.heightPixels are inverted on some devices but orientation seems to be always correct.
		//we need to decide which dimension is the real width and which is the real height based on orientation

		int metricsW = metrics.widthPixels;
		int metricsH = metrics.heightPixels;

		int displayWidth = metricsW;
		int displayHeight = metricsH;

		if (orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			displayWidth = metricsW > metricsH ? metricsW : metricsH;
			displayHeight = metricsW > metricsH ? metricsH : metricsW;
		}
		else if (orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			displayWidth = metricsW > metricsH ? metricsH : metricsW;
			displayHeight = metricsW > metricsH ? metricsW : metricsH;
		}

		int keyboardWidth = displayWidth;
		int keyboardHeight = Math.round(keyboardWidth / keyboardProportion);
		if (keyboardHeight > displayHeight / maxHeightProportion)
		{
			keyboardHeight = Math.round(displayHeight / maxHeightProportion);
		}

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(keyboardWidth, keyboardHeight);
		layout.setLayoutParams(params);

		Utils.DebugLog(Utils.LogType.INFO, "setFrameLayoutParams(layout) completed." +
				"orientation: " + orientationToString(orientation) +
				" keyboard width: " + keyboardWidth +
				" keyboard height: " + keyboardHeight);
	}

	public void HandleException()
	{
		if (BuildConfig.DEBUG_MODE)
		{
			if (org.acra.ACRA.isInitialised())
			{
				org.acra.ACRA.getErrorReporter().handleException(null);
			}
		}
	}

	public String GetSuggestionsLanguage()
	{
		String defaultLanguageValue = getResources().getString(R.string.suggestion_language_default_value);
		String language = eSharedPreferences.getString("SuggestionsLanguage", defaultLanguageValue);
		return language.equals("_") ? defaultLanguageValue : language;
	}

	private Locale getSuggestionsLocale()
	{
		String[] lang = GetSuggestionsLanguage().split("_");
		if (lang.length >= 2) return new Locale(lang[0], lang[1]);
		else return new Locale(lang[0]);
	}

	private int[] getCaretWordBoundaries(ExtractedText et)
	{
		String text = et.text.toString();
		int start = et.selectionStart;
		int end = et.selectionEnd;
		BreakIterator iterator = BreakIterator.getWordInstance(getSuggestionsLocale());
		iterator.setText(text);
		if (!iterator.isBoundary(start) || start == text.length())
			start = iterator.preceding(start);
		if (!iterator.isBoundary(end) || end == start) end = iterator.following(end);
		return new int[]{start, end};
	}

	private String getCaretWord()
	{
		InputConnection inputConnection = getCurrentInputConnection();
		ExtractedText et = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
		String textString = et == null ? "" : et.text.toString();
		if (textString.length() == 0) return "";
		int[] bounds = getCaretWordBoundaries(et);
		return textString.substring(bounds[0], bounds[1]).trim();
	}

	public void replaceCaretWord(String newWord)
	{
		InputConnection inputConnection = getCurrentInputConnection();
		ExtractedText et = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
		int[] bounds = getCaretWordBoundaries(et);
		inputConnection.setComposingRegion(et.startOffset + bounds[0], et.startOffset + bounds[1]);
		inputConnection.setComposingText(newWord, 1);
		inputConnection.finishComposingText();
		CommitString(" ", false);
	}

	public void CommitString(String s, boolean asSession)
	{
		InputConnection inputConnection = getCurrentInputConnection();
		ExtractedText text = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
		if (asSession)
		{
			mIsCommittingAsSession = true;
			inputConnection.setComposingText(s, 1);
		}
		else
		{
			TerminateStringSessionIfAny();
			inputConnection.commitText(s, 1);
			UnityPlayer.UnitySendMessage("Native Interface", "OnEditingWordChange", getCaretWord());
		}
	}

	public void TerminateStringSessionIfAny()
	{
		if (mIsCommittingAsSession)
		{
			mIsCommittingAsSession = false;
			InputConnection connection = getCurrentInputConnection();
			if (connection != null) connection.finishComposingText();
		}
	}

	public void CommitEmoji(String s)
	{
		InputConnection inputConnection = getCurrentInputConnection();
		inputConnection.commitText(s, 1);
	}

	public void ReplaceLastCharacterWith(String s)
	{
		InputConnection inputConnection = getCurrentInputConnection();

		ExtractedText extractedText = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
		int cursorIndex = extractedText.selectionStart;
		inputConnection.setSelection(cursorIndex - s.length(), cursorIndex);

		inputConnection.commitText(s, s.length());
	}

	public void CommitBackspace()
	{
		InputConnection inputConnection = getCurrentInputConnection();
		inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
		UnityPlayer.UnitySendMessage("Native Interface", "OnEditingWordChange", getCaretWord());
	}

	public void CommitDone()
	{
		InputConnection inputConnection = getCurrentInputConnection();

		int actionFlag = getCurrentInputEditorInfo().imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION);


		Utils.DebugLog(Utils.LogType.ERROR, "CommitDone  ime action is:" + actionFlag);

		if (actionFlag == EditorInfo.IME_ACTION_DONE ||
				actionFlag == EditorInfo.IME_ACTION_NEXT ||
				actionFlag == EditorInfo.IME_ACTION_PREVIOUS ||
				actionFlag == EditorInfo.IME_ACTION_GO ||
				actionFlag == EditorInfo.IME_ACTION_SEARCH ||
				actionFlag == EditorInfo.IME_ACTION_SEND)
		{
			Utils.DebugLog(Utils.LogType.ERROR, "performing Editor Action");

			inputConnection.performEditorAction(actionFlag);
		}
		else
		{
			Utils.DebugLog(Utils.LogType.ERROR, "sending KeyEvent");

			inputConnection.commitText("\n", 1);
			//inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
			UnityPlayer.UnitySendMessage("Native Interface", "OnEditingWordChange", getCaretWord());
		}
	}

	public int getInputType()
	{
		EditorInfo info = getCurrentInputEditorInfo();
		return info.inputType;
	}

	public int GetDoneActionFlag()
	{
		EditorInfo editorInfo = getCurrentInputEditorInfo();

		switch (editorInfo.imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION))
		{
			case EditorInfo.IME_ACTION_DONE:
				if ((editorInfo.inputType & EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) == EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE)
				{
					return DONE_ACTION_FLAG_NEWLINE;
				}
				else
				{
					return DONE_ACTION_FLAG_DONE;
				}
			case EditorInfo.IME_ACTION_NEXT:
				return DONE_ACTION_FLAG_DONE;
			case EditorInfo.IME_ACTION_PREVIOUS:
				return DONE_ACTION_FLAG_DONE;
			case EditorInfo.IME_ACTION_GO:
				return DONE_ACTION_FLAG_SEND;
			case EditorInfo.IME_ACTION_SEARCH:
				return DONE_ACTION_FLAG_SEARCH;
			case EditorInfo.IME_ACTION_SEND:
				return DONE_ACTION_FLAG_SEND;
			case EditorInfo.IME_ACTION_UNSPECIFIED:
				return DONE_ACTION_FLAG_NEWLINE;
			case EditorInfo.IME_ACTION_NONE:
				return DONE_ACTION_FLAG_NEWLINE;
			default:
				return DONE_ACTION_FLAG_NEWLINE;
		}
	}

	public void OpenSystemKeyboardChooser()
	{
		InputMethodManager imeManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imeManager != null)
		{
			imeManager.showInputMethodPicker();
		}
	}

	public void OpenKeyboardOptions()
	{
		Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.foschia.tondokeyboard");
		if (launchIntent != null)
		{
			launchIntent.putExtra(MainActivity.SETTINGS_INTENT_MESSAGE, 1);
			startActivity(launchIntent);
		}
	}

	public void HideKeyboard()
	{
		requestHideSelf(0);
	}

	public void Vibrate(long milliseconds)
	{
		vibrator.vibrate(milliseconds);
	}

	//region Cursor
	public void moveCursor(int entity, Direction direction)
	{
		int keyEvent = direction.asKey();
		InputConnection inputConnection = getCurrentInputConnection();
		CharSequence precedingChars = inputConnection.getTextBeforeCursor(1, 0);
		CharSequence followingChars = inputConnection.getTextAfterCursor(1, 0);
		boolean noPrecedingChars = precedingChars == null || precedingChars.length() == 0;
		boolean noFollowingChars = followingChars == null || followingChars.length() == 0;
		if ((direction == Direction.LEFT || direction == Direction.UP) && noPrecedingChars) return;
		if ((direction == Direction.RIGHT || direction == Direction.DOWN) && noFollowingChars)
			return;

		for (int i = 0; i < entity; i++)
		{
			inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEvent));
			inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEvent));
		}
	}
//endregion

	//region CTRL
	public void Copy()
	{
		InputConnection inputConnection = getCurrentInputConnection();
		inputConnection.performContextMenuAction(android.R.id.copy);
	}

	public void Paste()
	{
		InputConnection inputConnection = getCurrentInputConnection();
		inputConnection.performContextMenuAction(android.R.id.paste);
	}

	public void Cut()
	{
		InputConnection inputConnection = getCurrentInputConnection();
		inputConnection.performContextMenuAction(android.R.id.cut);
	}

	public void SelectAll()
	{
		InputConnection inputConnection = getCurrentInputConnection();
		inputConnection.performContextMenuAction(android.R.id.selectAll);
	}

	public void Undo()
	{
		InputConnection inputConnection = getCurrentInputConnection();
		inputConnection.performContextMenuAction(android.R.id.undo);
	}

	public void Redo()
	{
		InputConnection inputConnection = getCurrentInputConnection();
		inputConnection.performContextMenuAction(android.R.id.redo);
	}
//endregion

	public void UpdateProportions(float proportion)
	{
		Utils.DebugLog(Utils.LogType.INFO, "UpdateProportions called with proportion: " + proportion + ". current proportion is: " + keyboardProportion);

		keyboardProportion = proportion;
		this.currentView.post(new Runnable()
		{
			public void run()
			{
				setFrameLayoutParams();
			}
		});
	}

	public String GetPrecedingCharacter(int n)
	{
		InputConnection inputConnection = getCurrentInputConnection();
		if (inputConnection == null) return "";
		CharSequence charSequence = inputConnection.getTextBeforeCursor(n, 0);
		String precedingCharacter = charSequence == null ? "" : charSequence.toString();
		return precedingCharacter;
	}

	public String GetFollowingCharacter(int n)
	{
		InputConnection inputConnection = getCurrentInputConnection();
		if (inputConnection == null) return "";
		CharSequence charSequence = inputConnection.getTextAfterCursor(n, 0);
		String followingCharacter = charSequence == null ? "" : charSequence.toString();
		return followingCharacter;
	}

	public void showToast(String msg)
	{
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	public boolean GetNightModeOn()
	{
		int nightModeFlags = getApplicationContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
	}

	public boolean isPermissionGranted(@NonNull final String permission)
	{
		int check = ContextCompat.checkSelfPermission(this, permission);
		return check == PackageManager.PERMISSION_GRANTED;
	}

	public void requestPermissions(String... permissions)
	{
		requestPermissions(null, permissions);
	}

	public void requestPermissions(Consumer<Integer> callback, String... permissions)
	{
		List<String> required = new ArrayList<>();
		for (int i = 0; i < permissions.length; i++)
		{
			String p = permissions[i];
			if (!isPermissionGranted(p)) required.add(p);
		}

		if (required.size() == 0) return;

		String[] permissionsArray = required.toArray(new String[required.size()]);

		Intent intent = new Intent(this, PermissionsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.putExtra(PermissionsActivity.PERMISSIONS_EXTRA, permissionsArray);

		ActivityCompat.PermissionCompatDelegate delegate =
				new ActivityCompat.PermissionCompatDelegate()
				{
					@Override
					public boolean requestPermissions(@NonNull Activity activity, @NonNull String[] permissions, int requestCode)
					{
						return false;
					}

					@Override
					public boolean onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, @Nullable Intent data)
					{
						if (callback != null) callback.accept(resultCode);
						ActivityCompat.setPermissionCompatDelegate(null);
						return true;
					}
				};

		ActivityCompat.setPermissionCompatDelegate(delegate);

		startActivity(intent);
	}
}

