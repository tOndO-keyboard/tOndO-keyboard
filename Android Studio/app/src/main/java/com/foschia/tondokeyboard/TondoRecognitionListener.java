package com.foschia.tondokeyboard;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

public class TondoRecognitionListener implements RecognitionListener
{

	private final NativeInterface nativeInterface;

	public TondoRecognitionListener(NativeInterface nativeInterface)
	{
		this.nativeInterface = nativeInterface;
	}

	@Override
	public void onReadyForSpeech(Bundle params)
	{
		nativeInterface.TriggerOnReadyForSpeech();
	}

	@Override
	public void onBeginningOfSpeech()
	{
		nativeInterface.TriggerStartOfSpeech();
	}

	@Override
	public void onRmsChanged(float rmsdB)
	{
		nativeInterface.SendAudioLevel(rmsdB);
	}

	@Override
	public void onBufferReceived(byte[] buffer)
	{

	}

	@Override
	public void onEndOfSpeech()
	{
		nativeInterface.TriggerEndOfSpeech();
	}

	@Override
	public void onError(int error)
	{
		nativeInterface.SendSpeechError(error);
	}

	@Override
	public void onResults(Bundle results)
	{
		ArrayList<String> data = results
				.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		nativeInterface.SendSpeechData(data);
	}

	@Override
	public void onPartialResults(Bundle partialResults)
	{
		ArrayList<String> data = partialResults
				.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		nativeInterface.SendSpeechPartialData(data);
	}

	@Override
	public void onEvent(int eventType, Bundle params)
	{

	}
}
