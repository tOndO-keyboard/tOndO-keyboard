using UnityEngine;

public class NativeSpeechListener : AndroidJavaProxy
{
    public System.Action<string> FinalDataTrigger;
    public System.Action<string> PartialDataTrigger;
    public System.Action<string> ErrorTrigger;

    public System.Action<float> AudioLevelTrigger;
    public System.Action ReadyForSpeech;
    public System.Action SpeechStart;
    public System.Action SpeechEnd;

    public NativeSpeechListener() : base("com.foschia.tondokeyboard.SpeechListener") { }

    public void onFinalData(string data) => FinalDataTrigger?.Invoke(data);

    public void onPartialData(string data) => PartialDataTrigger?.Invoke(data);
    
    public void onError(string errorMessage) => ErrorTrigger?.Invoke(errorMessage);

    public void onAudioLevelChange(float dB) => AudioLevelTrigger?.Invoke(dB);

    public void onReadyForSpeech() => ReadyForSpeech?.Invoke();

    public void onSpeechStart() => SpeechStart?.Invoke();

    public void onSpeechEnd() => SpeechEnd?.Invoke();
}
