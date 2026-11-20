package com.foschia.tondokeyboard;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.foschia.tondo.android.AndroidFileStore;
import com.foschia.tondo.android.TondoKeyboardView;
import com.foschia.tondo.core.CtrlAction;
import com.foschia.tondo.core.CursorDirection;
import com.foschia.tondo.core.KeyboardEngine;
import com.foschia.tondo.core.KeyboardHost;
import com.foschia.tondo.core.PreferenceStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Bridge between the input method service and the shared tOndO keyboard engine.
 * It replaces UnityInterface: the same responsibilities (text editing, clipboard, speech, preferences)
 * but exposed through the plain {@link KeyboardHost} interface instead of UnitySendMessage/JNI.
 */
@Keep
public class TondoInterface implements KeyboardHost
{
	private static TondoInterface instance = null;
	private KeyboardActionListener keyboardActionListener;
	private SpeechRecognizer mSpeechRecognizer;
	private EncryptedSharedPreferences eSharedPreferences;
	private ClipboardManager mClipboardManager;
	private KeyboardEngine engine;
	private TondoKeyboardView view;

	public static final String PURCHASED_PRO_PREFERENCES_KEY = "PURCHASED_PRO_V1";

	private TondoInterface()
	{
	}

	public static TondoInterface getInstance()
	{
		if (instance == null)
		{
			instance = new TondoInterface();
		}
		return instance;
	}

	public KeyboardEngine getEngine()
	{
		return engine;
	}

	/** Creates the engine and its view. Called from onCreateInputView. */
	public TondoKeyboardView init(KeyboardActionListener keyboardActionListener)
	{
		this.keyboardActionListener = keyboardActionListener;
		this.eSharedPreferences = new EncryptedSharedPreferences(keyboardActionListener);
		this.mClipboardManager = (ClipboardManager) keyboardActionListener.getSystemService(Context.CLIPBOARD_SERVICE);
		if (this.mClipboardManager != null)
		{
			this.mClipboardManager.addPrimaryClipChangedListener(() ->
			{
				if (engine != null) engine.onClipboardChanged(clipboardText());
			});
		}
		engine = new KeyboardEngine(this, new Preferences(eSharedPreferences), new AndroidFileStore(keyboardActionListener));
		view = new TondoKeyboardView(keyboardActionListener, engine);
		return view;
	}

	public void HandleException()
	{
		keyboardActionListener.HandleException();
	}

	//region KeyboardHost: text editing
	@Override
	public void commitText(@NonNull String s)
	{
		keyboardActionListener.CommitString(s, false);
	}

	@Override
	public void commitEmoji(@NonNull String s)
	{
		keyboardActionListener.CommitEmoji(s);
	}

	@Override
	public void replaceLastCharacters(@NonNull String s)
	{
		keyboardActionListener.ReplaceLastCharacterWith(s);
	}

	@Override
	public void commitBackspace()
	{
		keyboardActionListener.CommitBackspace();
	}

	@Override
	public void commitDone()
	{
		keyboardActionListener.CommitDone();
	}

	@Override
	public void commitCorrection(@NonNull String word)
	{
		keyboardActionListener.replaceCaretWord(word);
	}

	@NonNull
	@Override
	public String textBeforeCursor(int n)
	{
		return keyboardActionListener.GetPrecedingCharacter(n);
	}

	@Override
	public void moveCursor(@NonNull CursorDirection direction, int count)
	{
		switch (direction)
		{
			case LEFT:
				keyboardActionListener.moveCursor(count, KeyboardActionListener.Direction.LEFT);
				break;
			case RIGHT:
				keyboardActionListener.moveCursor(count, KeyboardActionListener.Direction.RIGHT);
				break;
			case UP:
				keyboardActionListener.moveCursor(count, KeyboardActionListener.Direction.UP);
				break;
			case DOWN:
				keyboardActionListener.moveCursor(count, KeyboardActionListener.Direction.DOWN);
				break;
		}
	}

	@Override
	public void performCtrl(@NonNull CtrlAction action)
	{
		switch (action)
		{
			case COPY_TEXT:
				keyboardActionListener.Copy();
				break;
			case PASTE_TEXT:
				keyboardActionListener.Paste();
				break;
			case CUT_TEXT:
				keyboardActionListener.Cut();
				break;
			case SELECT_ALL:
				keyboardActionListener.SelectAll();
				break;
			case UNDO:
				keyboardActionListener.Undo();
				break;
			case REDO:
				keyboardActionListener.Redo();
				break;
		}
	}
	//endregion

	//region KeyboardHost: system
	@Override
	public void openKeyboardChooser()
	{
		keyboardActionListener.OpenSystemKeyboardChooser();
	}

	@Override
	public void openSettings()
	{
		keyboardActionListener.OpenKeyboardOptions();
	}

	@Override
	public void hideKeyboard()
	{
		keyboardActionListener.HideKeyboard();
	}

	@Override
	public void vibrate(long milliseconds)
	{
		keyboardActionListener.Vibrate(milliseconds);
	}

	@NonNull
	@Override
	public String clipboardText()
	{
		if (mClipboardManager == null || !mClipboardManager.hasPrimaryClip()) return "";

		CharSequence seq = mClipboardManager.getPrimaryClip()
				.getItemAt(0).coerceToText(keyboardActionListener);

		if (seq == null) return "";
		return seq.toString();
	}

	@Override
	public boolean isPro()
	{
		return eSharedPreferences.getEncryptedBoolean(PURCHASED_PRO_PREFERENCES_KEY, false);
	}

	@NonNull
	@Override
	public String localize(@NonNull String key)
	{
		return Localization.Instance.localize(key);
	}

	@Override
	public void showToast(@NonNull String message)
	{
		keyboardActionListener.showToast(message);
	}

	@Override
	public void setKeyboardProportion(float proportion)
	{
		keyboardActionListener.UpdateProportions(proportion);
	}

	@Override
	public void requestRender()
	{
		if (view != null) view.requestRender();
	}
	//endregion

	//region Dictionaries
	@Override
	public byte[] readDictionary(@NonNull String fileName)
	{
		AssetManager mngr = keyboardActionListener.getAssets();
		try
		{
			List<String> files = Arrays.asList(mngr.list("dictionaries"));
			if (!files.contains(fileName))
			{
				String defaultLanguageValue = keyboardActionListener.getResources().getString(R.string.suggestion_language_default_value);
				fileName = defaultLanguageValue + ".dic";
			}
		}
		catch (IOException e)
		{
			Utils.DebugLog(Utils.LogType.ERROR, "Exception caught while listing dictionaries: " + e.getMessage());
			return null;
		}

		try (InputStream in = mngr.open("dictionaries/" + fileName, AssetManager.ACCESS_STREAMING);
			 ByteArrayOutputStream bos = new ByteArrayOutputStream())
		{
			Utils.streamToStream(in, bos);
			Utils.DebugLog(Utils.LogType.INFO, "Loaded " + fileName + " dictionary");
			return bos.toByteArray();
		}
		catch (IOException e)
		{
			Utils.DebugLog(Utils.LogType.ERROR, "Exception caught while trying to get dictionary bytes.\nException message: " + e.getMessage());
			return null;
		}
	}
	//endregion

	//region Speech Recognition
	@Override
	public void startSpeechRecognition()
	{
		if (mSpeechRecognizer != null) mSpeechRecognizer.destroy();
		keyboardActionListener.getCurrentView().post(() ->
		{
			mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(keyboardActionListener);
			mSpeechRecognizer.setRecognitionListener(new TondoRecognitionListener(this));
			Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
			speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
			speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
					RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			mSpeechRecognizer.startListening(speechRecognizerIntent);
		});
	}

	@Override
	public void cancelSpeechRecognition()
	{
		if (mSpeechRecognizer == null) return;
		keyboardActionListener.getCurrentView().post(() ->
		{
			SendSpeechError(SpeechRecognizer.ERROR_CLIENT);
			mSpeechRecognizer.destroy();
			mSpeechRecognizer = null;
		});
	}

	@Override
	public boolean hasRecordAudioPermission()
	{
		return keyboardActionListener.isPermissionGranted(Manifest.permission.RECORD_AUDIO);
	}

	@Override
	public void requestRecordAudioPermission()
	{
		if (!hasRecordAudioPermission())
		{
			keyboardActionListener.requestPermissions(
					code -> { },
					Manifest.permission.RECORD_AUDIO
			);
		}
	}

	public void SendSpeechPartialData(ArrayList<String> data)
	{
		keyboardActionListener.CommitString(data.get(0), true);
	}

	public void SendSpeechData(ArrayList<String> data)
	{
		keyboardActionListener.CommitString(data.get(0), true);
		keyboardActionListener.TerminateStringSessionIfAny();
	}

	public void SendSpeechError(int error)
	{
		Resources r = keyboardActionListener.getResources();
		String errorMsg;
		switch (error)
		{
			case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
				errorMsg = r.getString(R.string.recognition_error_network_timeout);
				break;
			case SpeechRecognizer.ERROR_NETWORK:
				errorMsg = r.getString(R.string.recognition_error_network);
				break;
			case SpeechRecognizer.ERROR_AUDIO:
				errorMsg = r.getString(R.string.recognition_error_audio);
				break;
			case SpeechRecognizer.ERROR_SERVER:
				errorMsg = r.getString(R.string.recognition_error_server);
				break;
			case SpeechRecognizer.ERROR_CLIENT:
				errorMsg = "";
				break;
			case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
				errorMsg = r.getString(R.string.recognition_error_speech_timeout);
				break;
			case SpeechRecognizer.ERROR_NO_MATCH:
				errorMsg = r.getString(R.string.recognition_error_no_match);
				break;
			case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
				errorMsg = r.getString(R.string.recognition_error_recognizer_busy);
				break;
			case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
				errorMsg = r.getString(R.string.recognition_error_insufficient_permissions);
				break;
			case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS:
				errorMsg = r.getString(R.string.recognition_error_too_many_requests);
				break;
			case SpeechRecognizer.ERROR_SERVER_DISCONNECTED:
				errorMsg = r.getString(R.string.recognition_error_server_disconnected);
				break;
			case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
				errorMsg = r.getString(R.string.recognition_error_language_not_supported);
				break;
			case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
				errorMsg = r.getString(R.string.recognition_error_language_unavailable);
				break;
			default:
				errorMsg = r.getString(R.string.recognition_error_unknown);
				break;
		}

		if (engine != null) engine.onSpeechError();
		keyboardActionListener.TerminateStringSessionIfAny();
		if (!errorMsg.equals("")) keyboardActionListener.showToast(errorMsg);
	}

	public void SendAudioLevel(float level)
	{
		if (engine != null) engine.onSpeechAudioLevel(level);
	}

	public void TriggerOnReadyForSpeech()
	{
		if (engine != null) engine.onSpeechReady();
	}

	public void TriggerStartOfSpeech()
	{
	}

	public void TriggerEndOfSpeech()
	{
		if (engine != null) engine.onSpeechEnd();
	}
	//endregion

	/** Shared engine preferences backed by the existing (obfuscated) shared preferences. */
	private static final class Preferences implements PreferenceStore
	{
		private final EncryptedSharedPreferences prefs;

		Preferences(EncryptedSharedPreferences prefs)
		{
			this.prefs = prefs;
		}

		@NonNull
		@Override
		public String getString(@NonNull String key, @NonNull String def)
		{
			return prefs.getString(key, def);
		}

		@Override
		public int getInt(@NonNull String key, int def)
		{
			return prefs.getInt(key, def);
		}

		@Override
		public boolean getBoolean(@NonNull String key, boolean def)
		{
			return prefs.getBoolean(key, def);
		}

		@Override
		public void putString(@NonNull String key, @NonNull String value)
		{
			prefs.put(key, value);
		}

		@Override
		public void putInt(@NonNull String key, int value)
		{
			prefs.put(key, value);
		}

		@Override
		public void putBoolean(@NonNull String key, boolean value)
		{
			prefs.put(key, value);
		}

		@Override
		public boolean contains(@NonNull String key)
		{
			return prefs.contains(key);
		}
	}
}
