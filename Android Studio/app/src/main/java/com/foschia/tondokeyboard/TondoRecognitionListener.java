package com.foschia.tondokeyboard;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

public class TondoRecognitionListener implements RecognitionListener
{

	private final UnityInterface unityInterface;

	public TondoRecognitionListener(UnityInterface unityInterface)
	{
		this.unityInterface = unityInterface;
	}

	@Override
	public void onReadyForSpeech(Bundle params)
	{
		unityInterface.TriggerOnReadyForSpeech();
	}

	@Override
	public void onBeginningOfSpeech()
	{
		unityInterface.TriggerStartOfSpeech();
	}

	@Override
	public void onRmsChanged(float rmsdB)
	{
		unityInterface.SendAudioLevel(rmsdB);
	}

	@Override
	public void onBufferReceived(byte[] buffer)
	{

	}

	@Override
	public void onEndOfSpeech()
	{
		unityInterface.TriggerEndOfSpeech();
	}

	@Override
	public void onError(int error)
	{
		unityInterface.SendSpeechError(error);
	}

	@Override
	public void onResults(Bundle results)
	{
		ArrayList<String> data = results
				.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		unityInterface.SendSpeechData(data);
	}

	@Override
	public void onPartialResults(Bundle partialResults)
	{
		ArrayList<String> data = partialResults
				.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		unityInterface.SendSpeechPartialData(data);
	}

	@Override
	public void onEvent(int eventType, Bundle params)
	{

	}
}
