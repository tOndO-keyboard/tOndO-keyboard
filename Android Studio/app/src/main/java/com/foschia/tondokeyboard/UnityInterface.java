package com.foschia.tondokeyboard;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.Keep;

import com.unity3d.player.UnityPlayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Keep
public class UnityInterface
{
	private static UnityInterface instance = null;
	private KeyboardActionListener keyboardActionListener;
	private SpeechRecognizer mSpeechRecognizer;
	private EncryptedSharedPreferences eSharedPreferences;
	private ClipboardManager mClipboardManager;
	private final List<SpeechListener> speechListeners = new ArrayList<>();

	public static final String PURCHASED_PRO_PREFERENCES_KEY = "PURCHASED_PRO_V1";

	private UnityInterface()
	{
	}

	public static UnityInterface getInstance()
	{
		if (instance == null)
		{
			instance = new UnityInterface();
		}
		return instance;
	}

	public void init(KeyboardActionListener keyboardActionListener)
	{
		this.keyboardActionListener = keyboardActionListener;
		this.eSharedPreferences = new EncryptedSharedPreferences(keyboardActionListener);
		this.mClipboardManager = (ClipboardManager) keyboardActionListener.getSystemService(Context.CLIPBOARD_SERVICE);
		if (this.mClipboardManager != null)
		{
			this.mClipboardManager.addPrimaryClipChangedListener(() ->
			{
				UnityPlayer.UnitySendMessage("NativeInterface", "OnClipboardChanged", GetClipboardContent());
			});
		}
	}

	public void HandleException()
	{
		keyboardActionListener.HandleException();
	}

	public void CommitString(String s)
	{
		keyboardActionListener.CommitString(s, false);
	}

	public void CommitEmoji(String s)
	{
		keyboardActionListener.CommitEmoji(s);
	}

	public void ReplaceLastCharacterWith(String s)
	{
		keyboardActionListener.ReplaceLastCharacterWith(s);
	}

	public void CommitBackspace()
	{
		keyboardActionListener.CommitBackspace();
	}

	public void CommitDone()
	{
		keyboardActionListener.CommitDone();
	}

	public int GetDoneActionFlag()
	{
		return keyboardActionListener.GetDoneActionFlag();
	}

	public void OpenSystemKeyboardChooser()
	{
		keyboardActionListener.OpenSystemKeyboardChooser();
	}

	public void OpenKeyboardOptions()
	{
		keyboardActionListener.OpenKeyboardOptions();
	}

	public void HideKeyboard()
	{
		keyboardActionListener.HideKeyboard();
	}

	public void Vibrate(long milliseconds)
	{
		keyboardActionListener.Vibrate(milliseconds);
	}

	public void SetKeyboardProportion(float proportion)
	{
		keyboardActionListener.UpdateProportions(proportion);
	}

	public String GetPrecedingCharacter(int n)
	{
		return keyboardActionListener.GetPrecedingCharacter(n);
	}

	public String GetFollowingCharacter(int n)
	{
		return keyboardActionListener.GetFollowingCharacter(n);
	}

	public String GetClipboardContent()
	{
		if (mClipboardManager == null || !mClipboardManager.hasPrimaryClip()) return "";

		CharSequence seq = mClipboardManager.getPrimaryClip()
				.getItemAt(0).coerceToText(keyboardActionListener);

		if (seq == null) return "";
		return seq.toString();
	}

	public boolean GetNightModeOn()
	{
		return keyboardActionListener.GetNightModeOn();
	}

	public boolean isLandscape()
	{
		int orientation = keyboardActionListener.getResources().getConfiguration().orientation;
		return orientation == Configuration.ORIENTATION_LANDSCAPE;
	}

	public boolean isPro()
	{
		return eSharedPreferences.getEncryptedBoolean(PURCHASED_PRO_PREFERENCES_KEY, false);
	}

	//region Dictionaries
	public byte[] GetDictionaryBytes()
	{
		AssetManager mngr = keyboardActionListener.getAssets();
		String defaultLanguageValue = keyboardActionListener.getResources().getString(R.string.suggestion_language_default_value);
		String lang = keyboardActionListener.GetSuggestionsLanguage();

		try
		{
			List<String> files = Arrays.asList(mngr.list("dictionaries"));
			if (!files.contains(lang + ".dic") || !files.contains(lang + ".aff"))
				lang = defaultLanguageValue;
		}
		catch (IOException e)
		{
			Utils.DebugLog(Utils.LogType.ERROR, "Exception caught while trying to get dictionary bytes.\nException message: " + e.getMessage());
			return new byte[0];
		}

		String dic = "dictionaries/" + lang + ".dic";
		String aff = "dictionaries/" + lang + ".aff";
		try (InputStream sdic = mngr.open(dic, AssetManager.ACCESS_STREAMING);
			 InputStream saff = mngr.open(aff, AssetManager.ACCESS_STREAMING);
			 ByteArrayOutputStream bos = new ByteArrayOutputStream())
		{
			bos.write(Utils.intToBytesArray(0));
			bos.write(Utils.intToBytesArray(0));
			int dicLen = Utils.streamToStream(sdic, bos);
			int affLen = Utils.streamToStream(saff, bos);
			byte[] result = bos.toByteArray();
			System.arraycopy(Utils.intToBytesArray(dicLen), 0, result, 0, Integer.BYTES);
			System.arraycopy(Utils.intToBytesArray(affLen), 0, result, Integer.BYTES, Integer.BYTES);
			Log.i("DICTIONARIES", "Loaded " + lang + " dictionary");

			Utils.DebugLog(Utils.LogType.INFO, "Loaded " + lang + " dictionary");
			return result;
		}
		catch (IOException e)
		{

			Utils.DebugLog(Utils.LogType.ERROR, "Exception caught while trying to get dictionary bytes.\nException message: " + e.getMessage());
			return new byte[0];
		}
	}

	public void CommitCorrection(String word)
	{
		keyboardActionListener.replaceCaretWord(word);
	}

	//endregion

	//region Speech Recognition
	public void RegisterSpeechListener(SpeechListener listener)
	{
		speechListeners.add(listener);
	}

	public void UnregisterSpeechListener(SpeechListener listener)
	{
		speechListeners.remove(listener);
	}

	public void StartTranscribingSpeech()
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

	public void CancelTranscribingSpeech()
	{
		if (mSpeechRecognizer == null) return;
		keyboardActionListener.getCurrentView().post(() ->
		{
			//mSpeechRecognizer.cancel();
			SendSpeechError(SpeechRecognizer.ERROR_CLIENT);
			mSpeechRecognizer.destroy();
		});
	}

	public boolean HasRecordAudioPermissions()
	{
		return keyboardActionListener.isPermissionGranted(Manifest.permission.RECORD_AUDIO);
	}

	public void RequestRecordAudioPermissions()
	{
		if (!HasRecordAudioPermissions())
		{
			keyboardActionListener.requestPermissions(
					this::SendSpeechPermissionsResult,
					Manifest.permission.RECORD_AUDIO
			);
		}
	}

	//TO DO following methods in this region are not used from within Unity player.
	//They should be moved to a proper class.

	private void SendSpeechPermissionsResult(int code)
	{
	}

	public void SendSpeechPartialData(ArrayList<String> data)
	{
		for (SpeechListener l : speechListeners)
			l.onPartialData(data.get(0));
		keyboardActionListener.CommitString(data.get(0), true);
	}

	public void SendSpeechData(ArrayList<String> data)
	{
		for (SpeechListener l : speechListeners)
			l.onFinalData(data.get(0));
		keyboardActionListener.CommitString(data.get(0), true);
		keyboardActionListener.TerminateStringSessionIfAny();
	}

	public void SendSpeechError(int error)
	{
		Resources r = keyboardActionListener.getResources();
		String errorMsg = "";
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
				//errorMsg = r.getString(R.string.recognition_error_client);
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

		for (SpeechListener l : speechListeners)
			l.onError(errorMsg);
		keyboardActionListener.TerminateStringSessionIfAny();
		if (!errorMsg.equals("")) keyboardActionListener.showToast(errorMsg);
	}

	public void SendAudioLevel(float level)
	{
		for (SpeechListener l : speechListeners)
			l.onAudioLevelChange(level);
	}

	public void TriggerOnReadyForSpeech()
	{
		for (SpeechListener l : speechListeners)
			l.onReadyForSpeech();
	}

	public void TriggerStartOfSpeech()
	{
		for (SpeechListener l : speechListeners)
			l.onSpeechStart();
	}

	public void TriggerEndOfSpeech()
	{
		for (SpeechListener l : speechListeners)
			l.onSpeechEnd();
	}
	//endregion

	//region Preferences
	public void PreferencesPut(String key, boolean value)
	{
		eSharedPreferences.put(key, value);
	}

	public void PreferencesPut(String key, int value)
	{
		eSharedPreferences.put(key, value);
	}

	public void PreferencesPut(String key, float value)
	{
		eSharedPreferences.put(key, value);
	}

	public void PreferencesPut(String key, String value)
	{
		eSharedPreferences.put(key, value);
	}

	public boolean PreferencesGetBoolean(String key, boolean defaultValue)
	{
		return eSharedPreferences.getBoolean(key, defaultValue);
	}

	public int PreferencesGetInt(String key, int defaultValue)
	{
		return eSharedPreferences.getInt(key, defaultValue);
	}

	public float PreferencesGetFloat(String key, float defaultValue)
	{
		return eSharedPreferences.getFloat(key, defaultValue);
	}

	public String PreferencesGetString(String key, String defaultValue)
	{
		return eSharedPreferences.getString(key, defaultValue);
	}

	public boolean PreferencesContains(String key)
	{
		return eSharedPreferences.contains(key);
	}
	//endregion

	//region Cursor
	public void MoveCursorLeft(int entity)
	{
		keyboardActionListener.moveCursor(entity, KeyboardActionListener.Direction.LEFT);
	}

	public void MoveCursorRight(int entity)
	{
		keyboardActionListener.moveCursor(entity, KeyboardActionListener.Direction.RIGHT);
	}

	public void MoveCursorUp(int entity)
	{
		keyboardActionListener.moveCursor(entity, KeyboardActionListener.Direction.UP);
	}

	public void MoveCursorDown(int entity)
	{
		keyboardActionListener.moveCursor(entity, KeyboardActionListener.Direction.DOWN);
	}
	//endregion

	//region CTRL
	public void Copy()
	{
		keyboardActionListener.Copy();
	}

	public void Paste()
	{
		keyboardActionListener.Paste();
	}

	public void Cut()
	{
		keyboardActionListener.Cut();
	}

	public void SelectAll()
	{
		keyboardActionListener.SelectAll();
	}

	public void Undo()
	{
		keyboardActionListener.Undo();
	}

	public void Redo()
	{
		keyboardActionListener.Redo();
	}
	//endregion
}
