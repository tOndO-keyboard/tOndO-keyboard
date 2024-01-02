using System;
using System.IO;
using UnityEngine;
using UnityEngine.Android;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public class NativeInterface: LazySingleIstanceMonoBehaviour<NativeInterface>
{
    public delegate void ConfigurationChangedEventDelegate();
    public static event ConfigurationChangedEventDelegate ConfigurationChanged;

    public delegate void CursorPositionChangedEventDelegate(string precedingCharacter);
    public static event CursorPositionChangedEventDelegate CursorPositionChanged;

    public delegate void InputViewStartedEventDelegate(string precedingCharacter);
    public static event InputViewStartedEventDelegate InputViewStarted;
    
    public delegate void SetInputTypeEventDelegate(EditorInputType type);
    public static event SetInputTypeEventDelegate SetInputType;

    public delegate void ClipboardChangeDelegate(string content);
    public static event ClipboardChangeDelegate ClipboardChange;

    public delegate void EditingWordChangeDelegate(string word);
    public static event EditingWordChangeDelegate EditingWordChange;

    //<wink>
    public delegate void ProStateChangeDelegate(bool newState);
    public static event ProStateChangeDelegate ProStateChange;
    //</wink>
    
    [SerializeField]
    private InputField inputField;

    [Space]
    [SerializeField]
    private bool _editorLandscape = false;

    [SerializeField]
    private bool _editorIsPro = true;

    private bool lastIsPro = false;
    private string lastPrecedingCharacter = "";
    private bool lastCommitWasEmoji = false;

    
#if UNITY_ANDROID && !UNITY_EDITOR
    private AndroidJavaClass unityInterfaceClass;
    private AndroidJavaClass UnityInterfaceClass
    {
        get
        {
            if (unityInterfaceClass == null) unityInterfaceClass = new AndroidJavaClass("com.foschia.tondokeyboard.UnityInterface");
            return unityInterfaceClass;
        }
    }

    private AndroidJavaObject unityInterfaceInstance;
    private AndroidJavaObject UnityInterfaceInstance
    {
        get
        {
            if (unityInterfaceInstance == null)
                unityInterfaceInstance = UnityInterfaceClass.GetStatic<AndroidJavaObject>("instance");
            return unityInterfaceInstance;
        }
    }
#endif


    public bool LastCommitWasEmoji
    {
        get
        {
            return lastCommitWasEmoji;
        }
    }

    private void Awake()
    {
        DontDestroyOnLoad(this);
    }

    public void HandleException()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("HandleException");
#endif
    }

    public void CommitString(string s)
    {
        lastCommitWasEmoji = false;
        if (inputField) inputField.text = inputField.text + s;

#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("CommitString",  s );
#endif
    }

    public void CommitEmoji(string s)
    {
        lastCommitWasEmoji = true;
        if (inputField) inputField.text = inputField.text + s;

#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("CommitEmoji",  s );
#endif
    }

    public void ReplaceLastCharacterWith(string s)
    {
        if (inputField && inputField.text.Length >= s.Length)
        {
            string newText = inputField.text;
            newText = newText.Remove(inputField.text.Length - s.Length);
            newText = newText + s;
            inputField.text = newText;
        }

#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("ReplaceLastCharacterWith", s);
#endif
    }

    public void CommitBackspace()
    {
        lastCommitWasEmoji = false;
        if (inputField && inputField.text.Length > 0) inputField.text = inputField.text.Remove(inputField.text.Length - 1);

#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("CommitBackspace");
#endif
    }

    public void CommitDone()
    {
        lastCommitWasEmoji = false;
        if (inputField) inputField.text = inputField.text + "\n";
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("CommitDone");
#endif
    }

    public int GetDoneActionFlag()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return UnityInterfaceInstance.Call<int>("GetDoneActionFlag");
#else
        return -1;
#endif

    }

    public void OpenSystemKeyboardChooser()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("OpenSystemKeyboardChooser");
#endif
    }

    public void OpenKeyboardOptions()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("OpenKeyboardOptions");
#endif
    }

    public void HideKeyboard()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("HideKeyboard");
#endif
    }

    public void Vibrate()
    {
        long milliseconds = (long)SettingsManager.Instance.VibrationMilliseconds;
        if (milliseconds > 0) Vibrate(milliseconds);
    }

    public void Vibrate(long milliseconds)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("Vibrate", milliseconds);
#endif
    }

    public void SetKeyboardProportion(float proportion)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("SetKeyboardProportion", proportion);
#endif
    }

    public String GetPrecedingCharacter(int n, bool forced = false)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if(n == 1 && !forced) return lastPrecedingCharacter;
        return UnityInterfaceInstance.Call<string>("GetPrecedingCharacter", n);
#else
        if(inputField && inputField.text.Length >= n) return inputField.text.Substring(inputField.text.Length - n, n);
        return default;
#endif
        
    }

    public String GetFollowingCharacter(int n)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return UnityInterfaceInstance.Call<string>("GetFollowingCharacter", n);
#else
        return default;
#endif
    }

    public string GetClipboardContent()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return UnityInterfaceInstance.Call<string>("GetClipboardContent");
#else
        return GUIUtility.systemCopyBuffer;
#endif
    }

    public bool GetNightModeOn()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return UnityInterfaceInstance.Call<bool>("GetNightModeOn");
#else
        return false;
#endif

    }

    public bool IsLandscape()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return UnityInterfaceInstance.Call<bool>("isLandscape");
#else
        return _editorLandscape;
#endif    
    }

    public bool IsPro()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        lastIsPro = UnityInterfaceInstance.Call<bool>("isPro");
#else
        lastIsPro = _editorIsPro;
#endif
        return lastIsPro;
    }

    #region Dictionaries
    public (Stream Dic, Stream Aff) GetDictionaryStreams()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        byte[] bytes = (byte[])((Array)UnityInterfaceInstance.Call<sbyte[]>("GetDictionaryBytes"));
        if (bytes.Length != 0)
        {
            if (BitConverter.IsLittleEndian) 
            {
                Array.Reverse(bytes, 0, sizeof(int));
                Array.Reverse(bytes, sizeof(int), sizeof(int));
            }

            int dicLen = BitConverter.ToInt32(bytes, 0);
            int affLen = BitConverter.ToInt32(bytes, sizeof(int));
            var dic = new MemoryStream(bytes, sizeof(int) * 2, dicLen);
            var aff = new MemoryStream(bytes, sizeof(int) * 2 + dicLen, affLen);
            return (dic, aff);
        } 
        else
        { 
#endif
        return (Stream.Null, Stream.Null);
#if UNITY_ANDROID && !UNITY_EDITOR
        }
#endif
    }

    public void CommitCorrection(string word)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("CommitCorrection", word);
#endif
    }
    #endregion

    #region Speech Recognition
    public void RegisterSpeechListener(NativeSpeechListener listener)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("RegisterSpeechListener", listener);
#endif
    }

    public void UnregisterSpeechListener(NativeSpeechListener listener)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("UnregisterSpeechListener", listener);
#endif
    }

    public void StartTranscribingSpeech()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("StartTranscribingSpeech");
#endif
    }

    public void CancelTranscribingSpeech()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("CancelTranscribingSpeech");
#endif
    }

    public bool HasRecordAudioPermissions()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return UnityInterfaceInstance.Call<bool>("HasRecordAudioPermissions");
#else
        return true;
#endif
    }

    public void RequestRecordAudioPermissions()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("RequestRecordAudioPermissions");
#endif
    }
    #endregion

    #region Preferences
    public void PreferencesPut<T>(string key, T value)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("PreferencesPut", new object[] { key, value });
#endif
    }

    public T PreferencesGet<T>(string key, T defaultValue)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        string methodName = "";
        switch (Type.GetTypeCode(typeof(T)))
        {
            case TypeCode.Boolean:
                methodName = "PreferencesGetBoolean";
                break;
            case TypeCode.Int32:
                methodName = "PreferencesGetInt";
                break;
            case TypeCode.Single:
                methodName = "PreferencesGetFloat";
                break;
            case TypeCode.String:
                methodName = "PreferencesGetString";
                break;
            default:
                DebugLogger.Log("[PreferencesGet was called with unexpected generic type: " + Type.GetTypeCode(typeof(T)), DebugLogger.LogType.ERROR);
                return default(T);
        }

        return UnityInterfaceInstance.Call<T>(methodName, new object[] { key, defaultValue });
#else
        return default(T);
#endif
    }

    public bool PreferencesContains(string key)
{
#if UNITY_ANDROID && !UNITY_EDITOR
        return UnityInterfaceInstance.Call<bool>("PreferencesContains", key);
#else
        return false;
#endif    
    }
    #endregion

    #region Cursor
    public void MoveCursorLeft(int entity)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("MoveCursorLeft", entity);
#endif
    }

    public void MoveCursorRight(int entity)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("MoveCursorRight", entity);
#endif
    }

    public void MoveCursorUp(int entity)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("MoveCursorUp", entity);
#endif
    }

    public void MoveCursorDown(int entity)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("MoveCursorDown", entity);
#endif
    }
    #endregion

    #region CTRL
    public void Copy()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("Copy");
#endif
    }

    public void Paste()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("Paste");
#endif
    }

    public void Cut()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("Cut");
#endif
    }

    public void SelectAll()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("SelectAll");
#endif
    }

    public void Undo()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("Undo");
#endif
    }

    public void Redo()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        UnityInterfaceInstance.Call("Redo");
#endif
    }
    #endregion

    #region Native Events
    public void OnClipboardChanged(string value)
    {
        ClipboardChange?.Invoke(value);
    }

    public void OnConfigurationChanged()
    {
        float proportion = SettingsManager.Instance.TopBarHidden ?
                CanvasScalerManager.HIDDEN_TOP_BAR_PROPORTION : CanvasScalerManager.DEFAULT_PROPORTION;
        SetKeyboardProportion(proportion);
        ConfigurationChanged?.Invoke();
    }

    public void OnCursorPositionChanged(string precedingCharacter)
    {
#if UNITY_EDITOR
        if (inputField && inputField.text.Length >= 1)
            precedingCharacter = inputField.text.Substring(inputField.text.Length - 1, 1);
#endif
        this.lastPrecedingCharacter = precedingCharacter;
        CursorPositionChanged?.Invoke(precedingCharacter);
    }

    public void OnStartInputView(string precedingCharacter)
    {
        EventSystem.current.SetSelectedGameObject(null);

        this.lastPrecedingCharacter = precedingCharacter;
        CursorPositionChanged?.Invoke(precedingCharacter);
        InputViewStarted?.Invoke(precedingCharacter);

        bool wasPro = lastIsPro;
        bool isPro = IsPro();
        if(wasPro != isPro)
        {
            ProStateChange?.Invoke(isPro);
        }
    }

    public void OnSetInputType(string inputType)
    {
        EditorInputType type = (EditorInputType)int.Parse(inputType);
        SetInputType?.Invoke(type);
    }

    public void OnEditingWordChange(string word) => EditingWordChange?.Invoke(word);
    #endregion
}
