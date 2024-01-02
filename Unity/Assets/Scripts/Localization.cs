using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Localization
{
    public const string MACRO_TUTORIAL_LEFT = "macros_tutorial_left";
    public const string MACRO_TUTORIAL_RIGHT = "macros_tutorial_right";
    public const string MACRO_TUTORIAL_PRO = "macros_tutorial_pro";
    public const string TOO_MANY_MACRO_TOAST = "too_many_macros_toast";
    public const string PRO_REQUIRED_MESSAGE = "pro_required_message";

    public static readonly Localization Instance = new Localization();

#if UNITY_ANDROID && !UNITY_EDITOR
    private readonly AndroidJavaObject _native;
#endif

    private Localization()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        AndroidJavaClass klass = new AndroidJavaClass("com.foschia.tondokeyboard.Localization");
        _native = klass.GetStatic<AndroidJavaObject>("Instance");
#endif
    }

    public bool IsInitialized
    {
        get
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            return _native.Call<bool>("isInitialized");
#elif UNITY_EDITOR
            return true;
#endif
        }
    }

    public bool KeyExists(string key) 
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return _native.Call<bool>("keyExists", key);
#elif UNITY_EDITOR
        return true;
#endif
    }

    public string Localize(string key)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return _native.Call<string>("localize", key);
#elif UNITY_EDITOR
        return key;
#endif
    }

    public string LocalizeOrDefault(string key, string def)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        return _native.Call<string>("localizeOrDefault", key, def);
#elif UNITY_EDITOR
        return key;
#endif
    }
}
