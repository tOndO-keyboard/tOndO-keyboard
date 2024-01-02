using System.Collections.Generic;
using UnityEngine;
using WeCantSpell.Hunspell;
using System.IO;
using System.Threading;
using System.Linq;

public class Suggester : MonoBehaviour
{
    public enum InitStep { Uninitialized, Initializing, Initialized }
    public event System.Action<IEnumerable<string>> SuggestionReady;

    [SerializeField, Min(0)]
    private float _suggestionCooldown = .25f;

    private readonly List<string> _requests = new List<string>();
    private readonly List<string> _haltingRequests = new List<string>();
    private float _timer;
    private string _latestUnscheduledRequest;
    private WordList _dictionary;

    private readonly object _lock = new object();
    private IEnumerable<string> _suggestions;
    private bool _enableSuggestionsValue;

    public InitStep InitState { get; private set; } = InitStep.Uninitialized;

    public string Lang { get; private set; }

    private void Awake()
    {
        _enableSuggestionsValue = SettingsManager.Instance.EnableSuggestions;
        NativeInterface.InputViewStarted += _ => {
            if (!_enableSuggestionsValue) return;
            if (Lang != SettingsManager.Instance.SuggestionsLanguage && InitState != InitStep.Initializing)
            {
                InitState = InitStep.Initializing;
                ThreadPool.QueueUserWorkItem(LoadDictionaries, null);
            }
        };
    }

    private void LoadDictionaries(object _)
    {
        AndroidJNI.AttachCurrentThread();
        Lang = SettingsManager.Instance.SuggestionsLanguage;
        (Stream dic, Stream aff) = NativeInterface.Instance.GetDictionaryStreams();
        _dictionary = WordList.CreateFromStreams(dic, aff);
        if (_dictionary.Affix.Warnings.Count != 0)
            DebugLogger.Log(string.Join("\n", _dictionary.Affix.Warnings), DebugLogger.LogType.WARNING);
        InitState = InitStep.Initialized;
        AndroidJNI.DetachCurrentThread();
    }

    private void Update()
    {
        _enableSuggestionsValue = SettingsManager.Instance.EnableSuggestions;

        if (!_enableSuggestionsValue)
            return;

        if (_timer > 0) _timer -= Time.unscaledDeltaTime;
        if (_timer <= 0 && !string.IsNullOrEmpty(_latestUnscheduledRequest))
        {
            Suggest(_latestUnscheduledRequest);
            _latestUnscheduledRequest = null;
        }

        lock (_lock)
        {
            if (_suggestions == null) return;
            SuggestionReady?.Invoke(_suggestions);
            _suggestions = null;
        }
    }

    private void ThreadRun(object state)
    {
        string word = state as string;
        var result = _dictionary.Suggest(word);

        bool publish = false;
        lock (_requests) publish = _requests.Remove(word);
        if (publish) lock (_lock) _suggestions = result;
        else lock (_haltingRequests) _haltingRequests.Remove(word);
    }

    public void Suggest(string word)
    {
        if (!_enableSuggestionsValue) return;

        if (_timer > 0)
        {
            _latestUnscheduledRequest = word;
            return;
        }

        if (InitState != InitStep.Initialized)
        {
            _suggestions = Enumerable.Repeat(word, 1);
            return;
        }

        if (string.IsNullOrEmpty(word))
        {
            StopAll();
            _suggestions = Enumerable.Empty<string>();
            return;
        }
        ThreadPool.QueueUserWorkItem(ThreadRun, word);
        _requests.Add(word);
        _timer = _suggestionCooldown;
    }

    public void StopAll()
    {
        lock (_requests)
        {
            lock (_haltingRequests)
            {
                _haltingRequests.AddRange(_requests);
                _requests.Clear();
            }
        }
    }
}
