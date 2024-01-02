using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;
using UnityEngine.UI;

public class KeyboardController : MonoBehaviour
{
    public enum View { Main, Emoji, Mic, Macros }

    public event System.Action<View> ViewChanged; 

    [SerializeField]
    private GameObject SkinToneButtonsBar;

    [SerializeField]
    private GameObject MainButtonsBar;

    [SerializeField]
    private GameObject ControlButtonsBar;

    [SerializeField]
    private SuggestionsBar SuggestionsBar;

    [SerializeField]
    private GameObject MainBottom;

    [SerializeField]
    private GameObject EmojiBottom;

    [SerializeField]
    private GameObject MicBottom;

    [SerializeField]
    private GameObject MacrosBottom;

    [Space]
    [SerializeField]
    private ScrollRect EmojiScrollRect;
    [SerializeField]
    private Suggester Suggester;

    private NativeSpeechListener NativeSpeechListener;

    private View _requestedDuringSpeechRecognition = View.Main;
    private Queue<System.Action> _callbackQueue = new Queue<System.Action>();

    public View ActiveView
    {
        get
        {
            if (EmojiBottom.activeInHierarchy) return View.Emoji;
            if (MicBottom.activeInHierarchy) return View.Mic;
            if (MacrosBottom.activeInHierarchy) return View.Macros;

            return View.Main;
        }
    }

    public bool IsControlMode
    {
        get => ControlButtonsBar.activeSelf;
        set
        {
            ControlButtonsBar.SetActive(value);
            SetShow(ActiveView);
        }
    }

    public bool IsSuggestionsMode
    {
        get => SuggestionsBar.gameObject.activeSelf;
        set
        {
            SuggestionsBar.gameObject.SetActive(value);
            if (IsControlMode) IsControlMode = false;
            else SetShow(ActiveView);
        }
    }

    public bool IsEmojiActive => ActiveView == View.Emoji;
    public bool IsMainActive => ActiveView == View.Main;
    public bool IsMicActive => ActiveView == View.Mic;
    public bool IsMacrosActive => ActiveView == View.Macros;

    public bool IsListeningToSpeech { get; private set; } = false;

    private void Awake() 
    {
        SuggestionsBar.OnBack += () => 
        {
            Suggester.StopAll();
            IsSuggestionsMode = false;
        };

        NativeSpeechListener = new NativeSpeechListener
        {
            SpeechEnd = () => 
            {
                IsListeningToSpeech = false;
                QueueCallback(ShowMain);
            },
            ErrorTrigger = _ => 
            {
                IsListeningToSpeech = false;
                View v = _requestedDuringSpeechRecognition;
                _requestedDuringSpeechRecognition = View.Main;
                QueueCallback(() => SetShow(v));
            },
            ReadyForSpeech = () => IsListeningToSpeech = true
        };

        Suggester.SuggestionReady += OnSuggestionsReady;

        NativeInterface.InputViewStarted += OnInputViewStarted;
        NativeInterface.EditingWordChange += Suggester.Suggest;
        NativeInterface.Instance.RegisterSpeechListener(NativeSpeechListener);
    }

    private void Update()
    {
        if (_callbackQueue.Count == 0) return;
        _callbackQueue.Dequeue()?.Invoke();
    }

    private void OnApplicationPause(bool pause)
    {
        if (pause) EnsureClosure();
    }

    private void OnDestroy() => EnsureClosure();

    private void EnsureClosure()
    {
        if (NativeInterface.Instance != null)
            NativeInterface.Instance.CancelTranscribingSpeech();
        EmojiPersister.SaveInstance();
        MacrosPersister.SaveInstance();
    }

    private void QueueCallback(System.Action action) => _callbackQueue.Enqueue(action);

    private void OnInputViewStarted(string precedingCharacter) 
    {
        IsControlMode = false;
        IsSuggestionsMode = false;
        ShowMain();
    }

    private void OnSuggestionsReady(IEnumerable<string> suggestions)
    {
        IsSuggestionsMode = suggestions.Count() > 0;
        SuggestionsBar.SetSuggestions(suggestions);
    }

    private void ToggleView(View view, System.Action show)
    {
        if (ActiveView != view) show();
        else ShowMain();
    }

    private void SetShow(View view)
    {
        bool main = view == View.Main;
        bool emoji = view == View.Emoji;
        bool mic = view == View.Mic;
        bool macros = view == View.Macros;

        SkinToneButtonsBar.SetActive(!IsSuggestionsMode && !IsControlMode && emoji);
        MainButtonsBar.SetActive(!IsSuggestionsMode && !IsControlMode && (main || mic || macros));

        
        EmojiBottom.SetActive(emoji);
        MainBottom.SetActive(main);
        MicBottom.SetActive(mic);
        MacrosBottom.SetActive(macros);

        if (emoji) EmojiScrollRect.verticalNormalizedPosition = 1;

        if (mic && !IsListeningToSpeech) RequestPermissionOrStartTranscribingSpeech();
        else if (!mic && IsListeningToSpeech) 
        {
            _requestedDuringSpeechRecognition = view;
            NativeInterface.Instance.CancelTranscribingSpeech();
        }

        ViewChanged?.Invoke(view);
    }

    private void RequestPermissionOrStartTranscribingSpeech()
    {
        NativeInterface nativeInterface = NativeInterface.Instance;

        if (nativeInterface.HasRecordAudioPermissions())
            nativeInterface.StartTranscribingSpeech();
        else nativeInterface.RequestRecordAudioPermissions();

    }

    public void ToggleCtrlMode() => IsControlMode = !IsControlMode;

    public void ShowMain() => SetShow(View.Main);

    public void ShowEmoji() => SetShow(View.Emoji);

    public void ShowMic() => SetShow(View.Mic);

    public void ShowMacros() => SetShow(View.Macros);

    public void ToggleEmoji() => ToggleView(View.Emoji, ShowEmoji);


    public void ToggleMic() => ToggleView(View.Mic, ShowMic);

    public void ToggleMacros() => ToggleView(View.Macros, ShowMacros);
}
