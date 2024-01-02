using System.Collections.Generic;
using System.Linq;
using UnityEngine;

public class SpeechInterface : MonoBehaviour
{
    [SerializeField]
    private List<GameObject> _speechActive = null;
    [SerializeField]
    private List<GameObject> _speechInactive = null;

    private NativeSpeechListener _listener;

    private System.Action _requestedCallback = null;

    public bool IsActive 
    {
        get => _speechActive.Any(g => g.activeInHierarchy);
        set
        {
            _speechActive.ForEach(g => g.SetActive(value));
            _speechInactive.ForEach(g => g.SetActive(!value));
        }
    }

    private void Awake()
    {
        IsActive = false;
        _listener = new NativeSpeechListener
        {
            ReadyForSpeech = () => RequestSetActive(true),
            SpeechEnd = () => RequestSetActive(false)
        };

        NativeInterface.Instance.RegisterSpeechListener(_listener);
    }

    private void Update()
    {
        _requestedCallback?.Invoke();
        _requestedCallback = null;
    }

    private void OnDestroy()
    {
        if (NativeInterface.Instance != null)
            NativeInterface.Instance.UnregisterSpeechListener(_listener);
    }

    private void RequestSetActive(bool active) => 
        _requestedCallback = () => IsActive = active;
}
