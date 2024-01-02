package com.foschia.tondokeyboard;

public interface SpeechListener
{
	void onPartialData(String data);

	void onFinalData(String data);

	void onError(String errorMessage);

	void onAudioLevelChange(float dB);

	void onReadyForSpeech();

	void onSpeechStart();

	void onSpeechEnd();
}
