package com.foschia.tondokeyboard;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

public class TondoRecognitionListener implements RecognitionListener
{

	private final TondoInterface tondoInterface;

	public TondoRecognitionListener(TondoInterface tondoInterface)
	{
		this.tondoInterface = tondoInterface;
	}

	@Override
	public void onReadyForSpeech(Bundle params)
	{
		tondoInterface.TriggerOnReadyForSpeech();
	}

	@Override
	public void onBeginningOfSpeech()
	{
		tondoInterface.TriggerStartOfSpeech();
	}

	@Override
	public void onRmsChanged(float rmsdB)
	{
		tondoInterface.SendAudioLevel(rmsdB);
	}

	@Override
	public void onBufferReceived(byte[] buffer)
	{

	}

	@Override
	public void onEndOfSpeech()
	{
		tondoInterface.TriggerEndOfSpeech();
	}

	@Override
	public void onError(int error)
	{
		tondoInterface.SendSpeechError(error);
	}

	@Override
	public void onResults(Bundle results)
	{
		ArrayList<String> data = results
				.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		tondoInterface.SendSpeechData(data);
	}

	@Override
	public void onPartialResults(Bundle partialResults)
	{
		ArrayList<String> data = partialResults
				.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		tondoInterface.SendSpeechPartialData(data);
	}

	@Override
	public void onEvent(int eventType, Bundle params)
	{

	}
}
