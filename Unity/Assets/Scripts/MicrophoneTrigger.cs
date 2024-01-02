using UnityEngine;
using UnityEngine.Events;

public class MicrophoneTrigger : MonoBehaviour
{
    public event System.Action<bool> StateChanged;
    public event System.Action Trigger;

    [Space]
    [SerializeField, Min(11_000)]
    private int _sampleFrequency = 22_000;
    [SerializeField, Range(0, 1)]
    private float _thresholdUp = .8f;
    [SerializeField, Min(0)]
    private float _refractoryPeriod = .1f;

    [SerializeField]
    private UnityEvent _trigger = null;

    private AudioClip _recording = null;
    private float[] _audioData = null;
    private int _readHead = 0;
    private float _refractoryTimer = 0;

    public string CurrentDevice{ get; set; }

    public bool IsRecording => Microphone.IsRecording(CurrentDevice);

    private void Init()
    {
        if (CurrentDevice == null && Microphone.devices.Length > 0)
            CurrentDevice = Microphone.devices[0];
    }

    private void Awake() { }

    private void Update()
    {
        if (!IsRecording || CurrentDevice == null) return;

        _refractoryTimer = Mathf.Max(0, _refractoryTimer - Time.unscaledDeltaTime);
        
        int writeHead = Microphone.GetPosition(CurrentDevice);
        int samples = (_recording.samples - _readHead + writeHead) % _recording.samples;

        if (samples == 0) return;

        if (_audioData == null || _audioData.Length < samples)
            _audioData = new float[samples];
        _recording.GetData(_audioData, _readHead);

        for (int i = 0; i < samples && _refractoryTimer <= 0; i++)
        {
            if (_audioData[i] >= _thresholdUp)
            {
                Trigger?.Invoke();
                _trigger.Invoke();

                _refractoryTimer = _refractoryPeriod;
                i += 4;
            }
        }

        _readHead = (_readHead + samples) % _recording.samples;
    }

    public void Rec()
    {
        Init();
        if (!IsRecording)
        {
            _recording = Microphone.Start(CurrentDevice, true, 1, _sampleFrequency);
            StateChanged?.Invoke(true);
        }
    }

    public void Stop()
    {
        Init();

        _readHead = 0;

        Microphone.End(CurrentDevice);
        StateChanged?.Invoke(false);
    }
}
