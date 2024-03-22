using UnityEngine;
using UnityEngine.UI;

public enum InputType { Needle, DoubleNeedle, Joystick, NoThumbs };
public enum ThemeType { FollowSystem, Light, Dark };

public class SettingsManager : LazySingleIstanceMonoBehaviour<SettingsManager>
{
    private static readonly string playerPrefKeyPrefix = "SettingsManagerPlayerPrefKey_";

    private bool deserializeDone = false;

    [SerializeField]
    private InputType inputType = InputType.Joystick;
    public InputType InputType
    {
        get
        {
            Deserialize();
            return inputType;
        }
    }

    [SerializeField]
    private ThemeType themeType = ThemeType.FollowSystem;
    public ThemeType ThemeType
    {
        get
        {
            Deserialize();
            return themeType;
        }
    }

    [SerializeField]
    private bool reverse = false;
    public bool Reverse
    {
        get
        {
            Deserialize();
            return reverse;
        }
    }

    [SerializeField]
    private bool sticky = true;
    public bool Sticky
    {
        get
        {
            Deserialize();
            return sticky;
        }
    }

    [SerializeField]
    private bool verySticky = false;
    public bool VerySticky
    {
        get
        {
            Deserialize();
            return verySticky;
        }
    }

    [SerializeField]
    private bool showPopupPreview = true;
    public bool ShowPopupPreview
    {
        get
        {
            Deserialize();
            return showPopupPreview;
        }
    }

    [SerializeField]
    private int vibrationMillis = 10;
    public int VibrationMilliseconds
    {
        get
        {
            Deserialize();
            return vibrationMillis;

        }
    }

    [SerializeField]
    private int externalGlyphSelectionDistanceFactor = 7;
    public int ExternalGlyphSelectionDistanceFactor
    {
        get
        {
            Deserialize();
            return externalGlyphSelectionDistanceFactor;

        }
    }

    [SerializeField]
    private bool topBarWentBottom = false;
    public bool TopBarWentBottom
    {
        get
        {
            Deserialize();
            return topBarWentBottom;

        }
    }

    [SerializeField]
    private bool enableSuggestions = false;
    public bool EnableSuggestions
    {
        get
        {
            Deserialize();
            return enableSuggestions;

        }
    }

    [SerializeField]
    private string suggestionsLanguage = "en_US";
    public string SuggestionsLanguage
    {
        get
        {
            Deserialize();
            return suggestionsLanguage;

        }
    }

    [SerializeField]
    private bool disableAutomaticCapitalization = false;
    public bool DisableAutomaticCapitalization
    {
        get
        {
            Deserialize();
            return disableAutomaticCapitalization;

        }
    }

    [SerializeField]
    [HideInInspector]
    private string characterCustomizationString = "EQWRT*\nUYFGH\'\"\nIJLP:^\nA@()DS#\n Z,.VCX\nOB!?KMN\n\n$/123*\n£4567\'\"\n€890:^\n[@()]%#\n ;,.&><\n_~!?-+=\n\n•\\|½⅓²\nβαδεγ»«\nπλΣΔρ°\n✓₿{}¥§¶\n ®™¢©≥≤\n×√∞∙÷±≠"; 
    public string CharacterCustomizationString
    {
        get
        {
            Deserialize();
            return characterCustomizationString;

        }
    }

    [SerializeField]
    [HideInInspector]
    private string diacriticsCustomizationString;
    public string DiacriticsCustomizationString
    {
        get
        {
            Deserialize();
            return diacriticsCustomizationString;

        }
    }

    [SerializeField]
    private bool topBarHidden = false;
    public bool TopBarHidden
    {
        get
        {
            Deserialize();
            return topBarHidden;
        }
        set
        {
            topBarHidden = value;
            PlayerPrefs.SetInt(playerPrefKeyPrefix + "TopBarHidden", topBarHidden ? 1 : 0);
        }
    }

    private void Awake()
    {
        Deserialize();
        NativeInterface.InputViewStarted += OnInputViewStarted;
    }

    private void OnInputViewStarted(string s = "")
    {
        Deserialize(true);
    }

    private void Deserialize(bool force = false)
    {
        if(deserializeDone && !force)
        {
            return;
        }

        if (NativeInterface.Instance != null)
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            inputType = (InputType) int.Parse(NativeInterface.Instance.PreferencesGet<string>("InputType", ((int)inputType).ToString()));
            themeType = (ThemeType)int.Parse(NativeInterface.Instance.PreferencesGet<string>("ThemeType", ((int)themeType).ToString()));
            reverse = NativeInterface.Instance.PreferencesGet<bool>("Reverse", reverse);
            sticky = NativeInterface.Instance.PreferencesGet<bool>("Sticky", sticky);
            verySticky = NativeInterface.Instance.PreferencesGet<bool>("VerySticky", verySticky);
            showPopupPreview = NativeInterface.Instance.PreferencesGet<bool>("ShowPopupPreview", showPopupPreview);
            vibrationMillis = NativeInterface.Instance.PreferencesGet<int>("VibrationMilliseconds", vibrationMillis);
            externalGlyphSelectionDistanceFactor = NativeInterface.Instance.PreferencesGet<int>("ExternalGlyphSelectionDistanceFactor", externalGlyphSelectionDistanceFactor);
            topBarWentBottom = NativeInterface.Instance.PreferencesGet<bool>("TopBarWentBottom", topBarWentBottom);
            enableSuggestions = NativeInterface.Instance.PreferencesGet<bool>("EnableSuggestions", enableSuggestions);
            suggestionsLanguage = NativeInterface.Instance.PreferencesGet<string>("SuggestionsLanguage", suggestionsLanguage);
            disableAutomaticCapitalization = NativeInterface.Instance.PreferencesGet<bool>("DisableAutomaticCapitalization", disableAutomaticCapitalization);
            characterCustomizationString = NativeInterface.Instance.PreferencesGet<string>("CharacterCustomizationString", characterCustomizationString);
            diacriticsCustomizationString = NativeInterface.Instance.PreferencesGet<string>("DiacriticsCustomizationString", diacriticsCustomizationString);
            topBarHidden = PlayerPrefs.GetInt(playerPrefKeyPrefix + "TopBarHidden", 0) == 1;
#endif
            deserializeDone = true;
        }
    }

    public void SetNeedle(Toggle t)
    {
        if (t.isOn) inputType = InputType.Needle;
    }

    public void SetDoubleNeedle(Toggle t)
    {
        if (t.isOn) inputType = InputType.DoubleNeedle;
    }
    public void SetNoThumbs(Toggle t)
    {
        if (t.isOn) inputType = InputType.NoThumbs;
    }

    public void SetJoystick(Toggle t)
    {
        if (t.isOn) inputType = InputType.Joystick;
    }

    public void SetReverse(Toggle t)
    {
        reverse = t.isOn;
    }

    public void SetSticky(Toggle t)
    {
        sticky = t.isOn;
    }

    public void SetVerySticky(Toggle t)
    {
        verySticky = t.isOn;
    }

    public void SetTopBarWentBottom(Toggle t)
    {
        topBarWentBottom = t.isOn;
    }

    public void SetDisableAutomaticCapitalization(Toggle t)
    {
        disableAutomaticCapitalization = t.isOn;
    }

    public void SetShowPopupPreview(Toggle t)
    {
        showPopupPreview = t.isOn;
    }

    public void SetVibrationMillis(Slider s)
    {
        vibrationMillis = Mathf.RoundToInt(s.value);
    }
}
