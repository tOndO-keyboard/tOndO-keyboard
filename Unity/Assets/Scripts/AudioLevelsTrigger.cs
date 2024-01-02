using UnityEngine;
using UnityEngine.Events;

public class AudioLevelsTrigger : MonoBehaviour
{
    [SerializeField]
    private bool _triggerOnDownTransient = false;
    [SerializeField, Min(0)]
    private float _refractoryTimer = 0;

    [SerializeField]
    private UnityEvent _onTrigger = null;

    private NativeSpeechListener _listener = null;
    private float _timer = 0;
    private float _lastValue = 0;
    private bool _shouldTrigger = false;

    private void Awake()
    {
        _listener = new NativeSpeechListener
        {
            AudioLevelTrigger = OnTrigger,
        };

        NativeInterface.Instance.RegisterSpeechListener(_listener);
    }

    private void Update()
    {
        if (_shouldTrigger)
        {
            _shouldTrigger = false;
            _onTrigger.Invoke();
        }

        if (_timer > 0) _timer -= Time.unscaledDeltaTime;
    }

    private void OnDisable() => _lastValue = 0;

    private void OnDestroy() => NativeInterface.Instance.UnregisterSpeechListener(_listener);

    private void OnTrigger(float dB)
    {
        float comparison = _lastValue;
        _lastValue = dB;

        if (_timer > 0) return;
        if (!_triggerOnDownTransient && dB > comparison || _triggerOnDownTransient && dB < comparison)
        {
            _timer = _refractoryTimer; 
            _shouldTrigger = true;
        }
    }
}
