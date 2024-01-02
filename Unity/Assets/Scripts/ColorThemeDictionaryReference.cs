using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class ColorThemeDictionaryReference : LazySingleIstanceMonoBehaviour<ColorThemeDictionaryReference>
{
    public delegate void ThemeChangedEventDelegate();
    public static event ThemeChangedEventDelegate ThemeChanged;

    [SerializeField]
    public ColorThemeDictionaryScriptableObject ColorThemeDictionary;

    [SerializeField]
    private COLOR_THEME CurrentTheme = COLOR_THEME.LIGHT;

    private void Awake()
    {
        NativeInterface.InputViewStarted += OnInputViewStarted;
    }

    private void OnInputViewStarted(string precedingCharacter)
    {
        COLOR_THEME newTheme = COLOR_THEME.NONE;
        ThemeType currentThemeType = SettingsManager.Instance.ThemeType;
        switch(currentThemeType)
        {
            case ThemeType.FollowSystem:
                newTheme = NativeInterface.Instance.GetNightModeOn() ? COLOR_THEME.DARK : COLOR_THEME.LIGHT;
                break;
            case ThemeType.Dark:
                newTheme = COLOR_THEME.DARK;
                break;
            case ThemeType.Light:
                newTheme = COLOR_THEME.LIGHT;
                break;
        }

        if(newTheme != CurrentTheme)
        {
            CurrentTheme = newTheme;
            ThemeChanged?.Invoke();
        }
    }

    public Color GetColorForCurrentTheme(PALETTE_ELEMENT element)
    {
        return ColorThemeDictionary.ColorThemeColorPaletteDictionary[CurrentTheme].ColorPaletteDictionary[element];
    }
}
